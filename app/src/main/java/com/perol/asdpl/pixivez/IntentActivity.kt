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

package com.perol.asdpl.pixivez

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.data.model.ErrorResponse
import com.perol.asdpl.pixivez.networks.Pkce
import com.perol.asdpl.pixivez.networks.RestClient
import com.perol.asdpl.pixivez.networks.ServiceFactory.gson
import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.OAuthSecureService
import com.perol.asdpl.pixivez.ui.MainActivity
import com.perol.asdpl.pixivez.ui.WebViewActivity
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class IntentActivity : RinkActivity() {
    companion object {
        fun start(context: Context, string: String) {
            val intent = Intent(context, IntentActivity::class.java).setAction("intent.action")
            intent.data = Uri.parse(string)
            context.startActivity(intent)
        }

        fun start(context: Context, uri: Uri) {
            val intent = Intent(context, IntentActivity::class.java).setAction("intent.action")
            intent.data = uri
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                            AppDataRepo.pre.setString("last_login_code", code)
                            tryLogin(code)
                            finish()
                            return
                        } else if (host.contains("users")) {
                            try {
                                UserMActivity.start(this, segment[0].toInt())
                                finish()
                                return
                            } catch (e: Exception) {
                                Toasty.error(this, R.string.wrong_id).show()
                            }
                        } else if (host.contains("illusts")) {
                            try {
                                PictureActivity.start(this, segment[0].toInt())
                                finish()
                                return
                            } catch (e: Exception) {
                                Toasty.error(this, R.string.wrong_id).show()
                            }
                        }
                    }
                }
                // else if (scheme.contains("pixez")) {
                // TODO: add pixez
                // }
            }
            if (uri.host?.equals("pixiv.me") == true) {
                val i = Intent(this, WebViewActivity::class.java).setAction("view.pixiv.me")
                intent.putExtra("url", uri)
                startActivity(i)
            }
            if (uri.path?.contains("artworks") == true) {
                val id = segment[segment.size - 1]
                try {
                    PictureActivity.start(this, id.toInt())
                    finish()
                    return
                } catch (e: Exception) {
                    Toasty.error(this, R.string.wrong_id).show()
                }
                return
            }
            // en/user/xxxx
            if (segment.size == 2 || segment.size == 3) {
                if ((segment[segment.size - 2] == "users") or (segment[segment.size - 2] == "u")) {
                    val id = segment[segment.size - 1]
                    try {
                        UserMActivity.start(this, id.toInt())
                        finish()
                        return
                    } catch (e: Exception) {
                        Toasty.error(this, R.string.wrong_id).show()
                    }
                }
                if (segment[segment.size - 2] == "i") {
                    val id = segment[segment.size - 1]
                    try {
                        PictureActivity.start(this, id.toInt())
                        finish()
                        return
                    } catch (e: Exception) {
                        Toasty.error(this, R.string.wrong_id).show()
                    }
                }
            }
            uri.getQueryParameter("illust_id")?.let {
                try {
                    PictureActivity.start(this, it.toInt())
                    finish()
                    return
                } catch (e: Exception) {
                    Toasty.error(this, R.string.wrong_id).show()
                }
            }
            uri.getQueryParameter("id")?.let {
                try {
                    UserMActivity.start(this, it.toInt())
                    finish()
                    return
                } catch (e: Exception) {
                    Toasty.error(this, R.string.wrong_id).show()
                }
            }
            if (uri.encodedSchemeSpecificPart.contains("/fanbox/creator/")) {
                val index = uri.pathSegments.indexOf("creator") + 1
                val targetString = uri.pathSegments[index]
                targetString.toIntOrNull()?.let {
                    try {
                        UserMActivity.start(this, it)
                        finish()
                    } catch (e: Exception) {
                        Toasty.error(this, R.string.wrong_id).show()
                    }
                }
            }
        }
    }

    private fun tryLogin(code: String) {
        val oAuthSecureService =
            RestClient.retrofitOauthSecureDirect.create(OAuthSecureService::class.java)
        CoroutineScope(Dispatchers.Default).launch {
            Toasty.warning(applicationContext, R.string.try_to_login).show()
            try {
                oAuthSecureService.postAuthTokenX(code, Pkce.getPkce().verify).let {
                    val user = it.user.toUserEntity(it.refresh_token, it.access_token)
                    AppDataRepo.insertUser(user)
                }
                Toasty.success(applicationContext, getString(R.string.login_success)).show()
                val intent = Intent(this@IntentActivity, MainActivity::class.java)
                    .setAction("login.success").apply {
                        // 避免循环添加账号导致相同页面嵌套。或者在添加账号（登录）成功时回到账号列表页面而不是导航至新的主页
                        flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        // Or launchMode = "singleTop|singleTask"
                    }
                startActivity(intent)
            } catch (e: Exception) {
                if (e is HttpException) {
                    try {
                        val errorBody = e.response()?.errorBody()?.string()!!
                        val errorResponse = gson.decodeFromString<ErrorResponse>(errorBody)
                        var errMsg = "${e.message}\n${errorResponse.errors.system.message}"
                        CrashHandler.instance.e(className, errMsg)
                        errMsg =
                            if (errorResponse.has_error && errorResponse.errors.system.message.contains(
                                    Regex(""".*103:.*""")
                                )
                            ) {
                                getString(R.string.error_invalid_account_password)
                            } else {
                                getString(R.string.error_unknown) + "\n" + errMsg
                            }

                        Toasty.error(applicationContext, errMsg).show()
                    } catch (e1: IOException) {
                        Toasty.error(applicationContext, e.message.toString()).show()
                    }
                } else {
                    Toasty.error(applicationContext, "${e.message}").show()
                }
            }
        }
    }
}
