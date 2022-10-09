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
import com.perol.asdpl.pixivez.repository.AppDataRepository
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.UserDetailResponse
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import java.io.File

class UserMViewModel : BaseViewModel() {
    var retrofitRepository = RetrofitRepository.getInstance()
    var userDetail = MutableLiveData<UserDetailResponse>()
    var isfollow = MutableLiveData<Boolean>()
    var hideBookmarked = MutableLiveData(0)
    var hideDownloaded = MutableLiveData(false)

    fun getData(userid: Long) {
        retrofitRepository.getUserDetail(userid).subscribe({
            userDetail.value = it
            isfollow.value = it.user.isIs_followed
        }, {

        }, {}).add()
    }

    fun onFabclick(userid: Long) {
        if (isfollow.value!!) {
            retrofitRepository.postUnfollowUser(userid).subscribe({
                isfollow.value = false
            }, {}, {}).add()
        } else {
            retrofitRepository.postFollowUser(userid, "public").subscribe({
                isfollow.value = true
            }, {}, {}).add()
        }
    }

    fun onFabLongClick(userid: Long) {
        if (isfollow.value!!)
            retrofitRepository.postUnfollowUser(userid).subscribe({
                isfollow.value = false
            }, {}, {}).add()
        else retrofitRepository.postFollowUser(userid, "private").subscribe({
            isfollow.value = true
        }, {}, {}).add()
    }

    fun isuser(id: Long) = Single.create<Boolean> {
        launchUI {
            val pt = AppDataRepository.getUser()
            it.onSuccess(id == pt.userid)
        }

    }

    fun tryToChangeProfile(path: String): Observable<ResponseBody> {

        val file = File(path)
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        val body = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        builder.addFormDataPart("profile_image", file.name, body)
        return retrofitRepository.postUserProfileEdit(builder.build().part(0))
    }

}