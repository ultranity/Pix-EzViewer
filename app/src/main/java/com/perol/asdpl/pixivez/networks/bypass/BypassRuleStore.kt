/*
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ BypassRuleStore —— 加载打包的 Cealing-Host 规则 + 内置补充,按 host    │
 * │ 最长匹配返回规则。init 须在 Application 启动调用一次。                   │
 * └──────────────────────────────────────────────────────────────────────┘
 */
package com.perol.asdpl.pixivez.networks.bypass

import android.content.Context
import com.perol.asdpl.pixivez.networks.SniMode

object BypassRuleStore {
    // 登录/验证码所需、Cealing 表可能未覆盖的内置补充(REPLACE 用 pixiv.me/g.cn)。
    private val builtin: List<BypassRule> = listOf(
        BypassRule(listOf(HostPattern.Exact("accounts.pixiv.net")), SniMode.REPLACE, "pixiv.me", null),
        BypassRule(listOf(HostPattern.Suffix("recaptcha.net")), SniMode.REPLACE, "g.cn", null),
        BypassRule(listOf(HostPattern.Suffix("gstatic.com")), SniMode.REPLACE, "g.cn", null),
    )

    @Volatile private var rules: List<BypassRule> = builtin

    fun init(context: Context) {
        rules = try {
            val text = context.assets.open("bypass/cealing-host.json")
                .bufferedReader().use { it.readText() }
            builtin + CealingHostParser.parse(text)   // 内置优先(平手靠前胜出)
        } catch (e: Exception) {
            builtin
        }
    }

    fun match(host: String): BypassRule? = matchIn(rules, host)

    internal fun matchIn(rules: List<BypassRule>, host: String): BypassRule? {
        var best: BypassRule? = null
        var bestScore = -1
        for (r in rules) {
            val s = r.match(host)
            if (s > bestScore) { bestScore = s; best = r }   // > 保证平手取靠前
        }
        return if (bestScore >= 0) best else null
    }
}
