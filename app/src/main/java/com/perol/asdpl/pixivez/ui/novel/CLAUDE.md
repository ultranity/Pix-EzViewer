# ui/novel/ —— 小说模式

小说的独立浏览模式:首页(推荐/排行/动态)+ 搜索 + 用户小说列表 + 阅读页。
体验对齐插画模式但展示小说。数据模型复用 `data/model/Novel.kt`
(`Novel` / `NovelResponse` / `NovelDetailResponse` / `NovelWebResponse`；
`NovelTextResponse` 仅保留旧 API 模型),网络经 `PixivApiService` 的 novel 端点 +
`RetrofitRepository.getNextNovels` 分页。

## 模式装配(入口在 ui/MainActivity)

`main_mode` pref("illust"/"novel",`PxEZApp.instance.pre`)决定 MainActivity
装配哪个 pager:`NovelMainViewPager` 或 `HelloMainViewPager`。抽屉项
`nav_novel_mode` 翻转 pref 后 `finish() + startActivity` 全新实例重装——
不能用 `recreate()`:其 savedInstanceState 会让 FragmentPagerAdapter 按
tag(两模式相同,默认 itemId=position)复用旧模式 fragment。三个 tab 图标
(home/rank/user)两模式共用,语义上对应 推荐/排行/动态。所有按词搜索
统一走 `ui/search/SearchRouter.startWordSearch` 按 `main_mode` 分派:
novel 模式落地 `NovelSearchResultActivity`,illust 模式落地
`SearchResultActivity`(带 775 回传契约);新增搜索入口必须走它。

## 文件职责

| 文件 | 职责 |
|------|------|
| `NovelMainViewPager.kt` | 小说首页三 tab(`FragmentPagerAdapter`,与 `HelloMainViewPager` 对齐):推荐=`NovelListFragment(Recommend)`、排行=`NovelRankFragment`、动态=`NovelListFragment(Follow)`。 |
| `NovelRankFragment.kt` | 排行页:`ViewPager2 + TabLayout` 每个 rank mode 一个子 tab(照 `RankingMAdapter`)。mode 全序表在 `buildModes`,r18/r18g 按用户 `x_restrict` + `r18on` 门控;标签取 `R.array.novel_rank_mode`。日期取最新(端点 `date` 可空)。内含私有 `NovelRankAdapter`。 |
| `NovelSearchResultActivity.kt` | 小说模式轻量搜索结果页(`RinkActivity`)。承载 `NovelListFragment(Search)`,不复用插画 `SearchResult` 的 tab 体系。Manifest 已注册。 |
| `NovelListFragment.kt` | 泛化小说列表 `BaseVBFragment<FragmentNovelListBinding>`。`newInstance(tag, extraArgs)` 对齐 `PicListFragment`;线性排布 + next_url 上拉分页。Follow/UserBookmark 顶部提供 公开/非公开 切换。 |
| `NovelListViewModel.kt` | 列表数据源。`NOVEL_TAG`(Recommend/Rank/Follow/UserNovels/UserBookmark/Search)经 `loadFirstRx` 选端点;`restrict` 供 Follow/UserBookmark 切换;`getNextNovels(next_url)` 加载更多。 |
| `NovelListAdapter.kt` | 列表项适配器 `LBaseQuickAdapter<Novel>`(封面 + 标题 + 作者 + 字数/收藏数),点击进 `NovelActivity`。 |
| `NovelActivity.kt` | 阅读页(`RinkActivity`)。图文分块渲染:`RecyclerView` + 私有 `NovelReaderAdapter` 三 viewType——position 0 头部(`view_novel_header`,标题/作者点击跳主页/系列名/简介 HTML/标签点击跳小说搜索),正文 Text 块(`view_novel_chunk`,bind 时 `tokenize`→Spannable:chapter 居中加粗、ruby 括注、jumpuri/jump 可点)与 Image 块(`view_novel_image`,Glide 加载,pixivimage 点击跳插画,url 缺失显示标记占位)。字号记忆(`novel_text_size`,12~32sp)、背景主题+行距(`novel_bg`/`novel_line_space`,菜单单选)、收藏 toggle、系列上/下一篇、`[jump:N]` 按页号滚动、进度经 `NovelProgressStore` onPause 存/首载恢复。 |
| `NovelViewModel.kt` | 阅读页数据。并行拉详情(元数据 + 收藏态)与正文;`parseWebNovel` 从 webview HTML 抽 JSON,失败时保留错误并提供重试;正文经 `chunkNovel` 在 Default 线程切块,图源 resolver 闭包取自 webview JSON 的 `images`(uploadedimage)与 `illusts`(pixivimage)映射;`renderNovelText` 将标记转纯文本(导出/纯文字场景)。 |
| `NovelMarkup.kt` | 标记解析纯函数层(零 Android 依赖,JVM 单测直跑)。`chunkNovel`:`[newpage]` 页界分块 + 图片标记抽成 `NovelChunk.Image` + 超限页按段落再切(单块 ≤3000 字符),页号随块透传;`tokenize`:Text 块行内标记 → `NovelToken`(Plain/Chapter/Ruby/JumpUri/JumpPage),未识别标记原样保留。 |
| `NovelProgressStore.kt` | 阅读进度:novelId → (块位置, 像素偏移),JSON 存 SharedPreferences,LRU 上限 200(`trimLru` 纯函数可测)。 |

## 正文获取(关键约束)

正文优先走 `GET /webview/v2/novel?id=`,返回 HTML,内嵌
`pixiv.novel = { novel: {...}, isOwnWork: ... }`。`parseWebNovel` 以懒惰匹配
`novel:\s*(\{.*?\}),\s*isOwnWork`(DOTALL)+ `isOwnWork` 锚点定位闭合括号,
取组 1 用 `ServiceFactory.gson` 解析为 `NovelWebResponse`。抽取失败时正文进入
失败状态并可重试，不再调用会掩盖原始错误的旧纯文本接口。正文内联图片经
webview JSON 的图源映射解析:`[uploadedimage:id]` → `images[id].urls`
(480mw 优先),`[pixivimage:id]` → `illusts[id].illust.images`(medium 优先);
解析不到 url 的图片块显示原始标记占位。系列导航取自
`seriesNavigation.{prevNovel,nextNovel}`。

## 数据源端点(NOVEL_TAG → PixivApiService)

| NOVEL_TAG | 端点 |
|-----------|------|
| Recommend | `/v1/novel/recommended` |
| Rank | `/v1/novel/ranking`(mode,date) |
| Follow | `/v1/novel/follow`(restrict) |
| UserNovels | `/v1/user/novels` |
| UserBookmark | `/v1/user/bookmarks/novel`(restrict) |
| Search | `/v1/search/novel`(word,sort,search_target,date) |

## 2.3.0 正文兼容

`NovelWebResponse` 的 `images` / `illusts` 只将空数组转换为空映射；非空数组仍报错，`illusts` 的失效引用允许为 null。正文仅走 `/webview/v2/novel`，解析失败不再回退到会掩盖原始错误的 `/v1/novel/text`。`NovelViewModel.bodyLoading/bodyFailed` 驱动阅读页进度和重试状态，标题/简介加载成功不代表正文已加载。真实 reader 验收需检查 RecyclerView 有正文行。
