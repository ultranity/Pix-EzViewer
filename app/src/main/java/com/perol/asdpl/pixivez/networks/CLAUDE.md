# networks/ —— 网络层

Pixiv API 的鉴权、接口、图片与下载的 HTTP 栈。基于 OkHttp 4 + Retrofit 3。

## 核心矛盾:连通性与反审查

这些域名有两类入口,对"无 SNI"处理不同:

- **Pixiv 自有源站**(IDC Frontier/JP,如 `210.140.139.x`,AS4694):nginx 直接服务,
  接受无 SNI 并按 Host 路由 —— 直连它即绕开 Cloudflare 边缘。
- **Cloudflare 共享 anycast**(公共 DNS/DoH 返回,`104.x`/`172.64.x`,AS13335):
  无 SNI 时选不出证书 → TLS 握手失败;须带(替换/明文)SNI。

故 DNS 与 SNI 两维度**有耦合**(空/替换 SNI 须配源站):

- **DNS 维度** —— 连哪类入口:DIRECT(Pixiv 源站)/ DoH(anycast)/ 系统。
- **SNI 维度** —— ClientHello 呈现:替换(pixiv.me)/ 空 / 明文(可被 GFW RST)。

墙内可用组合 = **DIRECT(直连源站) + 替换 SNI=pixiv.me**(空 SNI 亦可,但部分入口
默认证书不覆盖三段域名会 421);无墙/代理 = DoH + 明文。

## 文件职责

| 文件 | 职责 |
|------|------|
| `NetworkMode.kt` | **API 连接两轴模型**:`DnsMode`(direct/doh/system)× `SniMode`(replace/empty/plain)+ `VerifyConfig`(证书+主机名校验开关,默认开)+ `DohConfig`(默认 cloudflare-dns.com + bootstrap)+ `SniReplaceConfig`(替换 SNI 主机,默认 pixiv.me;`candidates()` 读 SAN、`autoSelect()` 逐个实测自动择优)+ `DohApiDns`(DoH→anycast)+ `PixivDirectDns`(源站直连 IP,IPv4 校验)+ `applyApiNetwork()`。 |
| `ReplaceSniSocketFactory.kt` | 把 ClientHello 的 SNI 替换为指定主机(默认 pixiv.me)的 SSLSocketFactory;供 `SniMode.REPLACE` 用。 |
| `RestClient.kt` | 各 Retrofit/OkHttp 客户端构建。API/鉴权走 `applyApiNetwork()`(含 421 显式处理);图片/下载走 `imageProxySocket()`。 |
| `ServiceFactory.kt` | Retrofit 构建器 + `CFDNS`(经 1.1.1.1 的 DoH)。 |
| `RefreshToken.kt` | token 刷新/登录(经 `retrofitOauthSecure`)。 |
| `Pkce.kt` | OAuth PKCE。 |
| `RubySSLSocketFactory.kt` / `RubyX509TrustManager.kt` | 空 SNI 实现:用 `InetAddress` 重载建 SSLSocket(不发 SNI)。供 API 的 `SniMode.EMPTY` 与图片路径复用;信任全部仅在 `VerifyConfig` 关闭时启用。 |
| `ImageHttpDns.kt` | 图片域名(i/s.pximg.net)的直连 IP 池 + 负载均衡。 |
| `DnsUtil.kt` | 连通性自检。 |
| `ProgressInterceptor.kt` / `OkHttpUrlHeaderLoader.kt` / `ResponseBodySerializer.kt` | 下载进度 / Glide 集成 / 响应序列化。 |

## 两轴模型(`applyApiNetwork`)

**DnsMode**(pref `apiDnsMode`,默认 `direct`)— 连哪类入口
- `DIRECT`:Pixiv 自有源站([PixivDirectDns],硬编码 `210.140.139.x`,pref `apiDirectIPs` 可覆盖)。接受无 SNI;空-SNI 绕过落点。
- `DOH`:经 `DohConfig`(默认 `cloudflare-dns.com` + bootstrap IP)→ Cloudflare anycast;配明文。
- `SYSTEM`:系统 DNS。无墙 / 走代理 / VPN。

**SniMode**(pref `apiSniMode`,默认 `replace`)
- `REPLACE`:替换 SNI 为 `SniReplaceConfig`(默认 `pixiv.me`,pref `apiSniReplaceHost`,[ReplaceSniSocketFactory])。pixiv.me 不被 GFW 封,且其多-SAN 证书(含 oauth.secure 等)能授权目标 Host → 不致 421。**须配 `DIRECT`**。
- `EMPTY`:空 SNI([RubySSLSocketFactory])。须配 `DIRECT`;部分入口默认证书不覆盖三段域名(如 oauth.secure)会 421。
- `PLAIN`:明文真实 SNI + 证书校验。配 DoH/anycast;无墙 / 代理可用;墙内会被 SNI RST。

**VerifyConfig**(pref `apiVerifyCert`,默认开):REPLACE/EMPTY 下是否做证书+主机名校验。
默认开(源站多-SAN 证书可过校验,防 MITM);仅异常网络/入口需关(关=跳过主机名校验)。

默认 **DIRECT + REPLACE(pixiv.me) + 校验开**(墙内可用)。网络/DoH 设置变更经设置保存后
**真重启进程**(`snackbarForceRestart`)生效——单例按新配置重建。`PixivDirectDns` 硬编码源站
IP(不在公共 DNS,会随 Pixiv 轮换,失效需更新或经 `apiDirectIPs` 覆盖)。

设置入口:`SettingsFragment.showAPIConfigDialog()` 的「DNS 解析」「SNI 模式」下拉 +
「DoH 服务商」输入框 +「校验证书」开关 +「自动选 SNI」按钮(读 SAN 逐个实测择优)。
旧的 `dnsProxy` 开关现仅影响图片路径。
