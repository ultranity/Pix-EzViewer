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
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.sql.AppDatabase
import com.perol.asdpl.pixivez.sql.entity.SearchHistoryEntity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class TrendTagViewModel : BaseViewModel() {
    private var appDatabase = AppDatabase.getInstance(PxEZApp.instance)
    var searchhistroy = MutableLiveData<MutableList<String>>()
    var retrofitRepository: RetrofitRepository = RetrofitRepository.getInstance()

    init {
        reloadSearchHistory()
    }

    fun addhistory(searchword: String) {
        Observable.create<Int> {
            appDatabase.searchhistoryDao().insert(SearchHistoryEntity(searchword))
            reloadSearchHistory()
            it.onNext(1)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnError {
        }.subscribe {
        }.add()
    }

    fun sethis() {
        Observable.create<Int> {
            appDatabase.searchhistoryDao().deletehistory()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({}, { }, {}).add()
    }

    private fun reloadSearchHistory() {
        appDatabase.searchhistoryDao().getSearchHistory().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                searchhistroy.value = it.asReversed().map { it.word }.toMutableList()
            }, {}, {}).add()
    }

    fun removeTag() {
        appDatabase.searchhistoryDao().deletehistory()
    }

    fun getIllustTrendTags() = retrofitRepository.getIllustTrendTags()
    fun deleteHistory(word: String) = Observable.just(1).subscribeOn(Schedulers.io()).map {
        appDatabase.searchhistoryDao().deleteHistory(word)
    }.observeOn(AndroidSchedulers.mainThread()).subscribe()!!

    fun deleteHistoryEntity(searchHistoryEntity: SearchHistoryEntity): Observable<Int> = Observable.create<Int> {
        appDatabase.searchhistoryDao().deleteHistoryEntity(searchHistoryEntity)
        it.onNext(1)
    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}
