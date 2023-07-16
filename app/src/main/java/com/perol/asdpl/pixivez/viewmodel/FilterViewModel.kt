/*
 * MIT License
 *
 * Copyright (c) 2022 ultranity
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package com.perol.asdpl.pixivez.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.PicListBtnAdapter
import com.perol.asdpl.pixivez.adapters.PicListBtnUserAdapter
import com.perol.asdpl.pixivez.adapters.PicListXAdapter
import com.perol.asdpl.pixivez.adapters.PicListXUserAdapter
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.objects.KotlinUtil.plus
import com.perol.asdpl.pixivez.objects.KotlinUtil.times
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.PxEZApp

enum class ADAPTER_VERSION {
    PIC_LIKE,
    PIC_USER_LIKE,
    PIC_BTN,
    PIC_USER_BTN,
}

class PicListFilter(
    var R18on: Boolean = false,
    var blockTags: List<String>? = null,
    var blockUser: List<Long>? = null,
    var max_sanity: Int = 8,

    var minLike: Int = 0,
    var minViewed: Int = 0,
    var minLikeRate: Float = 0f,

    var showBookmarked: Boolean = true,
    var showNotBookmarked: Boolean = true,
    var showDownloaded: Boolean = true,
    var showNotDownloaded: Boolean = true,

    var showIllust: Boolean = true,
    var showManga: Boolean = true,
    var showUgoira: Boolean = true,

    var showFollowed: Boolean = true,
    var showNotFollowed: Boolean = true,

    var startTime:Long = 0,
    var endTime:Long = Long.MAX_VALUE,

    var sortTime: Boolean = false,
    var sortLike: Boolean = false,
    var sortLikeRate: Boolean = false,
) {
    fun needHide(item: Illust): Boolean =
        (!showBookmarked && item.is_bookmarked) || (!showNotBookmarked && !item.is_bookmarked) ||
        (!showFollowed && item.user.is_followed) || (!showNotFollowed && !item.user.is_followed) ||
        (!showDownloaded && FileUtil.isDownloaded(item)) || (!showNotDownloaded && !FileUtil.isDownloaded(item))||
        (!showIllust && (item.type == "illust")) || (!showManga && (item.type == "manga")) || (!showUgoira && (item.type == "ugoira"))
        //|| (minLike > item.total_bookmarks) || (minViewed > item.total_view) || (minLikeRate*item.total_view> item.total_bookmarks)

    fun needBlock(item: Illust): Boolean {
        if (blockTags.isNullOrEmpty() and blockUser.isNullOrEmpty()) {
            return false
        }
        val tags = item.tags.map{ it.name }
        if (tags.isNotEmpty()) {
            // if (blockTags.intersect(tags).isNotEmpty())
            for (i in blockTags!!) {
                if (tags.contains(i)) {
                    return true
                }
            }
        }
        return blockUser!!.contains(item.user.id)
    }

    fun applyTo(filter: IllustFilter){
        filter.let{
            it.R18on = R18on
            it.hideBookmarked = !showBookmarked + (!showNotBookmarked)*2
            it.hideDownloaded = !showDownloaded
            it.sortCoM = if (!showManga) 1 else (if (showIllust) 0 else 2)
            it.blockTags = blockTags
        }
    }

}
class FilterViewModel : ViewModel() {
    var TAG = "FilterViewModel"
    val spanNum = MutableLiveData(2)
    val filter = IllustFilter()
    val listFilter = PicListFilter()
    var adapterVersion = MutableLiveData(ADAPTER_VERSION.PIC_USER_LIKE)
    init {
        listFilter.R18on = PxEZApp.instance.pre.getBoolean("r18on", false)
    }
    fun applyConfig() = listFilter.applyTo(filter)
    fun getAdapter() = when (adapterVersion.value) {
        ADAPTER_VERSION.PIC_BTN -> PicListBtnAdapter(R.layout.view_recommand_item, null, filter)
        ADAPTER_VERSION.PIC_USER_BTN -> PicListBtnUserAdapter(R.layout.view_ranking_item, null, filter)
        ADAPTER_VERSION.PIC_LIKE -> PicListXAdapter(R.layout.view_recommand_item_s, null, filter)
        ADAPTER_VERSION.PIC_USER_LIKE -> PicListXUserAdapter(R.layout.view_ranking_item_s, null, filter)
        else -> PicListXUserAdapter(R.layout.view_ranking_item_s, null, filter)
    }
}
