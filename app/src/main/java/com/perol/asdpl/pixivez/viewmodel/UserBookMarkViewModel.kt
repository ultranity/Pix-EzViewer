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

package com.perol.asdpl.pixivez.viewmodel

import androidx.lifecycle.MutableLiveData
import com.perol.asdpl.pixivez.repository.AppDataRepository
import com.perol.asdpl.pixivez.responses.BookMarkTagsResponse
import com.perol.asdpl.pixivez.responses.Illust
import kotlin.properties.Delegates

class UserBookMarkViewModel : BaseViewModel() {
    var id by Delegates.notNull<Long>()
    val data = MutableLiveData<List<Illust>?>()
    val dataAdded = MutableLiveData<List<Illust>?>()
    val nextUrl = MutableLiveData<String?>()
    val tags = MutableLiveData<BookMarkTagsResponse>()
    fun onLoadMoreListener() {
        if (nextUrl.value != null) {
            retrofit.getNextUserIllusts(nextUrl.value!!).subscribe({
                dataAdded.value = it.illusts
                nextUrl.value = it.next_url
            }, {dataAdded.value = null}, {}).add()
        }
    }

    fun isSelfPage(): Boolean {
        return AppDataRepository.currentUser.userid == id
    }

    fun onRefreshListener(id: Long, string: String, tag: String?) {
        retrofit.getLikeIllust(id, string, tag).subscribe({
            data.value = it.illusts
            nextUrl.value = it.next_url
        }, {data.value = null}, {}).add()
    }

    fun first(id: Long, string: String) {
        retrofit.getLikeIllust(id, string, null).subscribe({
            data.value = it.illusts
            nextUrl.value = it.next_url
        }, {data.value = null}, {}).add()
        retrofit.getIllustBookmarkTags(id, string).subscribe({
            tags.value = it
        }, {}, {}).add()
    }
}
