package com.perol.asdpl.pixivez.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class LocalInitializationGateTest {
    @Test
    fun concurrentCallersShareThePendingLocalRead() = runBlocking {
        val calls = AtomicInteger()
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.Default)
        try {
            val release = CompletableDeferred<Unit>()
            val gate = LocalInitializationGate {
                calls.incrementAndGet()
                release.await()
                "ready"
            }

            val first = gate.start(scope)
            val second = gate.start(scope)

            assertSame(first, second)
            assertFalse(first.isCompleted)
            release.complete(Unit)
            assertEquals("ready", first.await().getOrThrow())
            assertEquals(1, calls.get())
        } finally {
            job.cancel()
        }
    }

    @Test
    fun failedLocalReadDoesNotLookLikeAnEmptyResultAndCanBeRetried() = runBlocking {
        val calls = AtomicInteger()
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.Default)
        try {
            val gate = LocalInitializationGate {
                if (calls.incrementAndGet() == 1) error("database unavailable")
                "ready"
            }

            val failed = gate.start(scope).await()
            assertTrue(failed.isFailure)

            val retried = gate.retry(scope).await()
            assertEquals("ready", retried.getOrThrow())
            assertEquals(2, calls.get())
        } finally {
            job.cancel()
        }
    }
}
