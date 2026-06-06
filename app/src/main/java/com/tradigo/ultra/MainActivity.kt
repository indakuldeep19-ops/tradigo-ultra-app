package com.tradigo.ultra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.tradigo.ultra.ui.screens.TradingDashboard
import com.tradigo.ultra.engine.TradingEngine
import com.tradigo.ultra.viewmodel.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — Tradigo Ultra V4.0
 *
 * Implements PaymentResultWithDataListener per PaymentViewModel contract.
 * Razorpay callbacks are forwarded to PaymentViewModel for verification.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private val paymentViewModel: PaymentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge rendering for immersive trading UI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            TradingDashboard(engine = TradingEngine())
        }
    }

    // Razorpay callbacks
    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        paymentViewModel.submitVerificationToken(
            orderId = paymentData?.orderId ?: return,
            paymentId = razorpayPaymentId ?: return,
            signature = paymentData?.signature ?: return
        )
    }

    override fun onPaymentError(errorCode: Int, errorDescription: String?, paymentData: PaymentData?) {
        paymentViewModel.onPaymentError(errorCode, errorDescription)
    }
}
