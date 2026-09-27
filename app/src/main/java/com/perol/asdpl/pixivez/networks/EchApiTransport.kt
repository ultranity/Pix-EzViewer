package com.perol.asdpl.pixivez.networks

import androidx.annotation.Keep
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import org.conscrypt.Conscrypt
import org.conscrypt.ConscryptNetworkSecurityPolicy
import org.conscrypt.DomainEncryptionMode
import org.conscrypt.NetworkSecurityPolicy
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/** Keeps OkHttp/Retrofit, authentication, cancellation and HTTP/2; only replaces TLS. */
internal object EchApiTransport {
    // Same Cloudflare ECH bootstrap used by pixez-flutter. Query HTTPS records, not
    // the API's A record: the latter does not supply the ECH key and may be polluted.
    private val primary = DohTransport.client(withoutSni = false)
    private val secondary = DohTransport.client(withoutSni = true).newBuilder()
        .dns(DnsLookup { host ->
            if (host == "cloudflare-dns.com") DohTransport.BOOTSTRAP_IPS.map(InetAddress::getByName)
            else Dns.SYSTEM.lookup(host)
        }).build()

    internal val configs = EchConfigCache(loader = {
        var failure: IOException? = null
        for ((client, url) in listOf(
            primary to "https://dns.alidns.com/resolve?name=cloudflare-ech.com&type=HTTPS",
            secondary to "https://cloudflare-dns.com/dns-query?name=cloudflare-ech.com&type=HTTPS",
        )) {
            try {
                return@EchConfigCache client.newCall(Request.Builder().url(url)
                    .header("Accept", "application/dns-json").build()).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("ECH resolver HTTP ${response.code}")
                    val body = response.body ?: throw IOException("Empty ECH DNS response")
                    // DNS responses are small. Bound both parsing and allocation.
                    val source = body.source()
                    source.request(65_537)
                    if (source.buffer.size > 65_536) throw IOException("Oversized ECH DNS response")
                    EchConfigParser.parse(source.readUtf8())
                }
            } catch (e: IOException) {
                failure?.let(e::addSuppressed)
                failure = e
            }
        }
        throw failure ?: IOException("No ECH resolver available")
    })

    private val fallbackIps = listOf("104.18.10.118", "104.18.11.118")
    private val trust by lazy { EchTrustManager(requireNotNull(systemTrustManagerOrNull())) }
    private val factory by lazy {
        val delegate = SSLContext.getInstance("TLS", Conscrypt.newProvider()).apply {
            init(null, arrayOf(trust), null)
        }.socketFactory
        EchSocketFactory(delegate) { configs.get().bytes }
    }

    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder = builder.apply {
        dns(DnsLookup { host ->
            if (host in PixivApiHosts.HOSTS) {
                (configs.get().ipv4Hints + fallbackIps).distinct().map(InetAddress::getByName)
            } else Dns.SYSTEM.lookup(host)
        })
        sslSocketFactory(factory, trust)
        hostnameVerifier(primary.hostnameVerifier)
        // ECH is TLS 1.3 only. Leave ALPN enabled so OkHttp negotiates HTTP/2.
        connectionSpecs(listOf(ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3).supportsTlsExtensions(true).build()))
        addInterceptor(EchRefreshInterceptor(configs))
    }
}

/**
 * Must be a plain X509TrustManager. Conscrypt 2.7.0 wraps an extended trust manager
 * in ConscryptEngineSocket and loses getNetworkSecurityPolicy(), silently disabling
 * ECH. Keep this public reflected method when shrinking. All trust checks still
 * delegate to Android's trust manager, and OkHttp verifies the actual URL hostname.
 */
@Keep
class EchTrustManager(private val delegate: X509TrustManager) : X509TrustManager {
    fun getNetworkSecurityPolicy(): NetworkSecurityPolicy = object : ConscryptNetworkSecurityPolicy() {
        override fun getDomainEncryptionMode(hostname: String?): DomainEncryptionMode =
            if (hostname in PixivApiHosts.HOSTS) DomainEncryptionMode.REQUIRED else DomainEncryptionMode.DISABLED
    }
    override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
        delegate.checkClientTrusted(chain, authType)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) =
        delegate.checkServerTrusted(chain, authType)
}

internal class EchSocketFactory(
    private val delegate: SSLSocketFactory,
    private val config: () -> ByteArray,
) : SSLSocketFactory() {
    private fun configured(socket: Socket, host: String): Socket {
        try {
            if (host in PixivApiHosts.HOSTS) {
                (socket as SSLSocket).enabledProtocols = arrayOf("TLSv1.3")
                Conscrypt.setEchConfigList(socket, config())
            }
            return socket
        } catch (e: Exception) {
            socket.close()
            throw e
        }
    }
    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites
    override fun createSocket(s: Socket, h: String, p: Int, autoClose: Boolean): Socket =
        configured(delegate.createSocket(s, h, p, autoClose), h)
    override fun createSocket(h: String, p: Int): Socket = configured(delegate.createSocket(h, p), h)
    override fun createSocket(h: String, p: Int, l: InetAddress, lp: Int): Socket =
        configured(delegate.createSocket(h, p, l, lp), h)
    override fun createSocket(): Socket = throw SocketException("ECH requires a logical hostname")
    override fun createSocket(h: InetAddress, p: Int): Socket = throw SocketException("ECH requires a logical hostname")
    override fun createSocket(h: InetAddress, p: Int, l: InetAddress, lp: Int): Socket =
        throw SocketException("ECH requires a logical hostname")
}

/** Refresh rotated keys on failure. This interceptor never replays POST or downgrades SNI. */
internal class EchRefreshInterceptor(private val cache: EchConfigCache) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        if (request.url.host !in PixivApiHosts.HOSTS) return chain.proceed(request)
        val replayable = request.method in setOf("GET", "HEAD") && request.body == null
        try {
            val response = chain.proceed(request)
            // A second proceed() after 421 can reuse the same live TLS connection.
            // Invalidate for a future handshake instead of pretending to reroute it.
            if (response.code == 421) cache.invalidate()
            return response
        } catch (e: IOException) {
            if (!cache.invalidate() || !replayable || chain.call().isCanceled()) throw e
        }
        return chain.proceed(request)
    }
}
