package com.perol.asdpl.pixivez.ui.novel

import androidx.lifecycle.MutableLiveData
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.data.model.Novel
import com.perol.asdpl.pixivez.data.model.NovelWebResponse
import com.perol.asdpl.pixivez.networks.ServiceFactory.gson
import com.perol.asdpl.pixivez.objects.CrashHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 阅读页数据:详情(标题/作者/标签/收藏态)+ 正文(webview HTML 抽 JSON)
class NovelViewModel : BaseViewModel() {
    val novel = MutableLiveData<Novel?>()
    val web = MutableLiveData<NovelWebResponse?>()
    // 正文分块结果(图文混排,见 NovelMarkup.chunkNovel),Default 线程算完再回主线程
    val chunks = MutableLiveData<List<NovelChunk>>()
    // is_bookmarked 在 Novel 中不可变,收藏态单独维护以便乐观更新
    val bookmarked = MutableLiveData(false)
    val bodyLoading = MutableLiveData(false)
    val bodyFailed = MutableLiveData(false)

    fun load(id: Int) {
        launchUI {
            try {
                val n = retrofit.api.getNovelDetail(id).novel
                novel.value = n
                bookmarked.value = n.is_bookmarked
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CrashHandler.instance.e("novel", "detail $id failed", e)
                novel.value = null
            }
        }
        loadText(id)
    }

    fun loadText(id: Int) {
        launchUI {
            bodyLoading.value = true
            bodyFailed.value = false
            try {
                val html = withContext(Dispatchers.IO) {
                    retrofit.api.getNovelText(id).use { it.string() }
                }
                val result = requireNotNull(parseWebNovel(html)) { "Novel payload missing" }
                require(result.text.isNotBlank()) { "Novel body is empty" }
                val parsedChunks = withContext(Dispatchers.Default) {
                    chunkNovel(
                        result.text,
                        resolvePixiv = { pid ->
                            result.illusts?.get(pid.toString())?.illust?.images
                                ?.let { it.medium ?: it.original }
                        },
                        resolveUploaded = { iid ->
                            result.images?.get(iid)?.urls?.let { it.mw480 ?: it.original }
                        },
                    )
                }
                web.value = result
                chunks.value = parsedChunks
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Do not hide a webview/JSON error behind a second /v1/novel/text
                // request (which may return 404). Keep the original failure retryable.
                CrashHandler.instance.e("novel", "body $id failed", e)
                web.value = null
                chunks.value = emptyList()
                bodyFailed.value = true
            } finally {
                bodyLoading.value = false
            }
        }
    }

    fun toggleBookmark(id: Int) {
        val cur = bookmarked.value == true
        launchUI {
            try {
                if (cur) retrofit.api.postUnlikeNovel(id)
                else retrofit.api.postLikeNovel(id, "public", null)
                bookmarked.value = !cur
            } catch (e: Exception) {
                CrashHandler.instance.e("novel", "bookmark $id failed", e)
            }
        }
    }
}

// webview/v2/novel 返回 HTML,内嵌 `novel: {...}, isOwnWork`。
// 懒惰匹配 `{.*?}` + isOwnWork 锚点回溯定位到正确的闭合括号;DOTALL 让 `.` 跨行。
// Android Pattern delegates to ICU, where the literal closing brace must be escaped; the JDK accepts it unescaped.
private val NOVEL_JSON = Regex("""novel:\s*(\{.*?\}),\s*isOwnWork""", RegexOption.DOT_MATCHES_ALL)

fun parseWebNovel(html: String): NovelWebResponse? {
    val json = NOVEL_JSON.find(html)?.groupValues?.getOrNull(1) ?: return null
    return gson.decodeFromString<NovelWebResponse>(json)
}

// pixiv 正文自有标记 → 纯文本(导出/纯文字场景用;阅读页走 NovelMarkup 图文管线)
fun renderNovelText(raw: String): String =
    raw
        .replace(Regex("""\[newpage\]"""), "\n\n")
        .replace(Regex("""\[chapter:(.*?)\]"""), "\n$1\n")
        .replace(Regex("""\[\[rb:(.*?)>(.*?)\]\]"""), "$1($2)")
        .replace(Regex("""\[\[jumpuri:(.*?)>.*?\]\]"""), "$1")
        .replace(Regex("""\[pixivimage:[^\]]*\]"""), "")
        .replace(Regex("""\[uploadedimage:[^\]]*\]"""), "")
        .replace(Regex("""\[jump:[^\]]*\]"""), "")
