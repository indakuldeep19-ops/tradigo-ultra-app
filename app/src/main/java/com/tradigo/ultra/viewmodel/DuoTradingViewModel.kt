package com.tradigo.ultra.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradigo.ultra.repository.DuoBuddy
import com.tradigo.ultra.repository.DuoTradingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI state
sealed interface DuoUiState {
    object Idle : DuoUiState
    object Loading : DuoUiState
    object AcceptanceSuccess : DuoUiState
    data class RequestCreated(val requestId: String) : DuoUiState
    data class ExecutionSuccess(
        val duoTradeId: String,
        val symbol: String,
        val side: String
    ) : DuoUiState
    data class Error(val message: String) : DuoUiState
}

@HiltViewModel
class DuoTradingViewModel @Inject constructor(
    private val repository: DuoTradingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DuoUiState>(DuoUiState.Idle)
    val uiState: StateFlow<DuoUiState> = _uiState.asStateFlow()

    // Current request context (survives state transitions)
    var activeRequestId: String? = null
        private set
    var activeSymbol: String = "BTC/USDT"
        private set
    var activeSide: String = "CALL"
        private set

    val buddyList = listOf(
        DuoBuddy(uid = "friend_uid_101", email = "alex@tradigo.io"),
        DuoBuddy(uid = "friend_uid_202", email = "sarah@quantum.net"),
        DuoBuddy(uid = "friend_uid_303", email = "raj@tradigo.io")
    )

    /**
     * Initiator — create a Duo request and invite a friend.
     */
    fun initializeDuoSession(
        friendId: String,
        symbol: String,
        side: String,
        amountA: Double
    ) {
        if (amountA <= 0) {
            _uiState.value = DuoUiState.Error("Amount must be greater than zero.")
            return
        }

        activeSymbol = symbol
        activeSide = side

        viewModelScope.launch {
            _uiState.value = DuoUiState.Loading
            
            repository.createDuoRequest(friendId, symbol, side, amountA)
                .onSuccess { result ->
                    activeRequestId = result.requestId
                    _uiState.value = DuoUiState.RequestCreated(result.requestId)
                }
                .onFailure { e ->
                    _uiState.value = DuoUiState.Error(
                        e.localizedMessage ?: "Failed to create Duo request."
                    )
                }
        }
    }

    /**
     * Friend (User B) — accept an incoming Duo invite and commit their amount.
     */
    fun completeBuddyAcceptance(requestId: String, amountB: Double) {
        if (amountB <= 0) {
            _uiState.value = DuoUiState.Error("Amount must be greater than zero.")
            return
        }

        viewModelScope.launch {
            _uiState.value = DuoUiState.Loading
            
            repository.acceptDuoRequest(requestId, amountB)
                .onSuccess {
                    activeRequestId = requestId
                    _uiState.value = DuoUiState.AcceptanceSuccess
                }
                .onFailure { e ->
                    _uiState.value = DuoUiState.Error(
                        e.localizedMessage ?: "Failed to accept Duo request."
                    )
                }
        }
    }

    /**
     * Initiator — after friend has accepted, fire the synchronized atomic execution.
     */
    fun triggerTwinExecution(requestId: String) {
        viewModelScope.launch {
            _uiState.value = DuoUiState.Loading
            
            repository.executeDuoTrade(requestId)
                .onSuccess { result ->
                    _uiState.value = DuoUiState.ExecutionSuccess(
                        duoTradeId = result.duoTradeId,
                        symbol = activeSymbol,
                        side = activeSide
                    )
                }
                .onFailure { e ->
                    _uiState.value = DuoUiState.Error(
                        e.localizedMessage ?: "Synchronized execution failed."
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = DuoUiState.Idle
        activeRequestId = null
    }
}
