/*
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ WebViewBypassInterceptor —— shouldInterceptRequest 的统一分发:        │
 * │   追踪器→空;*.pximg.net→imageHttpClient;命中规则的 GET→按 endpoint   │
 * │   重发;其余/非 GET/失败→null(交还原生栈)。仅处理 GET。               │
 * └──────────────────────────────────────────────────────────────────────┘
 */
package com.perol.asdpl.pixivez.networks.bypass

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.perol.asdpl.pixivez.networks.ReplaceSniSocketFactory
import com.perol.asdpl.pixivez.networks.RestClient
import com.perol.asdpl.pixivez.networks.RubySSLSocketFactory
import com.perol.asdpl.pixivez.networks.RubyX509TrustManager
import com.perol.asdpl.pixivez.networks.SniMode
import com.perol.asdpl.pixivez.networks.SniReplaceConfig
import com.perol.asdpl.pixivez.networks.systemTrustManagerOrNull
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
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
        clientCache.getOrPut(host + "|" + ep.ip.hostAddress + "|" + ep.sni + "|" + ep.verify) {
            val ip = ep.ip
            val b = OkHttpClient.Builder()
                .dns(object : Dns { override fun lookup(hostname: String) = listOf(ip) })
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
            val factory = when (ep.sni) {
                SniMode.REPLACE -> ReplaceSniSocketFactory(ep.frontSni ?: SniReplaceConfig.host())
                SniMode.EMPTY -> RubySSLSocketFactory()
                SniMode.PLAIN -> null
            }
            if (factory != null) {
                val tm = systemTrustManagerOrNull()
                // 注:verify=true 配 REPLACE 时,默认主机名校验按 URL host 比对证书 SAN,
                // 故替换 SNI 的前置域证书须覆盖目标 host,否则校验失败。
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
            val skipHeaders = setOf("user-agent", "host", "accept-encoding")
            request.requestHeaders.forEach { (k, v) -> if (k.lowercase() !in skipHeaders) rb.header(k, v) }
            val resp = client.newCall(rb.build()).execute()
            // Set-Cookie 多值头无法在 Map<String,String> 里保留,提前注入 CookieManager
            val cookieManager = CookieManager.getInstance()
            resp.headers("Set-Cookie").forEach { cookieManager.setCookie(request.url.toString(), it) }

            val type = resp.headers["content-type"]?.substringBefore(";")?.trim() ?: "text/html"
            WebResourceResponse(type, "UTF-8", resp.body?.byteStream()).also {
                it.responseHeaders = resp.headers.toMap().toMutableMap().apply {
                    // 去掉已由 CookieManager 处理的 set-cookie,避免多值折叠丢失
                    keys.removeIf { k -> k.lowercase() == "set-cookie" }
                    remove("access-control-allow-origin"); put("Access-Control-Allow-Origin", "*")
                }
            }
        } catch (e: Exception) { null }   // 失败放行原生栈
}
