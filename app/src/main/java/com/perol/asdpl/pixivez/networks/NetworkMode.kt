/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
 * Copyright (c) 2019 Perol_Notsfsssf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package com.perol.asdpl.pixivez.networks

import android.util.Log
import androidx.core.content.edit
import com.perol.asdpl.pixivez.services.PxEZApp
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/*
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ Pixiv 鉴权/接口连接 —— DNS × SNI 两维度(有耦合)                           │
 * │                                                                            │
 * │ 这些域名有两类入口,对"无 SNI"处理不同:                                   │
 * │   - Pixiv 自有源站(IDC Frontier/JP,如 210.140.139.x):nginx 直接服务,   │
 * │     接受无 SNI 并按 Host 路由 —— 直连它即绕开 Cloudflare 边缘;            │
 * │   - Cloudflare 共享 anycast(公共 DNS/DoH 返回,104.x/172.64.x):          │
 * │     无 SNI 时选不出证书 → TLS 握手失败。                                    │
 * │ 故两维度耦合(空/替换 SNI 须配源站):                                       │
 * │   • DNS 维度  —— 连哪类入口:DIRECT(Pixiv 源站)/ DoH(anycast)/ 系统   │
 * │   • SNI 维度  —— ClientHello 呈现:替换(pixiv.me)/ 空 / 明文(GFW RST)  │
 * │ 墙内可用 = DIRECT + 替换 SNI=pixiv.me(直连源站、SNI 不被 GFW 封、且其多-SAN │
 * │ 证书覆盖目标 Host 不致 421)。证书校验默认开([VerifyConfig]),异常网络可关。│
 * └──────────────────────────────────────────────────────────────────────────┘
 */

/**
 * DNS 解析来源:决定连到哪个 IP。这与 SNI 维度有物理耦合:
 * 空/替换 SNI 配 Pixiv 专属直连 IP(共享 anycast 无 SNI 时选不出证书 → 握手失败),
 * 故 [SniMode.EMPTY]/[SniMode.REPLACE] 须配 [DIRECT];[SniMode.PLAIN] 配 [DOH](anycast)。
 */
enum class DnsMode(val code: String) {
    /** Pixiv 专属直连 IP([PixivDirectDns])。接受空/替换 SNI、按 Host 服务 API;墙内绕过用。 */
    DIRECT("direct"),

    /** DoH(服务商见 [DohConfig])→ Cloudflare anycast。免污染、跟随轮换;配明文。 */
    DOH("doh"),

    /** 系统 DNS。无墙 / 走代理 / VPN 场景。 */
    SYSTEM("system");

    companion object {
        const val PREF_KEY = "apiDnsMode"
        val default = DIRECT
        val displayOrder = listOf(DIRECT, DOH, SYSTEM)
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: default
        fun current() = fromCode(PxEZApp.instance.pre.getString(PREF_KEY, default.code))
    }
}

/** SNI 模式:负责 TLS ClientHello 里如何呈现域名(对抗 GFW 的 SNI 封锁)。 */
enum class SniMode(val code: String) {
    /**
     * 替换 SNI 为 [SniReplaceConfig] 主机(默认 pixiv.me)。
     * 既避开空 SNI 在某些入口拿到不覆盖目标 Host 的默认证书而被判 421,
     * 又避开明文 *.pixiv.net SNI 被 GFW 过滤 RST。须配 DnsMode.DIRECT。
     */
    REPLACE("replace"),

    /** 空 SNI。须配 DnsMode.DIRECT(anycast 拒绝无 SNI)。 */
    EMPTY("empty"),

    /** 明文真实 SNI + 证书校验。配 DoH/anycast;无墙 / 代理可用;墙内会被 SNI RST。 */
    PLAIN("plain");

    companion object {
        const val PREF_KEY = "apiSniMode"

        // 面向墙内:默认替换 SNI=pixiv.me(须配 DnsMode.DIRECT 直连 Pixiv 源站)。
        val default = REPLACE
        val displayOrder = listOf(REPLACE, EMPTY, PLAIN)
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: default
        fun current() = fromCode(PxEZApp.instance.pre.getString(PREF_KEY, default.code))
    }
}

