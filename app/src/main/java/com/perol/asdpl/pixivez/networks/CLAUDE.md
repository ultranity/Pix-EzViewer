# networks/ 网络层

API / OAuth 使用 OkHttp 4 + Retrofit 3。2.3.0 默认使用 ECH；没有 Cronet。

## API ECH

- `EchApiTransport.kt`：通过 Conscrypt 2.7.0 提供 TLS 1.3 ECH，保留 OkHttp 的 HTTP/2、鉴权、超时与取消。
- 仅 `app-api.pixiv.net`、`oauth.secure.pixiv.net`、`accounts.pixiv.net` 要求 ECH；保留真实 URL/Host，并由系统信任管理器 + OkHttp 验证真实目标证书。
- 与 pixez-flutter 一致，从 `cloudflare-ech.com` 的 HTTPS DNS 记录获取 Cloudflare ECH 公钥与 IPv4 hints。AliDNS 主解析、Cloudflare 带 bootstrap 的严格验证 DoH 备用；每个解析请求限时 4 秒。解析客户端独立于 API 设置，不携带用户凭据。
- `EchConfigCache.kt`：尊重 DNS TTL（上限一天）、同步共享刷新、失败退避 30 秒；最近有效配置最多保留一天。连接失败或 421 触发限频失效；本层仅对 IOException 重试 GET/HEAD 一次，不重放 POST，421 直接返回，不回退明文 SNI。
- Conscrypt 2.7.0 的 engine socket 会包装 `X509ExtendedTrustManager` 并丢失 ECH policy。因此 `EchTrustManager` 必须保持普通 `X509TrustManager`，保留反射入口 `getNetworkSecurityPolicy()`（`@Keep`）。不能只设置 `setEchConfigList` 就认定 ECH 已开启。
- `EchHandshakeInstrumentedTest` 直接检查 Android 发出的 ClientHello：outer SNI 必须是 `cloudflare-ech.com`，必须包含 ECH 扩展。升级 Conscrypt 时必须重跑。
- ECH 模式自带 DNS、始终验证证书，设置页禁用旧 DNS/证书开关。此模式不使用旧 `apiDirectIPs`。

## 旧模式与迁移

`NetworkMode.kt` 保留 `DnsMode`（direct/doh/system）及 `SniMode`（ech/replace/empty/plain）。ECH 为默认；旧模式需按当前网络实际验证。

历史单一网络环境的手机排查记录（不是本次 2.3.0 验收）：源站 + pixiv.me 返回 421；源站 + 空 SNI 返回 403 HTML；真实明文 SNI 被重置。证书 SAN 同时覆盖多个主机不代表服务器允许 SNI/Host 不一致，403 也不代表内容加载成功。

`ApiNetworkMigration.kt` 在 Application 初始化网络客户端前，仅一次迁移旧的内置 DIRECT + REPLACE/EMPTY 配置；显式自定义源站/SNI、SYSTEM/PLAIN 等配置保留。迁移标记写入后，用户仍可切回旧模式。设置保存后重启进程重建单例。

`ReplaceSniSocketFactory` / `RubySSLSocketFactory` 在 OkHttp 已连接的 socket 上建立 TLS，不另开 TCP。旧模式禁用 TLS extensions，兼容会恢复 SNI 的旧 Android 适配器（Android 29+ 的 OkHttp 适配器不会覆盖 SNI，但此旧路径仍统一使用 HTTP/1.1）。ECH 保持 ALPN。

## 图片与普通 DoH

- `DohTransport.kt`：Cloudflare 域名端点 + bootstrap；无 SNI 优先、严格验证的常规 TLS 备用。自定义提供商必须 HTTPS。
- `RefreshingDns.kt` / `ImageHttpDns.kt`：按主机缓存，5 分钟刷新、失败退避 60 秒、持久最近有效地址最多保留 24 小时；自定义地址 > 动态地址 > 内置备用地址。移除 ICMP 淘汰 IP。
- `ImageDnsHosts.kt`：限定 pximg 域名，force-IP URL 失败时从原 Host 找回缓存键。其他主机交系统 DNS。
- `Works.cachedForUrl` 路径仅读快照并启动后台刷新，不在 UI 线程等待 DNS。
- `RestClient.kt`：API / OAuth 经 `applyApiNetwork()`；图片下载经 `imageProxySocket()`，不使用 ECH。`dnsProxy` 只影响图片。
- `ServiceFactory.CFDNS` 与 API / WebView 共享严格验证 DoH。`bypass/` 是独立 WebView GET 拦截层，本次未重构网页登录。

## 验证

JVM 测试验证缓存、解析、模式迁移、重试限制与旧 socket 行为。Android 测试覆盖实际 ECH ClientHello 与 ICU 小说正则。`PixivConnectivityInstrumentedTest` 必须显式传 `-e liveNetwork true`，在已有登录态的手机上使用正式客户端读取首页、刷新、小说详情和正文；不记录凭据或内容。
