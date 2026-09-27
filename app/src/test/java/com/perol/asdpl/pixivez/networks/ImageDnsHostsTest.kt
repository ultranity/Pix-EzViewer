package com.perol.asdpl.pixivez.networks

import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress

class ImageDnsHostsTest {
    @Test fun forceIpFailureInvalidatesOriginalHostCache() {
        var queries = 0
        val dns = RefreshingDns(DnsLookup {
            queries++
            listOf(InetAddress.getByName("192.0.2.$queries"))
        }, { emptyList() }, ImageDnsHosts::contains)
        dns.lookup("i.pximg.net")
        val request = Request.Builder().url("https://192.0.2.1/image.jpg")
            .header("Host", "i.pximg.net").build()
        dns.invalidate(ImageDnsHosts.origin(request))
        assertEquals("192.0.2.2", dns.lookup("i.pximg.net").first().hostAddress)
        assertEquals(2, queries)
    }

    @Test fun mirrorHostsAreNeverMistakenForImageOrigins() {
        assertFalse(ImageDnsHosts.contains("evilpximg.net"))
        assertFalse(ImageDnsHosts.contains("pximg.net.example.org"))
        assertTrue(ImageDnsHosts.contains("I.PXIMG.NET."))
        val request = Request.Builder().url("https://mirror.example.org/image.jpg")
            .header("Host", "mirror.example.org").build()
        assertEquals("mirror.example.org", ImageDnsHosts.origin(request))
    }
}
