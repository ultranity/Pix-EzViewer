/*
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ WebViewBypassInterceptor —— shouldInterceptRequest 的统一分发:        │
 * │   追踪器→空;*.pximg.net→imageHttpClient;命中规则的 GET→按 endpoint   │
 * │   重发;其余/非 GET/失败→null(交还原生栈)。仅处理 GET。               │
 * └──────────────────────────────────────────────────────────────────────┘
 */
package com.perol.asdpl.pixivez.networks.bypass

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.perol.asdpl.pixivez.networks.ReplaceSniSocketFactory
import com.perol.asdpl.pixivez.networks.RestClient
import com.perol.asdpl.pixivez.networks.RubySSLSocketFactory
import com.perol.asdpl.pixivez.networks.RubyX509TrustManager
import com.perol.asdpl.pixivez.networks.SniMode
import com.perol.asdpl.pixivez.networks.systemTrustManagerOrNull
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class WebViewBypassInterceptor(private val ua: String) {
    private val blocked = setOf(
        "d.pixiv.org", "connect.facebook.net", "platform.twitter.com", "www.google-analytics.com"
    )
    private val clientCache = ConcurrentHashMap<String, OkHttpClient>()

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        val host = request.url.host ?: return null
        if (host in blocked) {
            return WebResourceResponse(
                "application/javascript", "UTF-8", ByteArrayInputStream(ByteArray(0))
            )
        }
        if (!request.method.equals("GET", true)) return null      // 非 GET:放行(无 body)
        if (host.endsWith("pximg.net")) return reissue(request, RestClient.imageHttpClient)

        val rule = BypassRuleStore.match(host) ?: return null
        val ep = BypassResolver.resolve(host, rule) ?: return null
        return reissue(request, clientFor(host, ep))
    }

    private fun clientFor(host: String, ep: Endpoint): OkHttpClient =
        clientCache.getOrPut(host + "|" + ep.ip.hostAddress + "|" + ep.sni) {
            val ip = ep.ip
            val b = OkHttpClient.Builder()
                .dns(Dns { listOf(ip) })
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
            val factory = when (ep.sni) {
                SniMode.REPLACE -> ReplaceSniSocketFactory(ep.frontSni!!)
                SniMode.EMPTY -> RubySSLSocketFactory()
                SniMode.PLAIN -> null
            }
            if (factory != null) {
                val tm = systemTrustManagerOrNull()
                if (ep.verify && tm != null) b.sslSocketFactory(factory, tm)
                else b.sslSocketFactory(factory, RubyX509TrustManager())
                    .hostnameVerifier { _, _ -> true }
            }
            b.build()
        }

    private fun reissue(request: WebResourceRequest, client: OkHttpClient): WebResourceResponse? =
        try {
            val rb = Request.Builder().url(request.url.toString()).get()
                .header("User-Agent", ua)
            request.requestHeaders.forEach { (k, v) -> if (!k.equals("User-Agent", true)) rb.header(k, v) }
            val resp = client.newCall(rb.build()).execute()
            val type = resp.headers["content-type"]?.substringBefore(";")?.trim() ?: "text/html"
            WebResourceResponse(type, "UTF-8", resp.body?.byteStream()).also {
                it.responseHeaders = resp.headers.toMap().toMutableMap().apply {
                    remove("access-control-allow-origin"); put("Access-Control-Allow-Origin", "*")
                }
            }
        } catch (e: Exception) { null }   // 失败放行原生栈
}
