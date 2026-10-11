package com.perol.asdpl.pixivez.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/** Shares a local read, retaining failures until an explicit retry instead of treating them as logout. */
internal class LocalInitializationGate<T>(private val initialize: suspend () -> T) {
    private var attempt: Deferred<Result<T>>? = null

    @Synchronized
    fun start(scope: CoroutineScope): Deferred<Result<T>> = attempt ?: newAttempt(scope)

    @Synchronized
    fun retry(scope: CoroutineScope): Deferred<Result<T>> =
        attempt?.takeUnless { it.isCompleted } ?: newAttempt(scope)

    private fun newAttempt(scope: CoroutineScope): Deferred<Result<T>> =
        scope.async(Dispatchers.IO) {
            try {
                Result.success(initialize())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
        }.also { attempt = it }
}