/**
 * 替换/空 SNI 模式下是否做证书 + 主机名校验(默认开)。
 * REPLACE 下源站返回的多-SAN 证书本就覆盖目标 Host 且链到可信 CA,默认校验可通过;
 * 仅当某网络/入口返回不匹配证书导致连不上时才关闭。关闭 = 跳过主机名校验(有 MITM 风险)。
 */
object VerifyConfig {
    const val PREF_KEY = "apiVerifyCert"
    fun enabled(): Boolean = PxEZApp.instance.pre.getBoolean(PREF_KEY, true)
}

/*
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ SniReplaceConfig —— 替换 SNI 主机 + 自适应自动选择。                        │
 * │                                                                            │
 * │ 同一张源站证书的所有 SAN 都能"授权"目标 Host(选中即不致 421),但 GFW 对   │
 * │ 不同 SNI 字符串封锁不一。故:读源站证书 SAN 列出候选 → 在本机逐个实测      │
 * │ (握手未被 RST、HTTP 非 421、且对端证书确为 Pixiv)→ 选第一个可用的。      │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
object SniReplaceConfig {
    const val PREF_KEY = "apiSniReplaceHost"
    const val DEFAULT = "pixiv.me"

    // 读 SAN 失败时的兜底候选(均在源站证书 SAN 内)。非 *.pixiv.net 域名优先 ——
    // 更可能逃过 GFW 对 pixiv.net 的 SNI 过滤。
    val FALLBACK_CANDIDATES = listOf(
        "pixiv.me", "www.pixivision.net", "fanbox.cc",
        "public-api.secure.pixiv.net", "oauth.secure.pixiv.net", "pixiv.net",
    )

    fun host(): String =
        PxEZApp.instance.pre.getString(PREF_KEY, DEFAULT)?.trim()?.ifBlank { DEFAULT } ?: DEFAULT

    // 探测客户端复用同一 dispatcher + 连接池(newBuilder),仅替换 SSLSocketFactory,
    // 避免每次探测各起线程/连接池而泄漏。
    private val probeBase: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(PixivDirectDns)
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private fun probeClient(factory: SSLSocketFactory): OkHttpClient =
        probeBase.newBuilder()
            .sslSocketFactory(factory, RubyX509TrustManager())
            .hostnameVerifier { _, _ -> true }
            .build()

    private fun certIsPixiv(cert: X509Certificate?): Boolean {
        val sans = cert?.subjectAlternativeNames?.mapNotNull { it.getOrNull(1) as? String } ?: return false
        return sans.any { it == "pixiv.net" || it.endsWith(".pixiv.net") || it == "pixiv.me" }
    }

    /** 读源站证书 SAN 作候选(空 SNI 握手即可拿到多-SAN 证书);失败回退 [FALLBACK_CANDIDATES]。 */
    fun candidates(): List<String> = try {
        probeClient(RubySSLSocketFactory())
            .newCall(Request.Builder().url("https://oauth.secure.pixiv.net/").head().build())
            .execute().use { resp ->
                val cert = resp.handshake?.peerCertificates?.firstOrNull() as? X509Certificate
                val sans = cert?.subjectAlternativeNames
                    ?.mapNotNull { it.getOrNull(1) as? String }
                    ?.filterNot { it.startsWith("*.") }
                    ?.distinct().orEmpty()
                if (sans.isEmpty()) FALLBACK_CANDIDATES
                else sans.sortedBy { it.endsWith("pixiv.net") } // 非-pixiv.net 优先
            }
    } catch (e: Exception) {
        Log.w("SniReplace", "read SAN failed, use fallback", e)
        FALLBACK_CANDIDATES
    }

    /**
     * 实测:连源站 + 该 SNI,握手成功(未被 RST)、HTTP 非 421、且对端证书确为 Pixiv
     * (防 captive portal / 透明代理用非-Pixiv 证书冒充成"可用")才视为当前网络可用。
     */
    fun probe(sniHost: String): Boolean = try {
        probeClient(ReplaceSniSocketFactory(sniHost))
            .newCall(Request.Builder().url("https://app-api.pixiv.net/").head().build())
            .execute().use { resp ->
                resp.code != 421 &&
                    certIsPixiv(resp.handshake?.peerCertificates?.firstOrNull() as? X509Certificate)
            }
    } catch (e: Exception) {
        false
    }

    /** 逐个实测候选,第一个可用者写入 pref 并返回;都不可用返回 null。须在 IO 线程调用。 */
    fun autoSelect(): String? {
        for (h in candidates()) {
            if (probe(h)) {
                PxEZApp.instance.pre.edit { putString(PREF_KEY, h) }
                Log.i("SniReplace", "auto-selected SNI=$h")
                return h
            }
        }
        return null
    }
}

