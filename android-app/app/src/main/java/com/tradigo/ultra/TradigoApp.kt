package com.tradigo.ultra

import android.app.Application
import com.tradigo.ultra.engine.TradingSreEngine
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TradigoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Start background telemetry
        TradingSreEngine.startTelemetryMonitoring()
    }
}
