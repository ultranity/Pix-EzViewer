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
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.data.AppDatabase
import com.perol.asdpl.pixivez.data.entity.SearchHistoryEntity
import com.perol.asdpl.pixivez.data.model.TrendingtagResponse
import com.perol.asdpl.pixivez.base.BaseViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrendTagViewModel : BaseViewModel() {
    private var appDatabase = AppDatabase.getInstance(PxEZApp.instance)
    val searchHistory = MutableLiveData<MutableList<String>>()
    val trendTags = MutableLiveData<MutableList<TrendingtagResponse.TrendTagsBean>?>()

    init {
        reloadSearchHistory()
    }

    fun getIllustTrendTags() = retrofit.getIllustTrendTags().subscribe{
        trendTags.value = it?.trend_tags
    }.add()

    fun addhistory(keyword: String) {
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.searchhistoryDao().insert(SearchHistoryEntity(keyword))
            searchHistory.value?.add(0, keyword)
        }
    }

    fun clearHistory() {
        Observable.create<Int> {
            appDatabase.searchhistoryDao().deletehistory()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({}, { }, {}).add()
    }

    private fun reloadSearchHistory() {
        CoroutineScope(Dispatchers.IO).launch {
            val history = appDatabase.searchhistoryDao().getSearchHistory()
                .asReversed().map { it.word }.toMutableList()
            withContext(Dispatchers.Main) {
                searchHistory.value = history
            }
        }
    }

    fun deleteHistory(word: String) = CoroutineScope(Dispatchers.IO).launch {
        appDatabase.searchhistoryDao().deleteHistory(word)
    }
}
