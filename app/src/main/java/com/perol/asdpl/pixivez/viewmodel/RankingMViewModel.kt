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

class RankingMViewModel : BaseViewModel() {
    val nextUrl = MutableLiveData<String>()
    val addillusts = MutableLiveData<ArrayList<Illust>?>()
    val illusts = MutableLiveData<ArrayList<Illust>??>()
    fun first(mode: String, picdata: String?) {
        retrofit.getIllustRanking(mode, picdata).subscribe({
            nextUrl.value = it.next_url
            illusts.value = ArrayList(it.illusts)
        }, { it.printStackTrace() }, {}).add()
    }

    fun onRefresh(mode: String, picdata: String?) {
        retrofit.getIllustRanking(mode, picdata).subscribe({
            nextUrl.value = it.next_url
            illusts.value = it.illusts as ArrayList<Illust>?
        }, {
            illusts.value = null
        }, {}).add()
    }

    fun onLoadMore() {
        retrofit.getNextIllustRecommended(nextUrl.value!!).subscribe({
            nextUrl.value = it.next_url
            addillusts.value = it.illusts
        }, {
            addillusts.value = null
        }, {}).add()
    }

    fun datePick(mode: String, pickDate: String?) {
        retrofit.getIllustRanking(mode, pickDate).subscribe({
            nextUrl.value = it.next_url
            illusts.value = ArrayList(it.illusts)
        }, {
            illusts.value = null
        }, {}).add()
    }
}
