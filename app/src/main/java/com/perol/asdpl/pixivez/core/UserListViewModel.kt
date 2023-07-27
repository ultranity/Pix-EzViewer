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

package com.perol.asdpl.pixivez.core

import androidx.lifecycle.MutableLiveData
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.base.DMutableLiveData
import com.perol.asdpl.pixivez.data.model.SearchUserResponse
import com.perol.asdpl.pixivez.data.model.UserPreviewsBean
import io.reactivex.Observable
import kotlin.properties.Delegates

class UserListViewModel : BaseViewModel() {
    var needHeader: Boolean = false
    private lateinit var args: MutableMap<String, Any?>
    val data = MutableLiveData<List<UserPreviewsBean>?>()
    val dataAdded = MutableLiveData<List<UserPreviewsBean>?>()
    val nextUrl = MutableLiveData<String?>()
    val isRefreshing = DMutableLiveData(false)
    var restrict = DMutableLiveData("public")
    protected lateinit var onLoadFirstRx: () -> Observable<SearchUserResponse>

    open fun setonLoadFirstRx(mode: String, extraArgs: MutableMap<String, Any?>? = null) {
        if (extraArgs != null) {
            this.args = extraArgs
        }
        onLoadFirstRx = when (mode) {
            "Following" -> {
                needHeader = true
                {
                    retrofit.getUserFollowing(userid, restrict.value!!)
                }
            }
            "Follower" -> {
                { retrofit.getUserFollower(userid) }
            }
            "Search" -> {
                { retrofit.getSearchUser(keyword) }
            }
            else -> { //"Recommend"
                { retrofit.getUserRecommended() }
            }
        }
    }

    // -----------------
    lateinit var keyword:String
    // -----------------
    var userid by Delegates.notNull<Long>()
    // -----------------
    fun onLoadMore() {
        retrofit.getNextSearchUser(nextUrl.value!!).subscribeNext(dataAdded, nextUrl)
    }

    fun onLoadFirst(){
        isRefreshing.value = true
        onLoadFirstRx().subscribeNext(data, nextUrl){ isRefreshing.value = false }
    }
}
