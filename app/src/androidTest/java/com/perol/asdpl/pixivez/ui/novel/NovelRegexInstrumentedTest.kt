package com.perol.asdpl.pixivez.ui.novel

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android's java.util.regex.Pattern is backed by ICU. Keep this on-device
 * check next to the JVM parser tests so a desktop-only regex regression cannot
 * make NovelViewModel fail during class initialization.
 */
@RunWith(AndroidJUnit4::class)
class NovelRegexInstrumentedTest {
    @Test
    fun testNovelParserPatternsCompileOnAndroidIcu() {
        val html = """
            novel: {"id":"9","text":"正文[newpage]下一页",
            "images":{"77":{"urls":{"480mw":"https://i/u.jpg"}}}},
            isOwnWork: false
        """.trimIndent()

        val web = parseWebNovel(html)
        assertNotNull("embedded novel JSON should parse on Android", web)
        assertEquals("正文[newpage]下一页", web!!.text)
        assertEquals(2, chunkNovel(web.text).size)
        assertEquals("\n标题\n正文", renderNovelText("[chapter:标题]正文"))
    }

    @Test
    fun testEmptyImageArraysMatchTheProductionWebviewShape() {
        val html = """
            novel: {"id":"9","text":"正文","images":[],"illusts":[]},
            isOwnWork: false
        """.trimIndent()

        val web = parseWebNovel(html)
        assertNotNull(web)
        assertTrue(web!!.images!!.isEmpty())
        assertTrue(web.illusts!!.isEmpty())
    }
}
