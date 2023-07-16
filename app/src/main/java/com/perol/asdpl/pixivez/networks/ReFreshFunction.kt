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
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.repository.AppDataRepository
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.services.AppApiPixivService
import com.perol.asdpl.pixivez.services.OAuthSecureService
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.sql.entity.UserEntity
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.pow

class ReFreshFunction private constructor() : Function<Observable<Throwable>, ObservableSource<*>> {
    private var client_id: String = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    private var client_secret: String = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    private val TOKEN_ERROR = "Error occurred at the OAuth process"
    private val TOKEN_ERROR_2 = "Invalid refresh token"
    private var oAuthSecureService: OAuthSecureService? = null
    private var i = 0
    private val maxRetries = 3
    private var retryCount = 0

    init {
        this.oAuthSecureService =
            RestClient.retrofitOauthSecure.create(OAuthSecureService::class.java)
    }

    @Throws(Exception::class)
    override fun apply(throwableObservable: Observable<Throwable>): ObservableSource<*> {
        return throwableObservable.flatMap(
            Function<Throwable, ObservableSource<*>> { throwable ->
                Log.d("reFreshFunction", throwable.message.toString())
                if (throwable is TimeoutException || throwable is SocketTimeoutException ||
                    throwable is ConnectException
                ) {
                    return@Function Observable.error<Any>(throwable)
                }
                else if (throwable is HttpException) {
                    if (throwable.response()!!.code() == 400) {
                        if (throwable.message().contains(TOKEN_ERROR)) {
                            Toasty.info(
                                PxEZApp.instance,
                                PxEZApp.instance.getString(R.string.token_expired),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        if (throwable.message().contains(TOKEN_ERROR_2)) {
                            Toasty.info(
                                PxEZApp.instance,
                                PxEZApp.instance.getString(R.string.login_expired),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Function Observable.error<Any>(throwable)
                        }
                        retryCount++
                        Log.d("init", "400 retryCount $retryCount refreshing $refreshing")
                        if (refreshing && retryCount <= maxRetries - 1) {
                            return@Function Observable.timer(
                                (2000 * (0.8).pow(retryCount)).toLong(),
                                TimeUnit.MILLISECONDS
                            )
                        }
                        else if (retryCount <= maxRetries) {
                            return@Function reFreshToken()
                        }
                        else {
                            retryCount = 0
                            return@Function Observable.error<Any>(throwable)
                        }
                    }
                    else if (throwable.response()!!.code() == 404) {
                        // if (i == 0) {
                        Log.d("d", throwable.response()!!.message())
                        Toasty.warning(
                            PxEZApp.instance,
                            "404 " + throwable.response()!!.message(),
                            Toast.LENGTH_SHORT
                        ).show()
                        //    i++
                        // }
                        return@Function Observable.error<Any>(throwable)
                    }
                }
                else if (throwable is SocketException) {
                    Toasty.warning(
                        PxEZApp.instance,
                        "连接状态异常 $throwable",
                        Toast.LENGTH_SHORT
                    ).show()
                    throwable.printStackTrace()
                    Log.e("SocketException", throwable.localizedMessage ?: throwable.toString())
                    RetrofitRepository.getInstance().appApiPixivService = RestClient.retrofitAppApi.create(AppApiPixivService::class.java)
                }
                return@Function Observable.error<Any>(throwable)
            }
        )
    }

    @Synchronized
    fun reFreshToken(): ObservableSource<*> {
        if (refreshing) {
            return Observable.timer(2000, TimeUnit.MILLISECONDS)
        }
        refreshing = true
        val user: UserEntity
        runBlocking {
            user = AppDataRepository.getUser()!!
            Toasty.info(
                PxEZApp.instance,
                "reFreshToken",
                Toast.LENGTH_SHORT
            ).show()
        }
        return reFreshToken(user.Refresh_token)
    }

    fun reFreshToken(refreshToken: String, newToken:Boolean=false): ObservableSource<*> {
        Log.d("init", "reFreshToken")
        // UserInfoSharedPreferences.getInstance().setString("Device_token", it.Device_token)
        return oAuthSecureService!!.postRefreshAuthTokenX(
            client_id,
            client_secret,
            "refresh_token",
            refreshToken,
            true
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .doOnNext { pixivOAuthResponse ->
                val user = pixivOAuthResponse.response.user
                val userEntity = UserEntity(
                    user.id,
                    user.name,
                    user.mail_address,
                    user.is_premium,
                    user.profile_image_urls.px_170x170,
                    "OAuth2", // pixivOAuthResponse.response.device_token,
                    pixivOAuthResponse.response.refresh_token,
                    "Bearer " + pixivOAuthResponse.response.access_token
                )
                runBlocking {
                    if(newToken){
                        AppDataRepository.insertUser(userEntity)
                    }
                    else {
                        userEntity.Id = AppDataRepository.currentUser.Id
                        AppDataRepository.updateUser(userEntity)
                    }
                    Toasty.info(
                        PxEZApp.instance,
                        PxEZApp.instance.getString(R.string.refresh_token),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("init", "reFreshToken end")

                    AppDataRepository.pre.setInt("user_x_restrict", user.x_restrict)
                    AppDataRepository.pre.setLong("lastRefresh", System.currentTimeMillis())
                }
            }.doOnError {
                it.printStackTrace()
                Toasty.info(
                    PxEZApp.instance,
                    PxEZApp.instance.getString(R.string.refresh_token_fail) + ":" + it.message,
                    Toast.LENGTH_SHORT
                ).show()
            }.delay(500, TimeUnit.MILLISECONDS)
            .doFinally {
                refreshing = false
            }
    }

    companion object {
        @Volatile
        var refreshing = false

        @Volatile
        private var instance: ReFreshFunction? = null

        fun getInstance(): ReFreshFunction =
            instance ?: synchronized(this) {
                instance ?: ReFreshFunction().also { instance = it }
            }
    }
}
