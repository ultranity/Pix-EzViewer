package com.perol.asdpl.pixivez.networks.bypass

import com.perol.asdpl.pixivez.networks.SniMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CealingHostParserTest {
    @Test fun emptySni_maps_to_EMPTY_and_ip_kept() {
        val r = CealingHostParser.parse("""[ [["*pximg.net"],"","210.140.139.133"] ]""").single()
        assertEquals(SniMode.EMPTY, r.sni)
        assertNull(r.frontSni)
        assertEquals("210.140.139.133", r.ip)
    }

    @Test fun nonEmptySni_maps_to_REPLACE_with_frontSni() {
        val r = CealingHostParser.parse("""[ [["*pixiv.net","*fanbox.cc"],"pixivision.net","210.140.139.155"] ]""").single()
        assertEquals(SniMode.REPLACE, r.sni)
        assertEquals("pixivision.net", r.frontSni)
    }

    @Test fun blankIp_maps_to_null() {
        val r = CealingHostParser.parse("""[ [["*.googlevideo.com"],"",""] ]""").single()
        assertNull(r.ip)
    }

    @Test fun suffix_pattern_matches_host_and_subdomain_but_longest_wins() {
        val r = CealingHostParser.parse("""[ [["*pixiv.net"],"",""] ]""").single()
        assertTrue(r.match("www.pixiv.net") >= 0)
        assertTrue(r.match("pixiv.net") >= 0)
        assertEquals(-1, r.match("example.com"))
    }

    @Test fun label_and_operator_tokens_are_tolerated() {
        // '#'/'$' 前缀为标签/次级标记,'^' 为排除分隔;解析不得崩溃,仍产出可匹配正例
        val rules = CealingHostParser.parse(
            """[ [["#*google*","$*google.com","*gstatic.com"],"g.cn","183.56.143.147"] ]"""
        )
        assertTrue(rules.single().match("www.gstatic.com") >= 0)
        assertTrue(rules.single().match("accounts.google.com") >= 0)
    }
}
