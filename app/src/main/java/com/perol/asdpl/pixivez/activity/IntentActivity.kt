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

package com.perol.asdpl.pixivez.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.networks.Pkce
import com.perol.asdpl.pixivez.networks.RestClient
import com.perol.asdpl.pixivez.networks.SharedPreferencesServices
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.repository.AppDataRepository
import com.perol.asdpl.pixivez.responses.ErrorResponse
import com.perol.asdpl.pixivez.responses.PixivOAuthResponse
import com.perol.asdpl.pixivez.services.OAuthSecureService
import com.perol.asdpl.pixivez.sql.entity.UserEntity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import java.io.IOException

class IntentActivity : RinkActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferencesServices = SharedPreferencesServices.getInstance()
        val uri = intent.data
        if (uri != null) {
            val scheme = uri.scheme
            val segment = uri.pathSegments
            if (scheme != null) {
                // pixiv://illusts/
                if (scheme.contains("pixiv")) {
                    val host = uri.host
                    if (!host.isNullOrBlank()) {
                        if (host.contains("account") && segment.contains("login")) {
                            val code = uri.getQueryParameter("code").toString()
                            sharedPreferencesServices.setString("last_login_code", code)
                            tryLogin(code)
                            finish()
                            return
                        } else if (host.contains("users")) {
                            try {
                                UserMActivity.start(this, segment[0].toLong())
                                finish()
                                return
                            } catch (e: Exception) {
                                Toasty.error(this, getString(R.string.wrong_id))
                            }
                        } else if (host.contains("illusts")) {
                            try {
                                PictureActivity.start(this, segment[0].toLong())
                                finish()
                                return
                            } catch (e: Exception) {
                                Toasty.error(this, getString(R.string.wrong_id))
                            }
                        }
                    }
                } else if (scheme.contains("pixez")) {
                    //TODO: add pixez
                }
            }
            if (uri.host?.equals("pixiv.me") == true) {
                val i = Intent(this, WebViewActivity::class.java)
                intent.putExtra("url", uri)
                startActivity(i)
            }
            if (uri.path?.contains("artworks") == true) {
                val id = segment[segment.size - 1]
                try {
                    PictureActivity.start(this, id.toLong())
                    finish()
                    return
                } catch (e: Exception) {
                    Toasty.error(this, getString(R.string.wrong_id))
                }
                return
            }
            //en/user/xxxx
            if (segment.size == 2 || segment.size == 3) {
                if ((segment[segment.size - 2] == "users") or (segment[segment.size - 2] == "u")) {
                    val id = segment[segment.size - 1]
                    try {
                        UserMActivity.start(this, id.toLong())
                        finish()
                        return
                    } catch (e: Exception) {
                        Toasty.error(this, getString(R.string.wrong_id))
                    }
                }
                if (segment[segment.size - 2] == "i") {
                    val id = segment[segment.size - 1]
                    try {
                        PictureActivity.start(this, id.toLong())
                        finish()
                        return
                    } catch (e: Exception) {
                        Toasty.error(this, getString(R.string.wrong_id))
                    }
                }
            }
            uri.getQueryParameter("illust_id")?.let {
                try {
                    PictureActivity.start(this, it.toLong())
                    finish()
                    return
                } catch (e: Exception) {
                    Toasty.error(this, getString(R.string.wrong_id))
                }
            }
            uri.getQueryParameter("id")?.let {
                try {
                    UserMActivity.start(this, it.toLong())
                    finish()
                    return
                } catch (e: Exception) {
                    Toasty.error(this, getString(R.string.wrong_id))
                }
            }
            if (uri.encodedSchemeSpecificPart.contains("/fanbox/creator/")) {
                val index = uri.pathSegments.indexOf("creator") + 1
                val targetString = uri.pathSegments[index]
                targetString.toLongOrNull()?.let {
                    try {
                        UserMActivity.start(this, it)
                        finish()
                    } catch (e: Exception) {
                        Toasty.error(this, getString(R.string.wrong_id))
                    }
                }

            }
        }
    }

    private fun tryLogin(code: String) {
        val map = HashMap<String, Any>()
        map["client_id"] = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
        map["client_secret"] = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
        map["grant_type"] = "authorization_code"
        map["code"] = code
        map["code_verifier"] = Pkce.getPkce().verify
        //map["username"] = username!!
        //map["password"] = password!!
        //map["device_token"] = SharedPreferencesServices.getInstance().getString("Device_token") ?: "pixiv"
        map["redirect_uri"] = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback"
        //map["get_secure_url"] = true
        map["include_policy"] = true
        val oAuthSecureService =
            RestClient.retrofitOauthSecureDirect.create(OAuthSecureService::class.java)
        oAuthSecureService.postAuthToken(map).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.try_to_login),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .doOnNext { pixivOAuthResponse: PixivOAuthResponse ->
                val user = pixivOAuthResponse.response.user
                runBlocking {
                    AppDataRepository.insertUser(
                        UserEntity(
                            user.profile_image_urls.px_170x170,
                            user.id.toLong(),
                            user.name,
                            user.mail_address,
                            user.is_premium,
                            code,//pixivOAuthResponse.response.device_token,
                            pixivOAuthResponse.response.refresh_token,
                            "Bearer " + pixivOAuthResponse.response.access_token
                        )
                    )
                    //sharedPreferencesServices.setBoolean("isnone", false)
                    //sharedPreferencesServices.setString("username", username)
                    //sharedPreferencesServices.setString("password", password)
                    //sharedPreferencesServices.setString("Device_token", pixivOAuthResponse.response.device_token)
                }
            }
            .doOnError { e ->
                if (e is HttpException) {
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        val gson = Gson()
                        val errorResponse = gson.fromJson(
                            errorBody,
                            ErrorResponse::class.java
                        )
                        var errMsg = "${e.message}\n${errorResponse.errors.system.message}"
                        Log.e(className, errMsg)
                        errMsg =
                            if (errorResponse.has_error && errorResponse.errors.system.message.contains(
                                    Regex(""".*103:.*""")
                                )
                            ) {
                                getString(R.string.error_invalid_account_password)
                            } else {
                                getString(R.string.error_unknown) + "\n" + errMsg
                            }

                        Toast.makeText(applicationContext, errMsg, Toast.LENGTH_LONG).show()
                    } catch (e1: IOException) {
                        Toast.makeText(
                            applicationContext,
                            "${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {
                    Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
            .doOnComplete {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.login_success),
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(this, HelloMActivity::class.java).apply {
                    // 避免循环添加账号导致相同页面嵌套。或者在添加账号（登录）成功时回到账号列表页面而不是导航至新的主页
                    flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK // Or launchMode = "singleTop|singleTask"
                }
                startActivity(intent)
            }
            .subscribe()
    }
}
