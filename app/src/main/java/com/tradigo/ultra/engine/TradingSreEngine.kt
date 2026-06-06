package com.tradigo.ultra.engine

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

// Traffic tier enum
enum class NetworkTrafficTier {
    OPTIMAL,   // <150ms — full render, all indicators active
    HEAVY,     // 150–400ms — skip micro-renders, reduce overlay complexity
    CRITICAL   // >400ms or no connection — minimum viable chart only
}

/**
 * TradingSreEngine
 *
 * Monitors real-time API latency every [POLL_INTERVAL_MS] milliseconds
 * and exposes a [NetworkTrafficTier] StateFlow that UI components react to.
 *
 * Usage:
 * // In Application.onCreate() or a ViewModel init block:
 * TradingSreEngine.startTelemetryMonitoring()
 *
 * // In any Composable:
 * val tier by TradingSreEngine.trafficTier.collectAsState()
 *
 * PRODUCTION NOTE:
 * Replace PING_URL with your actual health-check endpoint, e.g.
 * "https://api.tradigoultra.com/healthz"
 * The endpoint should return HTTP 200 with an empty or minimal body.
 */
object TradingSreEngine {
    private const val TAG = "TradingSreEngine"
    
    // Backend exposes GET /api/ping (see server/src/server.ts)
    private const val PING_URL = "https://api.tradigoultra.com/api/ping"
    private const val CONNECT_TIMEOUT = 2_000 // ms
    private const val READ_TIMEOUT = 2_000 // ms
    private const val POLL_INTERVAL_MS = 5_000L // 5s between checks
    private const val LATENCY_OPTIMAL = 150 // ms threshold
    private const val LATENCY_HEAVY = 400 // ms threshold

    // SupervisorJob keeps the monitoring loop alive even if a child coroutine fails
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _trafficTier = MutableStateFlow(NetworkTrafficTier.OPTIMAL)
    val trafficTier: StateFlow<<NetworkTrafficTier> = _trafficTier.asStateFlow()
    
    /** Latency of the last successful ping in ms, or -1 if the last ping failed. */
    private val _lastLatencyMs = MutableStateFlow(-1L)
    val lastLatencyMs: StateFlow<Long> = _lastLatencyMs.asStateFlow()

    @Volatile
    private var isRunning = false

    /**
     * Starts the background telemetry loop.
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    fun startTelemetryMonitoring() {
        if (isRunning) return
        isRunning = true
        
        scope.launch {
            while (true) {
                val latency = measureLatency()
                _lastLatencyMs.value = latency
                
                _trafficTier.value = when {
                    latency < 0 -> NetworkTrafficTier.CRITICAL
                    latency < LATENCY_OPTIMAL -> NetworkTrafficTier.OPTIMAL
                    latency < LATENCY_HEAVY -> NetworkTrafficTier.HEAVY
                    else -> NetworkTrafficTier.CRITICAL
                }
                
                Log.d(TAG, "Latency: ${latency}ms, Tier: ${_trafficTier.value}")
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
            connection.connect()
            val responseCode = connection.responseCode
            connection.disconnect()
            
            if (responseCode == 200) {
                System.currentTimeMillis() - start
            } else {
                -1L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ping failed: ${e.message}")
            -1L
        }
    }

    fun statusSummary(): String {
        val tier = _trafficTier.value
        val latency = _lastLatencyMs.value
        return "Tier: $tier | Latency: ${if (latency < 0) "N/A" else "${latency}ms"}"
    }
}
