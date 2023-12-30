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
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.data.model.SearchUserResponse
import com.perol.asdpl.pixivez.data.model.UserPreviewsBean
import kotlin.properties.Delegates

class UserListViewModel : BaseViewModel() {
    var needHeader: Boolean = false
    private lateinit var args: MutableMap<String, Any?>
    val data = MutableLiveData<MutableList<UserPreviewsBean>?>()
    val dataAdded = MutableLiveData<MutableList<UserPreviewsBean>?>()
    val nextUrl = MutableLiveData<String?>()
    val isRefreshing = DMutableLiveData(false)
    var restrict = DMutableLiveData("public")
    private lateinit var onLoadFirstRx: suspend () -> SearchUserResponse

    fun setonLoadFirstRx(mode: String, extraArgs: MutableMap<String, Any?>? = null) {
        if (extraArgs != null) {
            this.args = extraArgs
        }
        if (mode == "Following") needHeader = AppDataRepo.isSelfPage(userid)
        onLoadFirstRx = when (mode) {
            "Following" -> {
                { retrofit.api.getUserFollowing(userid, restrict.value) }
            }

            "Follower" -> {
                { retrofit.api.getUserFollower(userid) }
            }

            "Related" -> {
                { retrofit.api.getUserRelated(userid) }
            }

            "Search" -> {
                { retrofit.api.getSearchUser(keyword) }
            }

            "MyPixiv" -> {
                { retrofit.api.getMyPixivFriend(userid) }
            }

            else -> {
                { retrofit.api.getUserRecommended() }
            } //"Recommend"
        }
    }

    // -----------------
    lateinit var keyword: String

    // -----------------
    var userid by Delegates.notNull<Int>()
    // -----------------
    fun onLoadMore() {
        subscribeNext({ retrofit.getNextSearchUser(nextUrl.value!!) }, dataAdded, nextUrl)
    }

    fun onLoadFirst(){
        isRefreshing.value = true
        subscribeNext(onLoadFirstRx, data, nextUrl){ isRefreshing.value = false }
    }
}
