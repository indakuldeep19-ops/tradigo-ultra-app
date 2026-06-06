const functions = require('firebase-functions');
const admin = require('firebase-admin');
const Razorpay = require('razorpay');

const razorpay = new Razorpay({
  key_id: process.env.RAZORPAY_KEY_ID,
  key_secret: process.env.RAZORPAY_SECRET
});

exports.createRazorpayOrder = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Login required');
  }

  const { amount, currency = 'INR' } = data;
  
  try {
    const order = await razorpay.orders.create({
      amount: amount,
      currency: currency,
      receipt: `order_${Date.now()}`
    });

    return {
      orderId: order.id,
      amount: order.amount,
      currency: order.currency,
      keyId: process.env.RAZORPAY_KEY_ID
    };
  } catch (error) {
    throw new functions.https.HttpsError('internal', error.message);
  }
});

exports.verifyPaymentSignature = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Login required');
  }

  const { orderId, paymentId, signature } = data;
  
  const crypto = require('crypto');
  const body = orderId + '|' + paymentId;
  const expectedSignature = crypto
    .createHmac('sha256', process.env.RAZORPAY_SECRET)
    .update(body)
    .digest('hex');

  if (expectedSignature === signature) {
    // Update user balance in Firestore
    const db = admin.firestore();
    const userRef = db.collection('users').doc(context.auth.uid);
    
    await db.runTransaction(async (transaction) => {
      const userDoc = await transaction.get(userRef);
      const currentBalance = userDoc.data()?.balance || 0;
      const amount = data.amount / 100; // Convert paise to rupees
      
      transaction.update(userRef, {
        balance: currentBalance + amount,
        lastDeposit: admin.firestore.FieldValue.serverTimestamp()
      });
    });

    return {
      success: true,
      grossDeposit: data.amount / 100,
      convenienceFee: 0,
      netAmountCredited: data.amount / 100,
      updatedBalance: 0 // Will be fetched client-side
    };
  } else {
    throw new functions.https.HttpsError('invalid-argument', 'Invalid signature');
  }
});
