const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Import all function modules
const jarvisProxy = require('./jarvisProxy');
const jarvisAI = require('./jarvis_ai_assistant');
const jarvisUpdates = require('./jarvis_updates');
const duoTrading = require('./duo_trading');
const razorpayIntegration = require('./razorpay_integration');
const orderEngine = require('./orderEngine');
const revenueEngine = require('./revenueEngine');
const sreMonitoring = require('./sre_monitoring');

// Export all functions
exports.jarvisProxy = jarvisProxy.jarvisProxy;
exports.jarvisAI = jarvisAI.jarvisAI;
exports.logJarvisEvent = jarvisUpdates.logJarvisEvent;
exports.sendJarvisEmailUpdate = jarvisUpdates.sendJarvisEmailUpdate;
exports.createDuoRequest = duoTrading.createDuoRequest;
exports.acceptDuoRequest = duoTrading.acceptDuoRequest;
exports.executeDuoTrade = duoTrading.executeDuoTrade;
exports.createRazorpayOrder = razorpayIntegration.createRazorpayOrder;
exports.verifyPaymentSignature = razorpayIntegration.verifyPaymentSignature;
exports.placeOrder = orderEngine.placeOrder;
exports.getOrderStatus = orderEngine.getOrderStatus;
exports.getRevenueReport = revenueEngine.getRevenueReport;
exports.checkSystemHealth = sreMonitoring.checkSystemHealth;
