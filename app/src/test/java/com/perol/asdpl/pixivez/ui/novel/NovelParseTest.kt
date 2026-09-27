package com.perol.asdpl.pixivez.ui.novel

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NovelParseTest {
    // 模拟 /webview/v2/novel HTML 内嵌 JSON(锚点:novel: {...}, isOwnWork)
    private val html = """
        <html><script>pixiv.novel = { novel: {"id":"123","title":"T",
        "text":"第一页[newpage]第二页","seriesNavigation":{
        "prevNovel":{"id":1,"viewable":true,"title":"p"},"nextNovel":null}},
        isOwnWork: false };</script></html>
    """.trimIndent()

    @Test fun parse_extracts_text_and_series() {
        val web = parseWebNovel(html)!!
        assertEquals("123", web.id)
        assertEquals("第一页[newpage]第二页", web.text)
        assertEquals(1, web.seriesNavigation?.prevNovel?.id)
        assertNull(web.seriesNavigation?.nextNovel)
    }

    @Test fun parse_returns_null_without_anchor() {
        assertNull(parseWebNovel("<html>no embedded json</html>"))
    }

    @Test fun parse_resolves_image_maps() {
        val h = """x novel: {"id":"9","text":"t",
            "images":{"77":{"urls":{"480mw":"https://i.pximg.net/u480.jpg","original":"https://i.pximg.net/uo.jpg"}}},
            "illusts":{"55":{"illust":{"images":{"medium":"https://i.pximg.net/m.jpg"}}}}},
            isOwnWork: true"""
        val web = parseWebNovel(h)!!
        assertEquals("https://i.pximg.net/u480.jpg", web.images?.get("77")?.urls?.mw480)
        assertEquals("https://i.pximg.net/m.jpg", web.illusts?.get("55")?.illust?.images?.medium)
    }

    @Test fun parse_treats_empty_image_arrays_as_empty_maps() {
        val h = """x novel: {"id":"9","text":"t","images":[],"illusts":[]}, isOwnWork: true"""
        val web = parseWebNovel(h)!!
        assertTrue(web.images!!.isEmpty())
        assertTrue(web.illusts!!.isEmpty())
    }

    @Test fun unavailableIllustrationsDoNotDiscardTheNovelBody() {
        val h = """novel: {"id":"9","text":"正文[pixivimage:55]下一段",
            "illusts":{"55":null,"56":{"illust":null}}}, isOwnWork: false"""
        val web = parseWebNovel(h)!!
        assertEquals("正文[pixivimage:55]下一段", web.text)
        assertNull(web.illusts?.get("55"))
        assertNull(web.illusts?.get("56")?.illust)
        assertTrue(chunkNovel(web.text).isNotEmpty())
    }

    @Test(expected = SerializationException::class)
    fun parse_rejects_non_empty_image_array_instead_of_dropping_it() {
        val h = """x novel: {"id":"9","text":"t","images":[{"urls":{}}]}, isOwnWork: true"""
        parseWebNovel(h)
    }

    @Test(expected = SerializationException::class)
    fun parse_rejects_non_empty_illust_array_instead_of_dropping_it() {
        val h = """x novel: {"id":"9","text":"t","illusts":[{"illust":{}}]}, isOwnWork: true"""
        parseWebNovel(h)
    }

    @Test fun render_text_transforms_all_markers() {
        assertEquals(
            "\n标题\n汉字(かんじ)链接",
            renderNovelText("[chapter:标题][[rb:汉字>かんじ]][[jumpuri:链接>https://x.example]]")
        )
        assertEquals("a\n\nb", renderNovelText("a[newpage]b"))
        assertEquals("ab", renderNovelText("a[pixivimage:1-2][uploadedimage:3][jump:4]b"))
    }

}
