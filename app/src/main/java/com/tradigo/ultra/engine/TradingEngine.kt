package com.tradigo.ultra.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

data class PriceTick(
    val symbol: String,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * TradingEngine
 * 
 * Simulates live price feeds for demo purposes.
 * Replace with real WebSocket connection to Bybit/Binance.
 */
class TradingEngine {
    
    private val seedPrices = mapOf(
        "BTC/USDT" to 65_000.0,
        "ETH/USDT" to 3_500.0,
        "SOL/USDT" to 160.0,
        "BNB/USDT" to 590.0,
        "XRP/USDT" to 0.52
    )
    
    fun streamLivePrices(symbol: String): Flow<<PriceTick> = flow {
        val basePrice = seedPrices[symbol] ?: 1.0
        var currentPrice = basePrice
        
        while (true) {
            // Simulate small random price movements
            val change = (Random.nextDouble() - 0.5) * basePrice * 0.002
            currentPrice += change
            currentPrice = kotlin.math.max(currentPrice, basePrice * 0.5)
            
            emit(PriceTick(symbol, currentPrice))
            delay(1000) // Update every second
        }
    }
    
    fun getHistoricalCandles(symbol: String): List<CandleNode> {
        // Return sample data - replace with API call
        return generateSampleCandles(symbol)
    }
    
    private fun generateSampleCandles(symbol: String): List<CandleNode> {
        val basePrice = seedPrices[symbol] ?: 1.0
        val candles = mutableListOf<CandleNode>()
        var currentPrice = basePrice
        val now = System.currentTimeMillis()
        
        for (i in 7 downTo 0) {
            val open = currentPrice
            val high = open * (1 + Random.nextDouble() * 0.01)
            val low = open * (1 - Random.nextDouble() * 0.01)
            val close = low + (high - low) * Random.nextDouble()
            val timestamp = now - (i * 60_000)
            
            candles.add(CandleNode(open, high, low, close, timestamp))
            currentPrice = close
        }
        
        return candles
    }
}
