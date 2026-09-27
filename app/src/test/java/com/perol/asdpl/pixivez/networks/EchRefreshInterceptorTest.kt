package com.perol.asdpl.pixivez.networks

import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class EchRefreshInterceptorTest {
    private fun cache() = EchConfigCache({ EchConfig(byteArrayOf(1), emptyList(), 300) })

    @Test fun invalidatesMisdirectedGetWithoutReusingTheSameConnection() {
        var loads = 0
        val cache = EchConfigCache({ loads++; EchConfig(byteArrayOf(1), emptyList(), 300) })
        cache.get()
        val chain = FakeChain(Request.Builder().url("https://app-api.pixiv.net/test").build()) {
            cache.get()
            421
        }
        EchRefreshInterceptor(cache).intercept(chain).close()
        assertEquals(1, chain.attempts)
        cache.get()
        assertEquals(2, loads)
    }

    @Test fun retriesFailedGetAtMostOnce() {
        val chain = FakeChain(Request.Builder().url("https://app-api.pixiv.net/test").build()) {
            throw IOException("handshake failed")
        }
        try {
            EchRefreshInterceptor(cache()).intercept(chain)
            fail("second failure must escape")
        } catch (_: IOException) { assertEquals(2, chain.attempts) }
    }

    @Test fun neverReplaysPostAfterResponseOrIoFailure() {
        for (fails in listOf(false, true)) {
            val chain = FakeChain(Request.Builder().url("https://oauth.secure.pixiv.net/auth/token")
                .post("test".toRequestBody()).build()) {
                if (fails) throw IOException("lost connection")
                421
            }
            try {
                EchRefreshInterceptor(cache()).intercept(chain).close()
                assertFalse(fails)
            } catch (_: IOException) { assertTrue(fails) }
            assertEquals(1, chain.attempts)
        }
    }

    @Test fun leavesOtherHostsAndCancelledCallsAlone() {
        val unrelated = FakeChain(Request.Builder().url("https://example.org/test").build()) { 421 }
        EchRefreshInterceptor(cache()).intercept(unrelated).close()
        assertEquals(1, unrelated.attempts)
        val cancelled = FakeChain(Request.Builder().url("https://app-api.pixiv.net/test").build()) { 421 }
        cancelled.call().cancel()
        EchRefreshInterceptor(cache()).intercept(cancelled).close()
        assertEquals(1, cancelled.attempts)
    }

    private class FakeChain(private val request: Request, private val next: () -> Int) : Interceptor.Chain {
        var attempts = 0
        private val call = OkHttpClient().newCall(request)
        override fun request() = request
        override fun proceed(request: Request): Response {
            attempts++
            return Response.Builder().request(request).code(next()).message("test")
                .protocol(Protocol.HTTP_1_1).body("".toResponseBody()).build()
        }
        override fun connection(): Connection? = null
        override fun call() = call
        override fun connectTimeoutMillis() = 1_000
        override fun readTimeoutMillis() = 1_000
        override fun writeTimeoutMillis() = 1_000
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }
}
