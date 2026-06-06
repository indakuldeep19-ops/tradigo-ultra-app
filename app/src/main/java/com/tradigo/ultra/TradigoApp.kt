package com.tradigo.ultra

import android.app.Application
import com.tradigo.ultra.engine.TradingSreEngine
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class annotated for Hilt dependency injection.
 * Must match android:name=".TradigoApp" in AndroidManifest.xml
 */
@HiltAndroidApp
class TradigoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Start background telemetry so UI can adapt to network tier (OPTIMAL/HEAVY/CRITICAL)
        TradingSreEngine.startTelemetryMonitoring()
    }
}
