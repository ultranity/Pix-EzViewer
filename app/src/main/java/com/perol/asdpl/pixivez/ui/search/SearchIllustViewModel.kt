/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
 * Copyright (c) 2019 Perol_Notsfsssf
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

package com.perol.asdpl.pixivez.ui.search

import androidx.lifecycle.MutableLiveData
import com.perol.asdpl.pixivez.data.RetrofitRepository
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import com.perol.asdpl.pixivez.base.BaseViewModel
import java.util.Calendar

fun Calendar?.generateDateString(): String? {
    if (this == null) {
        return this
    }
    return "${this.get(Calendar.YEAR)}-${this.get(Calendar.MONTH) + 1}-${this.get(Calendar.DATE)}"
}

class SearchIllustViewModel : BaseViewModel() {
    private var sortT = arrayOf("date_desc", "date_asc", "popular_desc")
    private var searchTargetT =
        arrayOf("partial_match_for_tags", "exact_match_for_tags", "title_and_caption")
    var isPreview = false
    var illusts = MutableLiveData<List<Illust>?>()
    var illustsAdded = MutableLiveData<List<Illust>?>()
    private var retrofitRepository = RetrofitRepository.getInstance()
    var nextUrl = MutableLiveData<String?>()
    var bookmarkID = MutableLiveData<Long>()
    var isRefresh = MutableLiveData(false)
    var pre = PxEZApp.instance.pre
    val HIDE_BOOKMARK_ITEM_IN_SEARCH = "hide_bookmark_item_in_search2"
    var hideBookmarked = MutableLiveData(
        pre.getInt(HIDE_BOOKMARK_ITEM_IN_SEARCH, 0)
    )
    val sort = MutableLiveData(0)
    val searchTarget = MutableLiveData(0)
    val startDate = MutableLiveData<Calendar?>()
    val endDate = MutableLiveData<Calendar?>()
    fun setPreview(word: String, sort: String, search_target: String?, duration: String?) {
        isRefresh.value = true
        retrofitRepository.getSearchIllustPreview(word, sort, search_target, null, duration)
            .subscribeNext(illusts, nextUrl) {
                isRefresh.value = false
            }
    }

    fun firstSetData(word: String) {
        isRefresh.value = true
        //TODO: WTF?
        if ((startDate.value != null || endDate.value != null) &&
            (startDate.value != null && endDate.value != null) &&
            startDate.value!!.timeInMillis >= endDate.value!!.timeInMillis
        ) {
            startDate.value = null
            endDate.value = null
        }
        if (isPreview) {
            setPreview(word, sortT[sort.value!!], searchTargetT[searchTarget.value!!], null)
        } else {
            retrofitRepository.getSearchIllust(
                word,
                sortT[sort.value!!],
                searchTargetT[searchTarget.value!!],
                startDate.value.generateDateString(),
                endDate.value.generateDateString(),
                null
            ).subscribeNext(illusts, nextUrl, ::localSortByBookmarks) {
                isRefresh.value = false
            }
        }
    }

    //TODO: UI标识
    private fun localSortByBookmarks(it: List<Illust>): List<Illust> {
        return if (sort.value == 2) it.sortedByDescending { it.total_bookmarks } else it
    }

    fun onLoadMoreListen() {
        nextUrl.value?.let {
            retrofitRepository.getNextIllustRecommended(it)
                .subscribeNext(illustsAdded, nextUrl, ::localSortByBookmarks)
        }
    }
}
