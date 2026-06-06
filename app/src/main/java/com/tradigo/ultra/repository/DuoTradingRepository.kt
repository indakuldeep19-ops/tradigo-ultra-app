package com.tradigo.ultra.repository

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Domain models
data class DuoBuddy(
    val uid: String,
    val email: String,
    val name: String = email.substringBefore("@")
)

data class DuoRequestResult(val requestId: String)
data class DuoExecutionResult(val duoTradeId: String)

/**
 * DuoTradingRepository
 *
 * Wraps the three Firebase Cloud Functions that power the Duo Trading feature:
 * createDuoRequest — initiator opens a request and invites a friend
 * acceptDuoRequest — friend allocates their amount and confirms
 * executeDuoTrade — initiator triggers simultaneous atomic execution
 *
 * All calls return Kotlin [Result] so the ViewModel can handle
 * success/failure without try-catch boilerplate.
 */
@Singleton
class DuoTradingRepository @Inject constructor(
    private val functions: FirebaseFunctions
) {
    
    // Create
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
        val map = result.data as? Map<<*, *>
        val id = map?.get("requestId") as? String
            ?: error("Missing requestId in Cloud Function response.")
        
        DuoRequestResult(requestId = id)
    }

    // Accept
    suspend fun acceptDuoRequest(
        requestId: String,
        amountB: Double
    ): Result<Unit> = safeCall {
        val params = hashMapOf(
            "requestId" to requestId,
            "amountB" to amountB
        )
        functions.getHttpsCallable("acceptDuoRequest").call(params).await()
    }

    // Execute
    suspend fun executeDuoTrade(
        requestId: String
    ): Result<DuoExecutionResult> = safeCall {
        val params = hashMapOf("requestId" to requestId)
        val result = functions.getHttpsCallable("executeDuoTrade").call(params).await()
        val map = result.data as? Map<<*, *>
        val id = map?.get("duoTradeId") as? String
            ?: error("Missing duoTradeId in Cloud Function response.")
        
        DuoExecutionResult(duoTradeId = id)
    }

    private suspend inline fun <T> safeCall(crossinline block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
}
