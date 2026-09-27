package com.perol.asdpl.pixivez.networks

import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class EchConfigCacheTest {
    private class Clock(var value: Long = 0L) {
        fun now(): Long = value
    }

    private val validEch = byteArrayOf(
        0, 6, // ECHConfigList payload length
        0xfe.toByte(), 0x0d, // ECHConfig version
        0, 2, // ECHConfig contents length
        1, 2,
    )

    @Test fun parserReadsQuotedAndUnquotedHttpsParams() {
        val encoded = validEch.toByteString().base64()
        val quoted = parseDnsJson(
            "1 . alpn=\"h2\" ipv4hint=\"192.0.2.1,198.51.100.4\" ech=\"$encoded\"",
            ttl = 123,
        )
        val unquoted = parseDnsJson(
            "1 . ech=$encoded ipv4hint=192.0.2.1,198.51.100.4",
            ttl = 7,
        )

        assertArrayEquals(validEch, quoted.bytes)
        assertEquals(listOf("192.0.2.1", "198.51.100.4"), quoted.ipv4Hints)
        assertEquals(123L, quoted.ttlSeconds)
        assertArrayEquals(validEch, unquoted.bytes)
        assertEquals(7L, unquoted.ttlSeconds)
    }

    @Test fun parserIgnoresNonHttpsAnswersButRejectsPollutionAndMissingEch() {
        val encoded = validEch.toByteString().base64()
        val onlyHttps = """
            {"Status":0,"Answer":[
              {"type":1,"TTL":60,"data":"1 . ech=$encoded"},
              {"type":65,"TTL":60,"data":"1 . ech=%%%"}
            ]}
        """.trimIndent()
        assertThrowsIOException { EchConfigParser.parse(onlyHttps) }

        assertThrowsIOException {
            parseDnsJson("1 . alpn=\"h2\" ipv4hint=192.0.2.1", ttl = 60)
        }
    }

    @Test fun parserRejectsInvalidFramingAndIpv4Hints() {
        val malformedLength = byteArrayOf(
            0, 7, // claims one byte more than the payload
            0xfe.toByte(), 0x0d, 0, 2, 1, 2,
        ).toByteString().base64()
        assertThrowsIOException {
            EchConfigParser.parse(parseDnsJsonText("1 . ech=\"$malformedLength\"", 60))
        }

        val encoded = validEch.toByteString().base64()
        assertThrowsIOException {
            EchConfigParser.parse(parseDnsJsonText("1 . ech=\"$encoded\" ipv4hint=192.0.2.999", 60))
        }
        assertThrowsIOException {
            EchConfigParser.parse(parseDnsJsonText("1 . ech=\"$encoded\" ipv4hint=192.0.2", 60))
        }
    }

    @Test fun cacheCapsPositiveTtlAtOneDayAndRefreshesZeroTtl() {
        val clock = Clock()
        var calls = 0
        val cache = EchConfigCache({
            calls++
            EchConfig(byteArrayOf(calls.toByte()), emptyList(), 2 * 24 * 60 * 60)
        }, clock::now)

        val first = cache.get()
        clock.value = 24 * 60 * 60 * 1_000L - 1
        assertSame(first, cache.get())
        clock.value++
        assertEquals(2, cache.get().bytes[0].toInt())
        assertEquals(2, calls)

        val zeroTtl = EchConfigCache({
            calls++
            EchConfig(byteArrayOf(calls.toByte()), emptyList(), 0)
        }, clock::now)
        zeroTtl.get()
        zeroTtl.get()
        assertEquals(4, calls)
    }

    @Test fun concurrentColdCallersShareOneLoaderInvocation() {
        val calls = AtomicInteger()
        val cache = EchConfigCache({
            calls.incrementAndGet()
            Thread.sleep(30)
            EchConfig(validEch, emptyList(), 60)
        }, { 0L })
        val pool = Executors.newFixedThreadPool(8)
        try {
            val results = pool.invokeAll(List(24) { Callable { cache.get() } })
                .map { it.get(2, TimeUnit.SECONDS) }
            assertEquals(1, calls.get())
            results.forEach { assertArrayEquals(validEch, it.bytes) }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test fun failedRefreshBacksOffAndServesLastGoodForOneDay() {
        val clock = Clock()
        var calls = 0
        var fail = false
        val first = EchConfig(byteArrayOf(1), emptyList(), 1)
        val cache = EchConfigCache({
            calls++
            if (fail) throw IOException("offline")
            first
        }, clock::now)

        assertSame(first, cache.get())
        fail = true
        clock.value = 1_001
        assertSame(first, cache.get())
        assertEquals(2, calls)
        clock.value += 29_999
        assertSame(first, cache.get())
        assertEquals(2, calls)
        clock.value++
        assertSame(first, cache.get())
        assertEquals(3, calls)
    }

    @Test fun firstFailureIsIoExceptionAndRetriesAfterBackoff() {
        val clock = Clock()
        var calls = 0
        val cache = EchConfigCache({
            calls++
            throw IOException("offline")
        }, clock::now)

        assertThrowsIOException { cache.get() }
        clock.value = 29_999
        assertThrowsIOException { cache.get() }
        assertEquals(1, calls)
        clock.value = 30_000
        assertThrowsIOException { cache.get() }
        assertEquals(2, calls)
    }

    @Test fun expiredLastGoodCannotMaskAnOutagePastOneDay() {
        val clock = Clock()
        var fail = false
        val cache = EchConfigCache({
            if (fail) throw IOException("offline")
            EchConfig(byteArrayOf(1), emptyList(), 1)
        }, clock::now)

        cache.get()
        fail = true
        clock.value = 24 * 60 * 60 * 1_000L + 1
        assertThrowsIOException { cache.get() }
    }

    @Test fun invalidateClearsFreshValueButPreservesLastGoodAndIsRateLimited() {
        val clock = Clock()
        var calls = 0
        var fail = false
        val first = EchConfig(byteArrayOf(1), emptyList(), 60 * 60)
        val cache = EchConfigCache({
            calls++
            if (fail) throw IOException("offline")
            first
        }, clock::now)

        assertSame(first, cache.get())
        clock.value = 1_000
        assertTrue(cache.invalidate())
        clock.value = 2_000
        assertFalse(cache.invalidate())
        fail = true
        assertSame(first, cache.get())
        assertEquals(2, calls)
        clock.value = 61_000
        assertTrue(cache.invalidate())
        assertSame(first, cache.get())
        assertEquals(3, calls)
    }

    private fun parseDnsJson(data: String, ttl: Long): EchConfig =
        EchConfigParser.parse(parseDnsJsonText(data, ttl))

    private fun parseDnsJsonText(data: String, ttl: Long): String =
        "{\"Status\":0,\"Answer\":[{\"type\":65,\"TTL\":$ttl,\"data\":\"${data.replace("\"", "\\\"")}\"}]}"

    private fun assertThrowsIOException(block: () -> Unit) {
        try {
            block()
        } catch (_: IOException) {
            return
        }
        throw AssertionError("Expected IOException")
    }
}
