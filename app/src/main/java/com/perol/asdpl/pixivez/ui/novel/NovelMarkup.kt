package com.perol.asdpl.pixivez.ui.novel

// ============================================================
// 正文标记解析:纯 Kotlin、零 Android 依赖,JVM 单测直跑。
// 管线:chunkNovel 切块(页界/图片/长页)→ Text 块 bind 时 tokenize
// ============================================================

// 单块字符上限:单个 TextView 的 StaticLayout 测量在主线程,块太大长文必卡
private const val CHUNK_LIMIT = 3000

// page 为 [newpage] 页号(0 起),[jump:N] 目标 = 首个 page==N-1 的块
sealed class NovelChunk {
    abstract val page: Int
    data class Text(override val page: Int, val text: String) : NovelChunk()
    // illustId 非空 = [pixivimage:](可点跳插画);url 空 = 未解析,UI 给占位
    data class Image(override val page: Int, val url: String?, val illustId: Int?) : NovelChunk()
}

private val IMAGE_MARK = Regex("""\[pixivimage:(\d+)(?:-\d+)?\]|\[uploadedimage:(\d+)\]""")

fun chunkNovel(
    raw: String,
    resolvePixiv: (Int) -> String? = { null },
    resolveUploaded: (String) -> String? = { null },
): List<NovelChunk> {
    val chunks = mutableListOf<NovelChunk>()
    raw.split("[newpage]").forEachIndexed { page, body ->
        var last = 0
        for (m in IMAGE_MARK.findAll(body)) {
            addTextChunks(chunks, page, body.substring(last, m.range.first))
            val pixivId = m.groups[1]?.value?.toInt()
            chunks += if (pixivId != null) {
                NovelChunk.Image(page, resolvePixiv(pixivId), pixivId)
            } else {
                NovelChunk.Image(page, resolveUploaded(m.groups[2]!!.value), null)
            }
            last = m.range.last + 1
        }
        addTextChunks(chunks, page, body.substring(last))
    }
    return chunks
}

// 文本段过滤空白后按 CHUNK_LIMIT 切段落,页号透传
private fun addTextChunks(out: MutableList<NovelChunk>, page: Int, text: String) {
    val t = text.trim()
    if (t.isBlank()) return
    val parts = if (t.length <= CHUNK_LIMIT) listOf(t) else splitByParagraph(t)
    parts.filter { it.isNotBlank() }.forEach { out += NovelChunk.Text(page, it) }
}

private fun splitByParagraph(page: String): List<String> {
    val chunks = mutableListOf<String>()
    val sb = StringBuilder()
    for (line in page.lineSequence()) {
        if (sb.isNotEmpty() && sb.length + line.length > CHUNK_LIMIT) {
            chunks += sb.toString().trim()
            sb.clear()
        }
        sb.appendLine(line)
    }
    if (sb.isNotBlank()) chunks += sb.toString().trim()
    return chunks
}

// Text 块 bind 时的行内标记;Ruby 显示为 base(rt),Jump* 转 ClickableSpan
sealed class NovelToken {
    data class Plain(val text: String) : NovelToken()
    data class Chapter(val title: String) : NovelToken()
    data class Ruby(val base: String, val rt: String) : NovelToken()
    data class JumpUri(val title: String, val url: String) : NovelToken()
    data class JumpPage(val page: Int) : NovelToken()
}

/**
 * Text appended for a chapter heading and the range that may receive heading
 * spans. The leading newline separates the previous paragraph, while the span
 * starts on the title so it cannot also align text that precedes the heading.
 * Keeping this calculation here makes that invariant testable without Android.
 */
data class NovelChapterSpanAppend(
    val text: String,
    val start: Int,
    val endExclusive: Int,
)

fun chapterSpanAppend(
    existingLength: Int,
    existingEndsWithNewline: Boolean,
    title: String,
): NovelChapterSpanAppend {
    val leadingBreak = if (existingLength > 0 && !existingEndsWithNewline) "\n" else ""
    val start = existingLength + leadingBreak.length
    val text = leadingBreak + title + "\n"
    return NovelChapterSpanAppend(text, start, start + title.length)
}

private val TOKEN_MARK = Regex(
    """\[chapter:(.*?)\]|\[\[rb:(.*?)>(.*?)\]\]|\[\[jumpuri:(.*?)>(.*?)\]\]|\[jump:(\d+)\]""",
    RegexOption.DOT_MATCHES_ALL
)

fun tokenize(text: String): List<NovelToken> {
    val tokens = mutableListOf<NovelToken>()
    var last = 0
    for (m in TOKEN_MARK.findAll(text)) {
        if (m.range.first > last) tokens += NovelToken.Plain(text.substring(last, m.range.first))
        val g = m.groups
        tokens += when {
            g[1] != null -> NovelToken.Chapter(g[1]!!.value.trim())
            g[2] != null -> NovelToken.Ruby(g[2]!!.value, g[3]?.value ?: "")
            g[4] != null -> NovelToken.JumpUri(g[4]!!.value.trim(), g[5]?.value?.trim() ?: "")
            else -> NovelToken.JumpPage(g[6]!!.value.toInt())
        }
        last = m.range.last + 1
    }
    if (last < text.length) tokens += NovelToken.Plain(text.substring(last))
    return tokens
}
