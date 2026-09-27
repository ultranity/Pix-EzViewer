package com.perol.asdpl.pixivez.networks

import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okhttp3.Dns
import okhttp3.Request

class DohTransportTest {
    @Test fun defaultTransportSuppressesPlatformSniWithoutDisablingCertificateChecks() {
        val regular = DohTransport.client(false)
        val noSni = DohTransport.client(true)
        assertTrue(noSni.sslSocketFactory is RubySSLSocketFactory)
        assertTrue(noSni.connectionSpecs.none { it.supportsTlsExtensions })
        assertSame(regular.hostnameVerifier, noSni.hostnameVerifier)
        assertEquals(regular.x509TrustManager!!::class, noSni.x509TrustManager!!::class)
        assertTrue(noSni.x509TrustManager!!.acceptedIssuers.isNotEmpty())
        assertEquals(4000, noSni.callTimeoutMillis)
    }

    @Test fun fullDefaultUrlStillUsesBootstrapAndTheNoSniFix() {
        assertTrue(DohTransport.usesDefaultHost(DohTransport.DEFAULT_URL))
        assertTrue(DohTransport.usesDefaultHost("https://1dot1dot1dot1.cloudflare-dns.com/"))
        assertFalse(DohTransport.usesDefaultHost("https://resolver.example/dns-query"))
    }

    @Test fun customProviderKeepsNormalTls() {
        assertTrue(DohTransport.client(false).connectionSpecs.any { it.supportsTlsExtensions })
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesCleartextResolver() {
        DohTransport.create("http://resolver.example/dns-query", emptyList(), false)
    }

    @Test fun tlsClientHelloActuallyOmitsSniWhileRegularClientSendsIt() {
        assertTrue(captureHello(DohTransport.client(false)).contains(0)) // server_name extension
        assertFalse(captureHello(DohTransport.client(true)).contains(0))
    }

}

internal fun captureHello(baseClient: okhttp3.OkHttpClient): Set<Int> {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            server.soTimeout = 5000
            val pool = Executors.newSingleThreadExecutor()
            try {
                val hello = pool.submit<ByteArray> {
                    server.accept().use { socket ->
                        socket.soTimeout = 5000
                        val input = DataInputStream(socket.getInputStream())
                        assertEquals(22, input.readUnsignedByte()) // TLS handshake record
                        input.readUnsignedShort() // legacy record version
                        ByteArray(input.readUnsignedShort()).also { input.readFully(it) }
                    }
                }
                val client = baseClient.newBuilder()
                    .dns(DnsLookup { listOf(server.inetAddress) })
                    .retryOnConnectionFailure(false).build()
                try {
                    client.newCall(Request.Builder().url("https://resolver.example:${server.localPort}/").build())
                        .execute().close()
                    fail("The capture server supplies no certificate")
                } catch (_: IOException) { }
                val input = DataInputStream(ByteArrayInputStream(hello.get(5, TimeUnit.SECONDS)))
                assertEquals(1, input.readUnsignedByte()) // ClientHello
                input.skipBytes(3 + 2 + 32) // length, version, random
                input.skipBytes(input.readUnsignedByte()) // session ID
                input.skipBytes(input.readUnsignedShort()) // cipher suites
                input.skipBytes(input.readUnsignedByte()) // compression methods
                val extensions = DataInputStream(ByteArrayInputStream(
                    ByteArray(input.readUnsignedShort()).also { input.readFully(it) }))
                val types = mutableSetOf<Int>()
                while (extensions.available() > 0) {
                    types += extensions.readUnsignedShort()
                    extensions.skipBytes(extensions.readUnsignedShort())
                }
                return types
            } finally { pool.shutdownNow() }
        }
    }
