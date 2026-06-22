# ui/ —— 界面层

各 Activity/Fragment 的 UI 骨架与导航。

## 浏览器入口

| 文件 | 职责 |
|------|------|
| `WebViewActivity.kt` | **唯一套壳浏览器**:含 Toolbar + 可编辑地址栏 + 进度条 + 下拉刷新 + 菜单;接入 `WebViewBypassInterceptor` 实现 SNI 绕过拦截。地址栏输入 URL 直达、关键词走搜索引擎。 |

> `OKWebViewActivity` 已在 Task 5/6 并入 `WebViewActivity` 后删除。所有调用方统一
> launch `WebViewActivity`;`dnsProxy` 开关不再选 Activity(仅影响图片路径)。

## OAuth 登录

| 文件 | 职责 |
|------|------|
| `account/NewUserActivity.kt` | OAuth PKCE 网页登录;独立保留,不经过 bypass 拦截器。 |

## 其他模块

| 目录 | 职责 |
|------|------|
| `home/` | 首页 Feed / 推荐 / 关注。 |
| `search/` | 搜索结果页。 |
| `pic/` | 作品详情 / 图片浏览。 |
| `user/` | 用户主页。 |
| `manager/` | 下载管理。 |
| `settings/` | 设置页(含网络/SNI 配置)。 |
| `MainActivity.kt` | 底部导航宿主。 |
| `FragmentActivity.kt` | 公共 Fragment 宿主基类。 |
