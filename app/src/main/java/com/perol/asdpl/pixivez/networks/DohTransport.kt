package com.perol.asdpl.pixivez.networks

import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** Shared by API, images and WebView. Never inherits API/image trust-all or DNS overrides. */
internal object DohTransport {
    const val DEFAULT_URL = "https://1dot1dot1dot1.cloudflare-dns.com/dns-query"
    val BOOTSTRAP_IPS = listOf("104.16.248.249", "104.16.249.249")

    fun usesDefaultHost(url: String): Boolean =
        url.toHttpUrlOrNull()?.host == DEFAULT_URL.toHttpUrl().host

    fun client(withoutSni: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
        if (withoutSni) {
            val trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(null as KeyStore?)
            }.trustManagers.filterIsInstance<X509TrustManager>().first()
            builder.sslSocketFactory(RubySSLSocketFactory(), trust)
                // Older Android adapters restore the real SNI after createSocket().
                // Disabling TLS extensions also disables ALPN; DoH works over HTTP/1.1.
                .connectionSpecs(listOf(ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .supportsTlsExtensions(false).build()))
        }
        // Keep the default hostname verifier and platform trust chain in BOTH modes.
        return builder.build()
    }

    fun create(url: String, bootstrap: List<InetAddress>, withoutSni: Boolean): Dns {
        val endpoint = url.toHttpUrl()
        require(endpoint.isHttps) { "DoH requires HTTPS" }
        fun resolver(suppressSni: Boolean): Dns = DnsOverHttps.Builder()
            .client(client(suppressSni))
            .url(endpoint)
            .bootstrapDnsHosts(bootstrap.takeIf { it.isNotEmpty() })
            .includeIPv6(false)
            .post(true)
            .resolvePrivateAddresses(false)
            .resolvePublicAddresses(true)
            .build()
        val primary = resolver(withoutSni)
        if (!withoutSni) return primary
        val regular = resolver(false)
        // Prefer the upstream's no-SNI fix. Some networks need normal TLS: try it once,
        // still with bootstrap addresses and strict certificate validation.
        // OkHttp DnsOverHttps wraps handshake/timeout failures in UnknownHostException.
        return DnsLookup { hostname ->
            try { primary.lookup(hostname) } catch (first: UnknownHostException) {
                try { regular.lookup(hostname) } catch (second: UnknownHostException) {
                    second.addSuppressed(first)
                    throw second
                }
            }
        }
    }
}
