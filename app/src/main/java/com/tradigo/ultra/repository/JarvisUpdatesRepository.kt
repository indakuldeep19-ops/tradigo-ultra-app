package com.tradigo.ultra.repository

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JarvisUpdatesRepository
 *
 * Minimal bridge to the new Firebase Functions:
 * - logJarvisEvent
 * - sendJarvisEmailUpdate
 *
 * NOTE: This does not change existing app flows; it only adds capability.
 */
@Singleton
class JarvisUpdatesRepository @Inject constructor(
    private val functions: FirebaseFunctions
) {
    
    suspend fun logEvent(
        type: String,
        payload: Map<String, Any?> = emptyMap(),
        notify: Boolean = false,
        title: String? = null,
        message: String? = null,
        severity: String? = null
    ): Result<Map<String, Any>> = safeCall {
        val params = hashMapOf(
            "type" to type,
            "payload" to payload,
            "notify" to notify,
            "title" to title,
            "message" to message,
            "severity" to severity
        )
        val result = functions.getHttpsCallable("logJarvisEvent").call(params).await()
        @Suppress("UNCHECKED_CAST")
        result.data as? Map<String, Any> ?: emptyMap()
    }

    suspend fun sendEmailUpdate(
        subject: String,
        body: String
    ): Result<Map<String, Any>> = safeCall {
        val params = hashMapOf("subject" to subject, "body" to body)
        val result = functions.getHttpsCallable("sendJarvisEmailUpdate").call(params).await()
        @Suppress("UNCHECKED_CAST")
        result.data as? Map<String, Any> ?: emptyMap()
    }

    private suspend inline fun <T> safeCall(crossinline block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
}
