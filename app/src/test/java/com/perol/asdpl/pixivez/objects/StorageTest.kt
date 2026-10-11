package com.perol.asdpl.pixivez.objects

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageTest {
    @Test
    fun relPathSplitsUserFormatsAndDropsEmptySegments() {
        assertEquals(
            listOf("xRestrict", "123", "1_p0.png"),
            Storage.relPath("xRestrict/", "", "123/1_p0.png")
        )
        assertEquals(listOf("a.jpg"), Storage.relPath("", "/a.jpg"))
    }
}
