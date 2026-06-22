package com.perol.asdpl.pixivez.networks.bypass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BypassRuleStoreTest {
    private val rules = CealingHostParser.parse(
        """[
          [["*pixiv.net"],"pixiv.me","210.140.139.155"],
          [["www.pixiv.net"],"","210.140.139.223"],
          [["*pximg.net"],"","210.140.139.133"]
        ]"""
    )

    @Test fun longest_match_wins() {
        // www.pixiv.net 同时命中 "*pixiv.net" 与精确 "www.pixiv.net",取更具体者
        assertEquals("210.140.139.223", BypassRuleStore.matchIn(rules, "www.pixiv.net")?.ip)
    }

    @Test fun suffix_match_for_subdomain() {
        assertEquals("210.140.139.155", BypassRuleStore.matchIn(rules, "i.pixiv.net")?.ip)
    }

    @Test fun no_match_returns_null() {
        assertNull(BypassRuleStore.matchIn(rules, "example.com"))
    }
}
