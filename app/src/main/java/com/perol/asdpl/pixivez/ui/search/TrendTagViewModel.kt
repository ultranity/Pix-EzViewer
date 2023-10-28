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
import androidx.lifecycle.viewModelScope
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.base.KotlinUtil.asMutableList
import com.perol.asdpl.pixivez.data.HistoryDatabase
import com.perol.asdpl.pixivez.data.model.TrendTagsBean
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrendTagViewModel : BaseViewModel() {
    private var historyDatabase = HistoryDatabase.getInstance(PxEZApp.instance)
    val searchHistory = MutableLiveData<MutableList<String>>()
    val trendTags = MutableLiveData<MutableList<TrendTagsBean>?>()

    init {
        reloadSearchHistory()
    }

    fun getIllustTrendTags() = viewModelScope.launch {
        trendTags.value = try{
            retrofit.api.getIllustTrendTags().trend_tags
        } catch (e:Exception){
            null
        }
    }

    fun addHistory(keyword: String) {
        CoroutineScope(Dispatchers.IO).launch {
            historyDatabase.searchHistoryDao().insert(keyword)
            searchHistory.value?.add(0, keyword)
        }
    }

    fun clearHistory() {
        CoroutineScope(Dispatchers.IO).launch { historyDatabase.searchHistoryDao().clear() }
    }

    private fun reloadSearchHistory() {
        viewModelScope.launch {
            val history = historyDatabase.searchHistoryDao().getSearchHistory()
                .asReversed().map { it.word }.asMutableList()
            withContext(Dispatchers.Main) {
                searchHistory.value = history
            }
        }
    }

    fun deleteHistory(word: String) = CoroutineScope(Dispatchers.IO).launch {
        historyDatabase.searchHistoryDao().deleteHistory(word)
    }
}
