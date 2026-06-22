/*
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ CealingHostParser —— 解析 SpaceTimee/Cealing-Host 格式(BSL-1.0)。     │
 * │ 条目 = [ [域名模式...], fakeSni, ip ]                                   │
 * │   fakeSni "" → EMPTY,非空 → REPLACE(frontSni);ip "" → null(走 DoH)。 │
 * │ 域名前缀:'*' 通配后缀;'#' 标签(忽略);'$' 次级(剥前缀按普通);     │
 * │           '^' 排除分隔(取首段正例,其余忽略——保守实现)。            │
 * │ 同一 parser 复用于打包种子与未来运行时导入。无 android 依赖。           │
 * └──────────────────────────────────────────────────────────────────────┘
 */
package com.perol.asdpl.pixivez.networks.bypass

import com.perol.asdpl.pixivez.networks.SniMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

object CealingHostParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(text: String): List<BypassRule> {
        val root = json.parseToJsonElement(text).jsonArray
        return root.mapNotNull { entry ->
            val arr = entry as? JsonArray ?: return@mapNotNull null
            if (arr.size < 3) return@mapNotNull null
            val rawDomains = (arr[0] as JsonArray).map { it.jsonPrimitive.content }
            val fakeSni = arr[1].jsonPrimitive.content.trim()
            val ip = arr[2].jsonPrimitive.content.trim()
            val patterns = rawDomains.mapNotNull(::toPattern)
            if (patterns.isEmpty()) return@mapNotNull null
            BypassRule(
                patterns = patterns,
                sni = if (fakeSni.isEmpty()) SniMode.EMPTY else SniMode.REPLACE,
                frontSni = fakeSni.ifEmpty { null },
                ip = ip.ifEmpty { null },
            )
        }
    }

    private fun toPattern(token: String): HostPattern? {
        if (token.startsWith("#")) return null            // 标签,非匹配项
        var t = token.removePrefix("$")                   // 次级标记按普通处理
        t = t.substringBefore("^").trim()                 // 排除分隔:保守取正例
        if (t.isEmpty()) return null
        return if (t.startsWith("*")) HostPattern.Suffix(t.removePrefix("*"))
        else HostPattern.Exact(t)
    }
}
