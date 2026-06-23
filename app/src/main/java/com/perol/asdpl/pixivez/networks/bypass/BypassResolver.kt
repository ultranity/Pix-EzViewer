/*
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ BypassResolver —— 为 (host, rule) 选可用 endpoint:                     │
 * │   候选 IP = rule.ip(若有) ∪ DoH(host);候选 SNI = rule.sni;            │
 * │   逐个运行时探测(握手未被 RST、HTTP≠421)首个可用者,按 host 缓存 TTL。 │
 * │ 复用 API 层 DoH 客户端与 SNI 原语。                                     │
 * └──────────────────────────────────────────────────────────────────────┘
 */
package com.perol.asdpl.pixivez.networks.bypass

import com.perol.asdpl.pixivez.networks.DohApiDns
import com.perol.asdpl.pixivez.networks.ReplaceSniSocketFactory
import com.perol.asdpl.pixivez.networks.RubySSLSocketFactory
import com.perol.asdpl.pixivez.networks.RubyX509TrustManager
import com.perol.asdpl.pixivez.networks.SniMode
import com.perol.asdpl.pixivez.networks.SniReplaceConfig
import com.perol.asdpl.pixivez.networks.VerifyConfig
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class Endpoint(
    val ip: InetAddress,
    val sni: SniMode,
    val frontSni: String?,
    val verify: Boolean,
)

fun interface Prober {
    fun ok(ip: InetAddress, host: String, sni: SniMode, frontSni: String?): Boolean
}

object BypassResolver {
    private const val TTL_MS = 10 * 60 * 1000L
    private val cache = ConcurrentHashMap<String, Pair<Long, Endpoint>>()
    private const val NEG_TTL_MS = 60 * 1000L   // 探测全败的负缓存,避免子请求风暴重复探测
    private val negativeCache = ConcurrentHashMap<String, Long>()

    private val probeBase: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    /** 默认探测器:连指定 IP + 指定 SNI,HEAD 该 host,握手成功且 HTTP≠421 即可用。 */
    private val defaultProber = Prober { ip, host, sni, frontSni ->
        try {
            val factory = when (sni) {
                SniMode.REPLACE -> ReplaceSniSocketFactory(frontSni ?: SniReplaceConfig.host())
                SniMode.EMPTY -> RubySSLSocketFactory()
                SniMode.PLAIN -> null
            }
            val b = probeBase.newBuilder()
                .dns(object : Dns { override fun lookup(hostname: String) = listOf(ip) })
            if (factory != null) b.sslSocketFactory(factory, RubyX509TrustManager())
                .hostnameVerifier { _, _ -> true }
            b.build().newCall(Request.Builder().url("https://$host/").head().build())
                .execute().use { it.code != 421 }
        } catch (e: Exception) { false }
    }

    fun resolve(host: String, rule: BypassRule): Endpoint? {
        val now = System.currentTimeMillis()
        cache[host]?.let { (at, ep) -> if (now - at < TTL_MS) return ep }
        negativeCache[host]?.let { at -> if (now - at < NEG_TTL_MS) return null }
        val ips = buildList {
            rule.ip?.let { runCatching { add(InetAddress.getByName(it)) } }
            runCatching { addAll(DohApiDns.lookupPublic(host)) }
        }.distinct()
        val verify = VerifyConfig.enabled()
        val candidates = ips.map { Endpoint(it, rule.sni, rule.frontSni, verify) }
        val ep = pick(candidates, host, defaultProber)
        if (ep != null) cache[host] = now to ep else negativeCache[host] = now
        return ep
    }

    internal fun pick(candidates: List<Endpoint>, host: String, prober: Prober): Endpoint? =
        candidates.firstOrNull { prober.ok(it.ip, host, it.sni, it.frontSni) }
}
