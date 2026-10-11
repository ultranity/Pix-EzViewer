package com.perol.asdpl.pixivez.networks

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException

class AuthRefreshBoundaryTest {
    private class TrackingBody : ResponseBody() {
        var closed = false
        override fun contentType() = null
        override fun contentLength() = 0L
        override fun source(): BufferedSource = Buffer()
        override fun close() { closed = true }
    }
    private fun response(body: ResponseBody) = Response.Builder()
        .request(Request.Builder().url("https://app-api.pixiv.net/test").build())
        .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").body(body).build()

    @Test fun oauth405BecomesIoFailureAndClosesOriginalResponse() {
        val body = TrackingBody()
        val error = HttpException(retrofit2.Response.error<Any>(405, "".toResponseBody()))
        try {
            refreshAuthentication(response(body)) { throw error }
            fail("must not report refresh success")
        } catch (e: IOException) {
            assertSame(error, e.cause)
        }
        assertTrue(body.closed)
    }

    @Test fun asyncOkHttpDelivers405RefreshFailureToCallback() {
        val completed = java.util.concurrent.CountDownLatch(1)
        val failure = java.util.concurrent.atomic.AtomicReference<IOException>()
        val body = TrackingBody()
        val client = okhttp3.OkHttpClient.Builder().addInterceptor {
            refreshAuthentication(response(body)) {
                throw HttpException(retrofit2.Response.error<Any>(405, "".toResponseBody()))
            }
            error("failed refresh must not replay request")
        }.build()
        try {
            client.newCall(Request.Builder().url("https://app-api.pixiv.net/test").build())
                .enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: IOException) {
                        failure.set(e)
                        completed.countDown()
                    }
                    override fun onResponse(call: okhttp3.Call, response: Response) {
                        response.close()
                        completed.countDown()
                    }
                })
            assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS))
            assertEquals(405, (failure.get()?.cause as? HttpException)?.code())
            assertTrue(body.closed)
        } finally {
            client.dispatcher.executorService.shutdownNow()
            client.connectionPool.evictAll()
        }
    }

    @Test fun offlineFailureClosesResponseAndLaterRefreshCanSucceed() {
        val body = TrackingBody()
        val error = IOException("offline")
        try {
            refreshAuthentication(response(body)) { throw error }
            fail("must fail offline")
        } catch (e: IOException) { assertSame(error, e) }
        assertTrue(body.closed)
        val retryBody = TrackingBody()
        var refreshed = false
        refreshAuthentication(response(retryBody)) { refreshed = true }
        assertTrue(refreshed)
        assertTrue(retryBody.closed)
    }
}
