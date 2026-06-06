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

sealed interface PaymentUiState {
    object Idle : PaymentUiState
    object ProcessingOrderCreation : PaymentUiState
    object SynchronizingVerificationLedger : PaymentUiState
    data class OrderReadyForCheckout(
        val orderId: String,
        val amount: Int,
        val keyId: String
    ) : PaymentUiState
    data class TransactionCompleted(
        val grossDeposit: Double,
        val convenienceFee: Double,
        val netAmountCredited: Double,
        val updatedBalance: Double
    ) : PaymentUiState
    data class Error(val reason: String) : PaymentUiState
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun initiateDepositSequence(amountPaise: Int) {
        viewModelScope.launch {
            _uiState.value = PaymentUiState.ProcessingOrderCreation
            try {
                val params = hashMapOf(
                    "amount" to amountPaise,
                    "currency" to "INR"
                )
                val result = functions
                    .getHttpsCallable("createRazorpayOrder")
                    .call(params)
                    .await()
                
                @Suppress("UNCHECKED_CAST")
                val data = result.data as? Map<String, Any>
                
                _uiState.value = PaymentUiState.OrderReadyForCheckout(
                    orderId = data?.get("orderId") as? String ?: "",
                    amount = amountPaise,
                    keyId = data?.get("keyId") as? String ?: ""
                )
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
                val result = functions
                    .getHttpsCallable("verifyPaymentSignature")
                    .call(params)
                    .await()
                
                @Suppress("UNCHECKED_CAST")
                val data = result.data as? Map<String, Any>
                
                _uiState.value = PaymentUiState.TransactionCompleted(
                    grossDeposit = (data?.get("grossDeposit") as? Number)?.toDouble() ?: 0.0,
                    convenienceFee = (data?.get("convenienceFee") as? Number)?.toDouble() ?: 0.0,
                    netAmountCredited = (data?.get("netAmountCredited") as? Number)?.toDouble() ?: 0.0,
                    updatedBalance = (data?.get("updatedBalance") as? Number)?.toDouble() ?: 0.0
                )
            } catch (e: Exception) {
                _uiState.value = PaymentUiState.Error("Verification failed: ${e.message}")
            }
        }
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