/**
 * DoH 服务商。接受裸主机(1.1.1.1)或完整 URL(https://host[/dns-query])。
 * 默认 Cloudflare 的 cloudflare-dns.com 端点 + bootstrap IP —— bootstrap 让 DoH
 * 客户端无需经(可能被污染的)系统 DNS 去解析 DoH 主机本身。
 */
object DohConfig {
    const val PREF_KEY = "apiDohProvider"
    const val DEFAULT = "https://1dot1dot1dot1.cloudflare-dns.com"

    // 默认 DoH 主机(1dot1dot1dot1.cloudflare-dns.com)的入口 IP。
    val DEFAULT_BOOTSTRAP = listOf("104.16.248.249", "104.16.249.249")

    fun provider(): String =
        PxEZApp.instance.pre.getString(PREF_KEY, DEFAULT)?.ifBlank { DEFAULT } ?: DEFAULT

    fun url(): String {
        val p = provider().trim().removeSuffix("/")
        return when {
            p.contains("/dns-query") -> p
            p.startsWith("http") -> "$p/dns-query"
            else -> "https://$p/dns-query" // 裸主机
        }
    }

    /** 仅默认服务商提供 bootstrap IP;自定义服务商需经系统 DNS 解析其主机。 */
    fun bootstrapIps(): List<InetAddress> =
        if (provider().trim().removeSuffix("/") == DEFAULT)
            DEFAULT_BOOTSTRAP.map { InetAddress.getByName(it) }
        else emptyList()
}

/** Pixiv 鉴权/接口域名。 */
object PixivApiHosts {
    val HOSTS = setOf(
        "app-api.pixiv.net",
        "oauth.secure.pixiv.net",
        "accounts.pixiv.net",
    )
}

