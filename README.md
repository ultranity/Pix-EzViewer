#  PixEz 阅读器 ![PixEz](https://github.com/Notsfsssf/Pix-EzViewer/raw/master/app/src/main/res/mipmap-xxhdpi/ic_launcherep.png)

一个支持免代理直连 + 多种额外特性功能优化的第三方 Pixiv android 客户端 。（Android 5.0+）
 >A third-party Pixiv Android client with modern design and many other enhancements. (Android 5.0+)
 
[![当前版本](https://img.shields.io/github/v/release/ultranity/Pix-EzViewer.svg)](https://github.com/ultranity/Pix-EzViewer/releases/latest)
 [![Codacy Badge](https://api.codacy.com/project/badge/Grade/a030ea8419b84907aeed53472abdcd91)](https://app.codacy.com/manual/ultranity/Pix-EzViewer?utm_source=github.com&utm_medium=referral&utm_content=ultranity/Pix-EzViewer&utm_campaign=Badge_Grade_Dashboard)
[![Downloads](https://img.shields.io/github/downloads/ultranity/Pix-EzViewer/total?color=FFAA11)](https://github.com/ultranity/Pix-EzViewer/releases)
 
**!原作者[Notsfsssf](https://github.com/Notsfsssf)因学业及flutter android版PixEz开发原因停止维护**

**经商议，从 1.5.4W 版本开始将由 [我](https://github.com/ultranity) 接手继续维护 [Pix-EzViewer](https://github.com/ultranity/Pix-EzViewer)，[点此查看更新记录及TODO](https://github.com/ultranity/Pix-EzViewer/blob/master/ReleaseNote.md),目前建议优先通过本repo提交issue进行反馈**

## 1.功能特性
* aria下载加速
* 多种自定义文件命名方式（保存tags等信息）
* 夜间模式
* 多用户切换
* GIF播放、保存
* 查看\添加\回复评论
* Pixiv特辑
* R80显示（需自行到官网开启），自动私密收藏，独立下载文件夹
* 隐藏已收藏图片
* …… 更多特性请下载体验

如果你觉得这个应用还不错，[点此](https://github.com/Notsfsssf/Pix-EzViewer#支持) 支持一下吧！

# 下载

|                                            来源                                             |                                                  说明                                                   |
|:-------------------------------------------------------------------------------------------:|:------------------------------------------------------------------------------------------------------:|
|            [GitHub Release](https://github.com/ultranity/Pix-EzViewer/releases)             |               点这个，在 GitHub Release 页面下载完整 APK<br />适用于没有 Play / 分享给他人时               |
|  [Google Play](https://play.google.com/store/apps/details?id=com.perol.asdpl.play.pixivez)  | ~~**★推荐!** 点这个，从 Google Play 上下载<br />需要已配置好 Play 全家桶，更新方便~~<br />因bugly依赖已下架 |                                                          |
|         [F-droid](https://f-droid.org/packages/com.perol.asdpl.play.pixivez.libre/)         |                                  F-droid 分发，无bugly日志，社区更新较慢                                  |

Java 端通过修改 `OkHttp` 的 `SSLSocket` 实现绕过 SNI 审查（即旁路阻断）直连 Pixiv 的功能

- 具体实现 [点此](https://github.com/Notsfsssf/Pix-EzViewer/tree/master/app/src/main/java/com/perol/asdpl/pixivez/networks) 浏览

如果直连代码对你有所启发，在项目或者程序中注明我的 ID 的话，我会很高兴的ヽ✿゜▽゜)ノ

***

如果你正在使用 Muzei 的话，那么由 [@Antony](https://github.com/yellowbluesky) 开发的 [Pixiv for Muzei 3](https://github.com/yellowbluesky/PixivforMuzei3) 是个不错的选择；

如果你需要一个 UWP 客户端，那么由 [@tobiichiamane](https://github.com/tobiichiamane) 开发的 [pixivfs-uwp ](https://github.com/tobiichiamane/pixivfs-uwp)会是不二之选；

如果你需要一个 WPF 客户端，那么可以尝试一下由 [@Rinacm](https://github.com/Rinacm) 开发的 [Pixeval](https://github.com/Rinacm/Pixeval) 。

# Preview

| ![Preview](./preview/2.jpg) | ![Preview](./preview/1.jpg) | ![Preview](./preview/3.jpg) |
|:---------------------------:|:---------------------------:|:---------------------------:|

# 交流反馈

对于普通用户，**如果在使用过程中有任何疑问，请先参考我们的 [常见问题](/help/README.md) 页进行快速自查。**

如果你的问题不在上述页面范围内，你可以通过 Email (Pix-Ez@outlook.com) 或 [GitHub Issues](https://github.com/ultranity/Pix-EzViewer/issues) 或 下面的聊天群 提交反馈。

![](https://img.shields.io/badge/PR-welcome-blue.svg)

如果你是有能 man，且愿意为本项目贡献代码，请不要犹豫提交 Pull Request 吧！

此外，你可以进企鹅群交流经验或者反馈：815791942

也可以在 Telegram 群交流反馈：[@PixEzViewer](https://t.me/PixEzViewer)

## 支持

如果你觉得这个应用还不错，就支持一下吧！

|                                       国内                                       |                                                                                                        国外                                                                                                         |
|:------------------------------------------------------------------------------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| [点此](https://github.com/ultranity/Pix-EzViewer/blob/master/donation/README.md) | <a href='https://ko-fi.com/W7W5YU4B' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://az743702.vo.msecnd.net/cdn/kofi1.png?v=2' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a> |
