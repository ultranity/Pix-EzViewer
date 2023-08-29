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
import androidx.lifecycle.viewModelScope
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.data.HistoryDatabase
import com.perol.asdpl.pixivez.data.model.User
import com.perol.asdpl.pixivez.data.model.UserDetailResponse
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UserMViewModel : BaseViewModel() {
    val userDetail = MutableLiveData<UserDetailResponse>()
    val isfollow = MutableLiveData<Boolean>()
    val currentTab = MutableLiveData(0)

    fun getData(userid: Int) {
        viewModelScope.launch {
            retrofit.api.getUserDetail(userid).let {
                userDetail.value = it
                isfollow.value = it.user.is_followed
            }
        }
    }

    fun onFabClick(userid: Int) {
        MainScope().launch {
            (if (isfollow.value!!)
                retrofit.api.postUnfollowUser(userid)
            else
                retrofit.api.postFollowUser(userid, "public")
                    ).let {
                    isfollow.value = !isfollow.value!!
                }
        }
    }

    fun onFabLongClick(userid: Int) {
        viewModelScope.launch {
            if (isfollow.value!!) {
                retrofit.api.postUnfollowUser(userid).let {
                    isfollow.value = false
                }
            } else {
                retrofit.api.postFollowUser(userid, "private").let {
                    isfollow.value = true
                }
            }
        }
    }

    fun tryToChangeProfile(path: String) {
        viewModelScope.launch {
            try {
                val file = File(path)
                val builder = MultipartBody.Builder()
                builder.setType(MultipartBody.FORM)
                val body = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                builder.addFormDataPart("profile_image", file.name, body)
                retrofit.api.postUserProfileEdit(builder.build().part(0))
                    .let {
                        Toasty.info(PxEZApp.instance, R.string.upload_success).show()
                    }
            } catch (e: Exception) {
                Toasty.info(PxEZApp.instance, R.string.update_failed).show()
            }
        }
    }

    fun insertHistory(user: User) {
        val historyDatabase = HistoryDatabase.getInstance(PxEZApp.instance)
        CoroutineScope(Dispatchers.IO).launch {
            val ee = historyDatabase.viewHistoryDao().getEntity(user.id, true)
            if (ee != null) {
                //ee.thumb = user.profile_image_urls.medium
                //ee.title = user.name
                historyDatabase.viewHistoryDao().increment(ee)
            } else
                historyDatabase.viewHistoryDao()
                    .insert(user.id, user.name, user.profile_image_urls.medium, true)
        }
    }
}
