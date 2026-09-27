package com.perol.asdpl.pixivez.networks

import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import javax.net.ssl.SSLSocket

class RubySSLSocketFactoryTest {
    @Test fun layersTlsOnTheConnectedSocketInsteadOfOpeningAnotherConnection() {
        ServerSocket(0, 2, InetAddress.getLoopbackAddress()).use { server ->
            Socket(server.inetAddress, server.localPort).use { raw ->
                server.accept().use {
                    val factory = RubySSLSocketFactory()
                    (factory.createSocket(raw, "example.org", server.localPort, true) as SSLSocket).use { tls ->
                        assertEquals("TLS must retain OkHttp's connected socket", raw.localPort, tls.localPort)
                        assertTrue(tls.sslParameters.serverNames.isNullOrEmpty())
                        assertTrue(factory.supportedCipherSuites.isNotEmpty())
                        server.soTimeout = 100
                        try {
                            server.accept().close()
                            fail("Unexpected second TCP connection")
                        } catch (_: SocketTimeoutException) { }
                    }
                    assertTrue("autoClose must close the original socket", raw.isClosed)
                }
            }
        }
    }
}
