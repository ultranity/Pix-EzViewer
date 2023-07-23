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

package com.perol.asdpl.pixivez.ui.user

import androidx.lifecycle.MutableLiveData
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.data.AppDataRepository
import com.perol.asdpl.pixivez.data.RetrofitRepository
import com.perol.asdpl.pixivez.data.model.BookMarkTagsResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlin.properties.Delegates

class BookMarkTagViewModel : BaseViewModel() {
    var id by Delegates.notNull<Long>()
    lateinit var pub:String
    val nextUrl = MutableLiveData<String?>()
    val tags = MutableLiveData<MutableList<BookMarkTagsResponse.BookmarkTagsBean>>()
    val tagsAdded = MutableLiveData<List<BookMarkTagsResponse.BookmarkTagsBean>?>()

    fun isSelfPage(): Boolean {
        return AppDataRepository.currentUser.userid == id
    }

    fun first(id: Long, pub: String) {
        this.pub = pub
        retrofit.getIllustBookmarkTags(id, pub).subscribe({
            tags.value = it.bookmark_tags
            nextUrl.value = it.next_url
        }, {}, {}).add()
    }

    fun onLoadMore() {
        RetrofitRepository.getInstance().getNextTags(nextUrl.value!!)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                tagsAdded.value = it.bookmark_tags
                nextUrl.value = it.next_url
            }, { tagsAdded.value = null }, {}).add()
    }
}
