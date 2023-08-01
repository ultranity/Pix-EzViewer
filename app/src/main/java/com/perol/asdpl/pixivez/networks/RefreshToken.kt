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

package com.perol.asdpl.pixivez.networks

import android.util.Log
import android.widget.Toast
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.OAuthSecureService
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RefreshToken{
    companion object {

        @Volatile
        private var instance: RefreshToken? = null

        fun getInstance(): RefreshToken =
            instance ?: synchronized(this) {
                instance ?: RefreshToken().also { instance = it }
            }
    }
    private var oAuthSecureService: OAuthSecureService =
            RestClient.retrofitOauthSecure.create(OAuthSecureService::class.java)

    fun refreshToken(block:(() -> Unit)?=null) {
        MainScope().launch {
            Toasty.info(
                PxEZApp.instance,
                "refreshToken",
                Toast.LENGTH_SHORT
            ).show()
            refreshToken(AppDataRepo.currentUser.Refresh_token)
            block?.invoke()
        }
    }
    private var lastRefresh = 0L
    private val mutex: Mutex = Mutex()
    suspend fun refreshToken(
        refreshToken: String,
        newToken: Boolean = false
    ) = mutex.withLock {
        if (System.currentTimeMillis()- lastRefresh<1000){
            return@withLock
        }
        try {
            withContext(Dispatchers.IO){
                val it = oAuthSecureService.postRefreshAuthTokenX(refreshToken)

                Log.d(
                    "refreshToken",
                    "refresh ${it.access_token} and original ${AppDataRepo.currentUser.Authorization}"
                )

                val userEntity = it.user.toUserEntity(refreshToken, it.access_token)
                if (newToken) {
                    AppDataRepo.insertUser(userEntity)
                } else {
                    userEntity.Id = AppDataRepo.currentUser.Id
                    AppDataRepo.updateUser(userEntity)
                }
            }
            lastRefresh = System.currentTimeMillis()
            AppDataRepo.pre.setLong("lastRefresh", lastRefresh)
            Toasty.info(PxEZApp.instance, PxEZApp.instance.getString(R.string.refresh_token))
                .show()
            Log.d("init", "refreshToken end")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("RefreshToken", e.message.toString(), e)
            Toasty.info(
                PxEZApp.instance,
                PxEZApp.instance.getString(R.string.refresh_token_fail) + ":" + e.message,
            ).show()
            throw e
        }
    }
}
