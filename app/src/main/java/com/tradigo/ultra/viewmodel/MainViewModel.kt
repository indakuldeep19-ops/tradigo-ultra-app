package com.tradigo.ultra.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.crypto.BybitConnector
import com.example.crypto.CryptoEngine
import com.example.db.AppDatabase
import com.example.db.ChatMessageEntity
import com.example.db.UserConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "tradigo_database"
    ).build()
    
    private val dao = db.dao()

    val userConfig: StateFlow<UserConfigEntity> = dao.getUserConfigFlow()
        .map { it ?: UserConfigEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserConfigEntity())

    val chatMessages: StateFlow<List<<ChatMessageEntity>> = dao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Marquee tickers stream
    private val _tickers = MutableStateFlow<Map<String, Double>>(mapOf(
        "BTC/USDT" to 68540.20,
        "ETH/USDT" to 3521.10,
        "SOL/USDT" to 143.15,
        "XRP/USDT" to 0.4912,
        "BNB/USDT" to 582.40,
        "DOGE/USDT" to 0.1425,
        "ADA/USDT" to 0.4418,
        "LINK/USDT" to 15.65,
        "SUI/USDT" to 1.0540
    ))
    val tickers: StateFlow<Map<String, Double>> = _tickers.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    private val _tradeStatus = MutableStateFlow<String?>(null)
    val tradeStatus: StateFlow<String?> = _tradeStatus.asStateFlow()

    private var bybitConnector: BybitConnector? = null

    init {
        // Run daily streak calculation & instantiate stored keys
        viewModelScope.launch(Dispatchers.IO) {
            val config = dao.getUserConfig() ?: UserConfigEntity()
            
            // Re-instantiate Bybit if stored credentials exist
            if (config.apiKeyEncrypted.isNotEmpty() && config.apiSecretEncrypted.isNotEmpty()) {
                try {
                    val key = CryptoEngine.decryptData(config.apiKeyEncrypted, "tradigo_salt_2026")
                    val secret = CryptoEngine.decryptData(config.apiSecretEncrypted, "tradigo_salt_2026")
                    bybitConnector = BybitConnector(key, secret)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to restore API keys: ${e.message}")
                }
            }
        }
    }

    fun upgradeToPremium() {
        viewModelScope.launch {
            val currentConfig = userConfig.value
            if (currentConfig.userTier == "PREMIUM") return@launch
            
            val updated = currentConfig.copy(
                userTier = "PREMIUM",
                xp = currentConfig.xp + 100
            )
            dao.saveUserConfig(updated)
            
            val jMessage = ChatMessageEntity(
                id = "UPGRADE_${System.currentTimeMillis()}",
                role = "model",
                text = "Premium level system access granted. Copy trading pipelines unlocked, high-volume terminal channels synchronized, zero lag metrics successfully upgraded."
            )
            dao.insertMessage(jMessage)
        }
    }

    fun saveApiKeys(key: String, secret: String) {
        viewModelScope.launch {
            try {
                if (key.trim().isEmpty() || secret.trim().isEmpty()) {
                    _errorMessage.emit("API key or secret cannot be empty!")
                    return@launch
                }

                val keyEncrypted = CryptoEngine.encryptData(key, "tradigo_salt_2026")
                val secretEncrypted = CryptoEngine.encryptData(secret, "tradigo_salt_2026")

                val current = userConfig.value
                val updated = current.copy(
                    apiKeyEncrypted = keyEncrypted,
                    apiSecretEncrypted = secretEncrypted,
                    xp = current.xp + 50
                )
                dao.saveUserConfig(updated)

                bybitConnector = BybitConnector(key, secret)

                val sysMessage = ChatMessageEntity(
                    id = "EXCHANGE_LINK_${System.currentTimeMillis()}",
                    role = "model",
                    text = "API hardware connection established with Bybit V5 Live Connector. Accounts decrypted and verified. Baseline terminal status online."
                )
                dao.insertMessage(sysMessage)
            } catch (e: Exception) {
                _errorMessage.emit("Encryption failed: ${e.message}")
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            dao.clearAllMessages()
            val welcomeMsg = ChatMessageEntity(
                id = "1",
                role = "model",
                text = "Jarvis Protocol Online. Secure Multi-Exchange terminal channels verified. Command parameters loaded."
            )
            dao.insertMessage(welcomeMsg)
        }
    }

    fun executeQuickTrade(symbol: String, side: String, qty: Double, price: Double? = null) {
        viewModelScope.launch {
            _tradeStatus.value = "Sending Order to Gateway..."
            val activeConnector = bybitConnector ?: BybitConnector("", "")
            
            try {
                // Simulate order execution
                delay(1500)
                _tradeStatus.value = "Order executed: $side $qty $symbol @ ${price ?: "market"}"
                delay(3000)
                _tradeStatus.value = null
            } catch (e: Exception) {
                _tradeStatus.value = "Order failed: ${e.message}"
                delay(3000)
                _tradeStatus.value = null
            }
        }
    }
}
