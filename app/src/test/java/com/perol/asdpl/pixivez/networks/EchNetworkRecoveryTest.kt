package com.perol.asdpl.pixivez.networks

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class EchNetworkRecoveryTest {
    @Test fun offlineColdStartCanRetryImmediatelyOnNewNetwork() {
        var online = false
        var loads = 0
        val config = EchConfig(byteArrayOf(1), emptyList(), 300)
        val cache = EchConfigCache({
            loads++
            if (!online) throw IOException("offline")
            config
        }, now = { 1000L })
        repeat(2) {
            try { cache.get(); fail("offline") } catch (_: IOException) { }
        }
        assertEquals(1, loads)
        online = true
        cache.onNetworkAvailable()
        assertSame(config, cache.get())
        assertEquals(2, loads)
    }

    @Test fun networkCallbackPreservesFreshConfigAndDoesNotPerformIo() {
        var loads = 0
        val config = EchConfig(byteArrayOf(1), emptyList(), 300)
        val cache = EchConfigCache({ loads++; config }, now = { 1000L })
        cache.get()
        repeat(3) { cache.onNetworkAvailable() }
        assertEquals(1, loads)
        assertSame(config, cache.get())
        assertEquals(1, loads)
    }
}
