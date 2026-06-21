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
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.objects.ToastQ
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.OAuthSecureService
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

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
            Toasty.info(PxEZApp.instance, "refreshToken")
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
            Toasty.tokenRefreshed()
            Log.d("init", "refreshToken end")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("RefreshToken", e.message.toString(), e)
            // 421 Misdirected Request:入口 IP / SNI 模式不对(非凭据失效)。吞掉不抛出 ——
            // 否则刷新身份失败的异常会被 CrashHandler 当未捕获处理而退出进程,造成"进不去
            // 设置改网络模式"的死锁。提示用户去设置切换后重试。
            if (e is HttpException && e.code() == 421) {
                ToastQ.post("421:连接被拒,请到 设置→连接设置 切换 DNS / SNI 模式后重试")
                return@withLock
            }
            Toasty.tokenRefreshed(e)
            throw e
        }
    }
}
