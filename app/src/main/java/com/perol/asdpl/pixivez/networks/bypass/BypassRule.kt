/*
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ BypassRule —— 单条「域名集合 → SNI 策略 + 落点 IP」规则。              │
 * │ 复用 API 层 SniMode;ip=null 表示交由运行时 DoH 解析。无 android 依赖。 │
 * └──────────────────────────────────────────────────────────────────────┘
 */
package com.perol.asdpl.pixivez.networks.bypass

import com.perol.asdpl.pixivez.networks.SniMode

sealed class HostPattern {
    /** 命中返回特异度(越大越具体),否则 -1。 */
    abstract fun match(host: String): Int

    data class Exact(val host: String) : HostPattern() {
        override fun match(host: String) = if (host.equals(this.host, true)) this.host.length + 1 else -1
    }

    /** Cealing `*base`:任意前缀 + base 结尾(含 base 本身)。 */
    data class Suffix(val base: String) : HostPattern() {
        override fun match(host: String) =
            if (host.equals(base, true) || host.endsWith(base, true)) base.length else -1
    }
}

data class BypassRule(
    val patterns: List<HostPattern>,
    val sni: SniMode,
    val frontSni: String?,
    val ip: String?,
) {
    fun match(host: String): Int = patterns.maxOfOrNull { it.match(host) } ?: -1
}
