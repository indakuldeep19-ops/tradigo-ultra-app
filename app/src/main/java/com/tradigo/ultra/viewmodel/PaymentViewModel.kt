package com.tradigo.ultra.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// UI State
sealed interface PaymentUiState {
    object Idle : PaymentUiState
    object ProcessingOrderCreation : PaymentUiState
    object SynchronizingVerificationLedger : PaymentUiState
    data class OrderReadyForCheckout(
        val orderId: String,
        val amount: Int, // paise
        val keyId: String // Razorpay publishable key
    ) : PaymentUiState
    data class TransactionCompleted(
        val grossDeposit: Double,
        val convenienceFee: Double,
        val netAmountCredited: Double,
        val updatedBalance: Double
    ) : PaymentUiState
    data class Error(val reason: String) : PaymentUiState
}

/**
 * PaymentViewModel
 *
 * Manages the two-step Razorpay payment flow:
 * Step 1 — [initiateDepositSequence]:
 * Calls the `createRazorpayOrder` Cloud Function → emits [OrderReadyForCheckout]
 * → the UI opens the Razorpay Checkout SDK with the returned orderId + keyId.
 *
 * Step 2 — [submitVerificationToken]:
 * Called from MainActivity.onPaymentSuccess with the three Razorpay callback
 * params → calls `verifyPaymentSignature` Cloud Function → credits the wallet
 * → emits [TransactionCompleted].
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow<<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<<PaymentUiState> = _uiState.asStateFlow()

    fun initiateDepositSequence(amountUsdt: Double) {
        if (amountUsdt <= 0) {
            _uiState.value = PaymentUiState.Error("Amount must be greater than zero")
            return
        }

        viewModelScope.launch {
            _uiState.value = PaymentUiState.ProcessingOrderCreation
            
            try {
                val params = hashMapOf(
                    "amount" to (amountUsdt * 100).toInt(), // Convert to paise
                    "currency" to "INR"
                )
                
                val result = functions.getHttpsCallable("createRazorpayOrder").call(params).await()
                val data = result.data as? Map<<*, *>
                
                val orderId = data?.get("orderId") as? String
                val amount = data?.get("amount") as? Int
                val keyId = data?.get("keyId") as? String
                
                if (orderId != null && amount != null && keyId != null) {
                    _uiState.value = PaymentUiState.OrderReadyForCheckout(orderId, amount, keyId)
                } else {
                    _uiState.value = PaymentUiState.Error("Invalid order response from server")
                }
            } catch (e: Exception) {
                _uiState.value = PaymentUiState.Error("Failed to create order: ${e.message}")
            }
        }
    }

    fun submitVerificationToken(orderId: String, paymentId: String, signature: String) {
        viewModelScope.launch {
            _uiState.value = PaymentUiState.SynchronizingVerificationLedger
            
            try {
                val params = hashMapOf(
                    "orderId" to orderId,
                    "paymentId" to paymentId,
                    "signature" to signature
                )
                
                val result = functions.getHttpsCallable("verifyPaymentSignature").call(params).await()
                val data = result.data as? Map<<*, *>
                
                val grossDeposit = data?.get("grossDeposit") as? Double
                val convenienceFee = data?.get("convenienceFee") as? Double
                val netAmountCredited = data?.get("netAmountCredited") as? Double
                val updatedBalance = data?.get("updatedBalance") as? Double
                
                if (grossDeposit != null && convenienceFee != null && 
                    netAmountCredited != null && updatedBalance != null) {
                    _uiState.value = PaymentUiState.TransactionCompleted(
                        grossDeposit, convenienceFee, netAmountCredited, updatedBalance
                    )
                } else {
                    _uiState.value = PaymentUiState.Error("Verification response incomplete")
                }
            } catch (e: Exception) {
                _uiState.value = PaymentUiState.Error("Payment verification failed: ${e.message}")
            }
        }
    }

    fun onPaymentCancelled() {
        _uiState.value = PaymentUiState.Error("Payment was cancelled or failed. Please retry.")
    }

    fun onPaymentError(errorCode: Int, errorDescription: String?) {
        val msg = errorDescription?.takeIf { it.isNotBlank() } 
            ?: "Payment failed (code=$errorCode). Please retry."
        _uiState.value = PaymentUiState.Error(msg)
    }

    fun resetState() {
        _uiState.value = PaymentUiState.Idle
    }
}
