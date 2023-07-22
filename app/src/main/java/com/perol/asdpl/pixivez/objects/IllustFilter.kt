package com.perol.asdpl.pixivez.objects

import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.ui.settings.BlockViewModel

class IllustFilter(
    var R18on: Boolean = false,
    var blockTags: List<String>? = null,
    var hideBookmarked: Int = 0,
    var sortCoM: Int = 0,
    var hideDownloaded: Boolean = false
) {
    fun needHide(item: Illust): Boolean {
        return (
            ((hideBookmarked == 1 && item.is_bookmarked) || (hideBookmarked == 3 && !item.is_bookmarked)) ||
                (sortCoM == 1 && item.type != "manga") || (sortCoM == 2 && item.type == "manga") ||
                (hideDownloaded && FileUtil.isDownloaded(item))
            )
    }
    fun needBlock(item: Illust): Boolean {
        val blockTagString = BlockViewModel.getBlockTagString()
        if (blockTags.isNullOrEmpty() and blockTagString.isEmpty()) {
            return false
        }

        val tags = item.tags.map { it.name }
        if (tags.isNotEmpty()) {
            blockTags?.let { for (i in it) {
                if (tags.contains(i)) {
                    return true
                }
            }}
            for (i in blockTagString) {
                if (tags.contains(i)) {
                    return true
                }
            }
        }
        return false
    }

    fun Illust.needHide(filter: IllustFilter): Boolean {
        return filter.needHide(this)
    }
    fun Illust.needBlock(filter: IllustFilter): Boolean {
        return filter.needBlock(this)
    }
}