/*
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ PixivDirectDns —— Pixiv 专属直连 IP(不在公共 DNS,须硬编码)。             │
 * │ 这些 IP 接受无 SNI 握手并按 Host 服务 API,是空/替换-SNI 绕过的落点。      │
 * │ 会随 Pixiv 轮换;失效时需更新此表(可经设置 apiDirectIPs 以逗号分隔覆盖)。 │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
object PixivDirectDns : Dns {
    const val PREF_KEY = "apiDirectIPs"
    val DEFAULT_IPS = listOf(
        "210.140.139.155", "210.140.139.156", "210.140.139.157", "210.140.139.158",
    )

    private val IPV4 =
        Regex("^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$")

    // 仅采纳格式合法的 IPv4 字面量;非法项(typo / 主机名)会被丢弃以免触发系统 DNS
    // (墙内可能被污染/挂起),全部非法则回退默认。
    private fun configured(): List<String> {
        val raw = PxEZApp.instance.pre.getString(PREF_KEY, null)?.trim().orEmpty()
        if (raw.isEmpty()) return DEFAULT_IPS
        val ips = raw.split(",").map { it.trim() }.filter { it.matches(IPV4) }
        return ips.ifEmpty { DEFAULT_IPS }
    }

    override fun lookup(hostname: String): List<InetAddress> =
        if (hostname in PixivApiHosts.HOSTS)
            configured().map { InetAddress.getByName(it) }.shuffled()
        else Dns.SYSTEM.lookup(hostname)
}

/*
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ DohApiDns —— 经 DoH 服务商解析 API 域名 → 当前真实入口 IP(免污染、跟轮换)。│
 * │ 失败回退硬编码兜底。其余域名交还系统 DNS。                                  │
 * │ (服务商/模式变更经设置后真重启进程生效,见 snackbarForceRestart。)         │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
object DohApiDns : Dns {
    private const val TAG = "DohApiDns"
    private const val TTL_MS = 10 * 60 * 1000L

    // 冷启动 / DoH 失败时的兜底入口(Cloudflare anycast)。正常路径由 DoH 动态解析。
    private val fallback: List<InetAddress> =
        listOf("172.64.145.17", "104.18.42.239").map { InetAddress.getByName(it) }

    private val cache = ConcurrentHashMap<String, Pair<Long, List<InetAddress>>>()

    private val doh: DnsOverHttps by lazy {
        val b = DnsOverHttps.Builder()
            .client(OkHttpClient())
            .url(DohConfig.url().toHttpUrl())
            .post(true)
            .resolvePrivateAddresses(false)
            .resolvePublicAddresses(true)
        DohConfig.bootstrapIps().takeIf { it.isNotEmpty() }?.let { b.bootstrapDnsHosts(it) }
        b.build()
    }

    override fun lookup(hostname: String): List<InetAddress> {
        if (hostname !in PixivApiHosts.HOSTS) return Dns.SYSTEM.lookup(hostname)
        val now = System.currentTimeMillis()
        cache[hostname]?.let { (at, ips) -> if (now - at < TTL_MS && ips.isNotEmpty()) return ips }
        return try {
            val fresh = doh.lookup(hostname)
            if (fresh.isNotEmpty()) {
                cache[hostname] = now to fresh
                Log.d(TAG, "DoH $hostname -> ${fresh.joinToString { it.hostAddress ?: "" }}")
                fresh
            } else {
                fallback
            }
        } catch (e: Exception) {
            Log.w(TAG, "DoH lookup failed for $hostname, use fallback", e)
            fallback
        }
    }

    /** 供 WebView bypass:对任意域名经 DoH 解析(失败回退空表)。 */
    fun lookupPublic(host: String): List<InetAddress> =
        try { doh.lookup(host) } catch (e: Exception) { emptyList() }
}

/*
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ 按 (DNS 维度 × SNI 维度) 装配鉴权/接口 OkHttpClient。                       │
 * └──────────────────────────────────────────────────────────────────────────┘
 */

/** 供 bypass 复用系统信任链(校验开启时用);异常返回 null 退化为信任全部。 */
fun systemTrustManagerOrNull(): javax.net.ssl.X509TrustManager? =
    try { systemTrustManager } catch (e: Exception) { null }

/** 系统默认信任链(校验开启时用,验证证书链到可信 CA)。 */
private val systemTrustManager: X509TrustManager by lazy {
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    tmf.init(null as KeyStore?)
    tmf.trustManagers.first { it is X509TrustManager } as X509TrustManager
}

/**
 * 装配自定义 SNI 的 SSLSocketFactory。
 * verify=true:保留默认主机名校验(按 URL host 比对证书 SAN)+ 系统信任链;
 * verify=false:信任全部 + 跳过主机名校验(异常网络兜底,有 MITM 风险)。
 */
private fun OkHttpClient.Builder.applySni(factory: SSLSocketFactory, verify: Boolean) = apply {
    if (verify) {
        sslSocketFactory(factory, systemTrustManager)
    } else {
        sslSocketFactory(factory, RubyX509TrustManager())
        hostnameVerifier { _, _ -> true }
    }
}

fun OkHttpClient.Builder.applyApiNetwork(
    dnsMode: DnsMode = DnsMode.current(),
    sniMode: SniMode = SniMode.current(),
    verify: Boolean = VerifyConfig.enabled(),
): OkHttpClient.Builder = apply {
    // ── DNS 维度:拿到正确 IP ──
    when (dnsMode) {
        DnsMode.DIRECT -> dns(PixivDirectDns)
        DnsMode.DOH -> dns(DohApiDns)
        DnsMode.SYSTEM -> Unit
    }
    // ── SNI 维度:ClientHello 呈现 ──
    when (sniMode) {
        SniMode.REPLACE -> applySni(ReplaceSniSocketFactory(SniReplaceConfig.host()), verify)
        SniMode.EMPTY -> applySni(RubySSLSocketFactory(), verify)
        SniMode.PLAIN -> Unit // 默认 TLS:真实 SNI + 证书校验
    }
}
