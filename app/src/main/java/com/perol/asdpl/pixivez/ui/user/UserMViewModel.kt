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
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.data.model.UserDetailResponse
import io.reactivex.Observable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import java.io.File

class UserMViewModel : BaseViewModel() {
    val userDetail = MutableLiveData<UserDetailResponse>()
    val isfollow = MutableLiveData<Boolean>()
    val currentTab = MutableLiveData(0)

    fun getData(userid: Long) {
        retrofit.getUserDetail(userid).subscribe({
            userDetail.value = it
            isfollow.value = it.user.is_followed
        }, {
            it.printStackTrace()
        }, {}).add()
    }

    fun onFabClick(userid: Long) {
        if (isfollow.value!!) {
            retrofit.postUnfollowUser(userid).subscribe({
                isfollow.value = false
            }, {
                it.printStackTrace()
            }, {}).add()
        } else {
            retrofit.postFollowUser(userid, "public").subscribe({
                isfollow.value = true
            }, {
                it.printStackTrace()
            }, {}).add()
        }
    }

    fun onFabLongClick(userid: Long) {
        if (isfollow.value!!) {
            retrofit.postUnfollowUser(userid).subscribe({
                isfollow.value = false
            }, {}, {}).add()
        } else {
            retrofit.postFollowUser(userid, "private").subscribe({
                isfollow.value = true
            }, {}, {}).add()
        }
    }

    fun isSelfPage(id: Long): Boolean {
        return AppDataRepo.currentUser.userid == id
    }

    fun tryToChangeProfile(path: String): Observable<ResponseBody> {
        val file = File(path)
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        val body = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        builder.addFormDataPart("profile_image", file.name, body)
        return retrofit.postUserProfileEdit(builder.build().part(0))
    }
}
