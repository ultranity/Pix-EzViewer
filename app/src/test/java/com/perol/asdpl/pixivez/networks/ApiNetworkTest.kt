package com.perol.asdpl.pixivez.networks

import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test

class ApiNetworkTest {
    @Test fun legacyEmptyModeOmitsSniAndKeepsCertificateVerification() {
        assertEquals(SniMode.ECH, SniMode.fromCode(null))
        val client = OkHttpClient.Builder()
            .applyApiNetwork(DnsMode.DIRECT, SniMode.EMPTY, verify = true).build()
        assertFalse(captureHello(client).contains(0))
        assertSame(OkHttpClient().hostnameVerifier, client.hostnameVerifier)
        assertTrue(client.x509TrustManager!!.acceptedIssuers.isNotEmpty())
    }

    @Test fun explicitlySavedModesRemainSelectable() {
        assertEquals(SniMode.REPLACE, SniMode.fromCode("replace"))
        assertEquals(SniMode.PLAIN, SniMode.fromCode("plain"))
        val client = OkHttpClient.Builder()
            .applyApiNetwork(DnsMode.DIRECT, SniMode.PLAIN, verify = true).build()
        assertTrue(captureHello(client).contains(0))
    }
}
