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
import androidx.lifecycle.viewModelScope
import com.perol.asdpl.pixivez.data.RetrofitRepository
import com.perol.asdpl.pixivez.data.model.INext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel(), LifecycleObserver {
    val retrofit = RetrofitRepository.getInstance()
    fun launchUI(block: suspend CoroutineScope.() -> Unit) {
        try {
            viewModelScope.launch(Dispatchers.Main) {
                block()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*protected fun <T : IIllustNext> Observable<T>.subscribeNext(
        target: MutableLiveData<MutableList<Illust>?>,
        nextUrl: MutableLiveData<String?>,
        illustTransform: (MutableList<Illust>) -> MutableList<Illust> = { it },
        onError: ((Throwable) -> Unit)? = null,
        onCompleted: (() -> Unit)? = null
    ) {
        this.observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io()).subscribe({ //Next
            target.value = illustTransform(it.illusts)
            nextUrl.value = it.next_url
        }, { //Error
            target.value = null
            it.printStackTrace()
            onError?.invoke(it)
        }, { //Completed
            onCompleted?.invoke()
        }).add()
    }*/

    protected fun <T : INext<MutableList<R>>, R> subscribeNext(
        getter: suspend () -> T,
        target: MutableLiveData<MutableList<R>?>,
        nextUrl: MutableLiveData<String?>?,
        transform: (MutableList<R>) -> MutableList<R> = { it },
        onError: ((Throwable) -> Unit)? = null,
        onCompleted: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            try { //Next
                val it = getter()
                target.value = transform(it.data())
                nextUrl?.value = it.next_url
            } catch (e: Exception) { //Error
                target.value = null
                e.printStackTrace()
                onError?.invoke(e)
            } finally {
                //Completed
                onCompleted?.invoke()
            }
        }
    }

    /*protected fun Observable<SearchUserResponse>.subscribeNext(
        target: MutableLiveData<MutableList<UserPreviewsBean>?>,
        nextUrl: MutableLiveData<String?>,
        onError: ((Throwable) -> Unit)? = null,
        onCompleted: (() -> Unit)? = null
    ) {
        this.subscribe({ //Next
            target.value = it.user_previews
            nextUrl.value = it.next_url
        }, { //Error
            target.value = null
            it.printStackTrace()
            onError?.invoke(it)
        }, { //Completed
            onCompleted?.invoke()
        }).add()
    }*/
}
