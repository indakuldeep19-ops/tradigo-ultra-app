package com.tradigo.ultra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.tradigo.ultra.ui.screens.LoginScreen
import com.tradigo.ultra.ui.theme.TradigoUltraTheme
import com.tradigo.ultra.viewmodel.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private val paymentViewModel: PaymentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            TradigoUltraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onLoginSuccess = {
                            // Navigate to dashboard
                        }
                    )
                }
            }
        }
    }

    // Razorpay callbacks
    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        paymentViewModel.submitVerificationToken(
            orderId = paymentData?.orderId ?: return,
            paymentId = razorpayPaymentId ?: return,
            signature = paymentData.signature ?: return
        )
    }

    override fun onPaymentError(errorCode: Int, errorDescription: String?, paymentData: PaymentData?) {
        paymentViewModel.onPaymentError(errorCode, errorDescription)
    }
}
