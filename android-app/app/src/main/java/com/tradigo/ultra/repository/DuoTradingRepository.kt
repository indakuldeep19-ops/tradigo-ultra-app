package com.tradigo.ultra.repository

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class DuoBuddy(
    val uid: String,
    val email: String,
    val name: String = email.substringBefore("@")
)

data class DuoRequestResult(val requestId: String)
data class DuoExecutionResult(val duoTradeId: String)

@Singleton
class DuoTradingRepository @Inject constructor(
    private val functions: FirebaseFunctions
) {
    suspend fun createDuoRequest(
        friendId: String,
        symbol: String,
        side: String,
        amountA: Double
    ): Result<DuoRequestResult> = safeCall {
        val params = hashMapOf(
            "friendId" to friendId,
            "symbol" to symbol,
            "side" to side,
            "amountA" to amountA
        )
        val result = functions.getHttpsCallable("createDuoRequest").call(params).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any>
        val id = data?.get("requestId") as? String
            ?: error("Missing requestId")
        DuoRequestResult(requestId = id)
    }

    suspend fun acceptDuoRequest(requestId: String, amountB: Double): Result<Unit> = safeCall {
        val params = hashMapOf(
            "requestId" to requestId,
            "amountB" to amountB
        )
        functions.getHttpsCallable("acceptDuoRequest").call(params).await()
    }

    suspend fun executeDuoTrade(requestId: String): Result<DuoExecutionResult> = safeCall {
        val params = hashMapOf("requestId" to requestId)
        val result = functions.getHttpsCallable("executeDuoTrade").call(params).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any>
        val id = data?.get("duoTradeId") as? String
            ?: error("Missing duoTradeId")
        DuoExecutionResult(duoTradeId = id)
    }

    private suspend inline fun <T> safeCall(crossinline block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
}
