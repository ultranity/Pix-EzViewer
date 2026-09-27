package com.perol.asdpl.pixivez.networks

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.OkHttpClient
import okhttp3.Request
import org.conscrypt.Conscrypt
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

/** Guards against Conscrypt silently dropping the ECH policy on Android. No internet/account needed. */
@RunWith(AndroidJUnit4::class)
class EchHandshakeInstrumentedTest {
    @Test fun encryptsRealApiHostnameOnTheWire() {
        // Public DNS fixture; only the ClientHello is inspected, no server needs this old key.
        val config = Base64.decode("AEX+DQBBdAAgACBjEPOpr4CoOtieuXOxYlVxZesNpiBFOa5e33U90X77XAAEAAEAAQASY2xvdWRmbGFyZS1lY2guY29tAAA=", Base64.DEFAULT)
        val trust = EchTrustManager(requireNotNull(systemTrustManagerOrNull()))
        val delegate = SSLContext.getInstance("TLS", Conscrypt.newProvider()).apply {
            init(null, arrayOf(trust), null)
        }.socketFactory
        val client = OkHttpClient.Builder().sslSocketFactory(EchSocketFactory(delegate) { config }, trust)
            .callTimeout(5, TimeUnit.SECONDS).build()
        try {
            assertSame(OkHttpClient().hostnameVerifier, client.hostnameVerifier)
            assertTrue(trust.acceptedIssuers.isNotEmpty())
            val production = OkHttpClient.Builder().hostnameVerifier { _, _ -> true }
                .applyApiNetwork(DnsMode.DIRECT, SniMode.ECH, verify = false).build()
            assertSame("ECH must override legacy verification bypasses",
                OkHttpClient().hostnameVerifier, production.hostnameVerifier)
            captureEchHello(client)
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }
}

private fun captureEchHello(base: OkHttpClient) {
    java.net.ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
        server.soTimeout = 4000
        val pool = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            val hello = pool.submit<ByteArray> {
                server.accept().use { socket ->
                    socket.soTimeout = 4000
                    val input = java.io.DataInputStream(socket.getInputStream())
                    check(input.readUnsignedByte() == 22)
                    input.readUnsignedShort()
                    ByteArray(input.readUnsignedShort()).also { input.readFully(it) }
                }
            }
            val client = base.newBuilder().dns(DnsLookup { listOf(server.inetAddress) }).retryOnConnectionFailure(false).build()
            try { client.newCall(Request.Builder().url("https://app-api.pixiv.net:${server.localPort}/").build()).execute().close() } catch (_: java.io.IOException) {}
            val input = java.io.DataInputStream(java.io.ByteArrayInputStream(hello.get(5, TimeUnit.SECONDS)))
            check(input.readUnsignedByte() == 1)
            input.skipBytes(3 + 2 + 32)
            input.skipBytes(input.readUnsignedByte())
            input.skipBytes(input.readUnsignedShort())
            input.skipBytes(input.readUnsignedByte())
            val ext = java.io.DataInputStream(java.io.ByteArrayInputStream(ByteArray(input.readUnsignedShort()).also { input.readFully(it) }))
            val types = mutableListOf<Int>(); var sni = "absent"
            while (ext.available() > 0) {
                val type = ext.readUnsignedShort(); types += type
                val data = ByteArray(ext.readUnsignedShort()).also { ext.readFully(it) }
                if (type == 0) {
                    val name = java.io.DataInputStream(java.io.ByteArrayInputStream(data))
                    name.readUnsignedShort(); name.readUnsignedByte()
                    sni = String(ByteArray(name.readUnsignedShort()).also { name.readFully(it) }, Charsets.US_ASCII)
                }
            }
            assertEquals("cloudflare-ech.com", sni)
            assertTrue("ECH extension must be present on Android, not just configured in Java", 0xfe0d in types)
        } finally { pool.shutdownNow() }
    }
}
