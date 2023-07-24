/*
 * MIT License
 *
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

package com.perol.asdpl.pixivez.base

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.perol.asdpl.pixivez.data.RetrofitRepository
import com.perol.asdpl.pixivez.data.model.IIllustNext
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.data.model.SearchUserResponse
import com.perol.asdpl.pixivez.data.model.UserPreviewsBean
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel(), LifecycleObserver {
    val retrofit = RetrofitRepository.getInstance()
    val disposables = CompositeDisposable()
    fun launchUI(block: suspend CoroutineScope.() -> Unit) {
        try {
            MainScope().launch(Dispatchers.Main) {
                block()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected fun <T : IIllustNext> Observable<T>.subscribeNext(
        target: MutableLiveData<MutableList<Illust>?>,
        nextUrl: MutableLiveData<String?>,
        illustTransform: (MutableList<Illust>) -> MutableList<Illust> = { it },
        onError: (Throwable) -> Unit = {},
        onCompleted: () -> Unit = {}
    ) {
        this.subscribe({ //Next
            target.value = illustTransform(it.illusts)
            nextUrl.value = it.next_url
        }, { //Error
            target.value = null
            it.printStackTrace()
            onError(it)
        }, { //Completed
            onCompleted()
        }).add()
    }

    protected fun Observable<SearchUserResponse>.subscribeNext(
        target: MutableLiveData<List<UserPreviewsBean>?>,
        nextUrl: MutableLiveData<String?>,
        onError: (Throwable) -> Unit = {},
        onCompleted: () -> Unit = {}
    ) {
        this.subscribe({ //Next
            target.value = it.user_previews
            nextUrl.value = it.next_url
        }, { //Error
            target.value = null
            it.printStackTrace()
            onError(it)
        }, { //Completed
            onCompleted()
        }).add()
    }

    fun Disposable.add() {
        disposables.add(this)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
