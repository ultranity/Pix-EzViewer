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
import com.perol.asdpl.pixivez.data.model.BookmarkTagsBean
import kotlin.properties.Delegates

class BookMarkTagViewModel : BaseViewModel() {
    var noFirst: Boolean = true
    var id by Delegates.notNull<Int>()
    var pub: String = "public"
    val nextUrl = MutableLiveData<String?>()
    val tags = MutableLiveData<MutableList<BookmarkTagsBean>>()
    val tagsAdded = MutableLiveData<MutableList<BookmarkTagsBean>?>()

    fun first(id: Int, pub: String) {
        noFirst = false
        this.pub = pub
        subscribeNext({ retrofit.api.getIllustBookmarkTags(id, pub) }, tagsAdded, nextUrl)
    }

    fun onLoadMore() {
        subscribeNext({ retrofit.getNextTags(nextUrl.value!!) },tagsAdded, nextUrl)
    }
}
