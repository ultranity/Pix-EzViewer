# networks/bypass/ —— WebView SNI 绕过引擎

为 WebView 内的 HTTP(S) 请求提供基于规则的 SNI 绕过能力,与 API 层共用同一套 SNI 原语。

## 各文件职责

| 文件 | 职责 |
|------|------|
| `BypassRule.kt` | 数据模型:`BypassRule`(目标 IP 列表)+ `HostPattern`(Exact 精确匹配 / Suffix 点边界后缀匹配)。 |
| `CealingHostParser.kt` | 将 Cealing-Host JSON 格式解析为 `BypassRule` 列表。 |
| `BypassRuleStore.kt` | 启动时加载 `assets/bypass/cealing-host.json` + 内置补充规则;`match(host)` 按最长后缀优先返回匹配规则。 |
| `BypassResolver.kt` | 候选 IP = rule.ip ∪ DoH 查询;运行时探测可达性;对结果做正/负 TTL 缓存,返回 `Endpoint`。 |
| `WebViewBypassInterceptor.kt` | `shouldInterceptRequest` 拦截管道:tracker 屏蔽 → pximg→imageHttpClient → 规则命中→解析→逐 Endpoint 重发 → 未命中返回 null。 |

## 请求数据流

```
WebView 请求
  └─ WebViewBypassInterceptor.shouldInterceptRequest
       ├─ [tracker] → 返回空响应(屏蔽)
       ├─ [pximg]   → 直接走 imageHttpClient(已有直连 IP)
       ├─ [规则命中] → BypassResolver.resolve → 逐 Endpoint 重发(共用 ReplaceSniSocketFactory)
       └─ [其他]    → 返回 null(交由 WebView 默认处理)
```

## 关键边界

- **GET-only**:仅拦截 GET 请求;非 GET(POST/PUT 等)原样放行,不做 bypass。
- **SNI 原语复用**:Endpoint 重发使用 API 层的 `ReplaceSniSocketFactory` / `RubySSLSocketFactory`,不重复造轮子。
- **DoH 泛化**:`DohApiDns.lookupPublic` 供 Resolver 查询任意主机(非仅 Pixiv API 域名)。

## 许可声明(混合)

| 部分 | 许可 |
|------|------|
| `assets/bypass/cealing-host.json` 规则数据 | **BSL-1.0**;来源:[SpaceTimee/Cealing-Host](https://github.com/SpaceTimee/Cealing-Host);完整声明见 `assets/bypass/NOTICE`。 |
| 本目录 Kotlin 源代码 | 项目本体许可(MIT)。 |

不打包任何站点 favicon。
