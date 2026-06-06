package com.tradigo.ultra.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL

enum class NetworkTrafficTier {
    OPTIMAL,   // <150ms
    HEAVY,     // 150-400ms
    CRITICAL   // >400ms
}

object TradingSreEngine {
    private const val PING_URL = "https://api.tradigoultra.com/api/ping"
    private const val CONNECT_TIMEOUT = 2000
    private const val READ_TIMEOUT = 2000
    private const val POLL_INTERVAL_MS = 5000L
    private const val LATENCY_OPTIMAL = 150
    private const val LATENCY_HEAVY = 400

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _trafficTier = MutableStateFlow(NetworkTrafficTier.OPTIMAL)
    val trafficTier: StateFlow<NetworkTrafficTier> = _trafficTier.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow(-1L)
    val lastLatencyMs: StateFlow<Long> = _lastLatencyMs.asStateFlow()

    @Volatile
    private var isRunning = false

    fun startTelemetryMonitoring() {
        if (isRunning) return
        isRunning = true

        scope.launch {
            while (isActive) {
                val latency = measureLatency()
                _lastLatencyMs.value = latency
                
                _trafficTier.value = when {
                    latency < 0 -> NetworkTrafficTier.CRITICAL
                    latency < LATENCY_OPTIMAL -> NetworkTrafficTier.OPTIMAL
                    latency < LATENCY_HEAVY -> NetworkTrafficTier.HEAVY
                    else -> NetworkTrafficTier.CRITICAL
                }
                
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun measureLatency(): Long {
        return try {
            val start = System.currentTimeMillis()
            val connection = URL(PING_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.requestMethod = "GET"
            connection.responseCode
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            -1
        }
    }

    fun statusSummary(): String {
        val tier = _trafficTier.value
        val latency = _lastLatencyMs.value
        return "Tier: ${tier.name} | Latency: ${if (latency < 0) "N/A" else "${latency}ms"}"
    }
}
