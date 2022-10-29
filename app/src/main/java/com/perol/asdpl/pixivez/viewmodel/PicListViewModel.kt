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
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.responses.IllustNext
import io.reactivex.Observable

enum class RESTRICT_TYPE{
    all,
    public,
    private,
}

/**
 *  check if value update (only then trigger observer)
 */
fun <T> MutableLiveData<T>.checkUpdate(value:T): Boolean {
    return if (this.value !=value) {
        this.value = value
        true
    } else false
}
class PicListViewModel : BaseViewModel() {
    val data = MutableLiveData<List<Illust>?>()
    val adddata = MutableLiveData<List<Illust>?>()
    val nextUrl = MutableLiveData<String>()
    val isRefreshing = MutableLiveData(false)
    val restrict = MutableLiveData(RESTRICT_TYPE.all)
    lateinit var onLoadFirstRx :()->Observable<IllustNext>

    fun setonLoadFirstRx(mode:String) {
        onLoadFirstRx = when (mode) {
            "Recommend" -> { { retrofit.getRecommend().map{ IllustNext(it.illusts, it.next_url) } }}
            "Rank" -> { { retrofit.getRecommend().map{ IllustNext(it.illusts, it.next_url) } } }
            "MyFollow" -> { { retrofit.getFollowIllusts(restrict.value!!.name) } }
            "UserIllust" -> { { retrofit.getRecommend().map{ IllustNext(it.illusts, it.next_url) } }}
            "UserBookmark" -> { { retrofit.getRecommend().map{ IllustNext(it.illusts, it.next_url) } }}
            else -> { { retrofit.getRecommend().map{ IllustNext(it.illusts, it.next_url) } } }
        }
    }

    fun onLoadFirst() {
        isRefreshing.value = true
        onLoadFirstRx().subscribe({
            data.value = it.illusts
            nextUrl.value = it.next_url
        }, {
            data.value = null
        }, {
            isRefreshing.value = false
        }).add()
    }

    fun onLoadMoreRx(nextUrl: String): Observable<IllustNext> = retrofit.getNext(nextUrl)
    fun onLoadMore() {
        if (nextUrl.value != null) {
            retrofit.getNextUserIllusts(nextUrl.value!!).subscribe({
                adddata.value = it.illusts
                nextUrl.value = it.next_url
            }, {
               adddata.value = null
            }, {}).add()
        }
    }

}
