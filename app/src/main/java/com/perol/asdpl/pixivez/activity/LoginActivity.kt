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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.databinding.ActivityLoginBinding
import com.perol.asdpl.pixivez.dialog.FirstInfoDialog
import com.perol.asdpl.pixivez.networks.Pkce
import com.perol.asdpl.pixivez.networks.RestClient
import com.perol.asdpl.pixivez.networks.SharedPreferencesServices
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.repository.AppDataRepository
import com.perol.asdpl.pixivez.responses.ErrorResponse
import com.perol.asdpl.pixivez.responses.PixivOAuthResponse
import com.perol.asdpl.pixivez.services.OAuthSecureService
import com.perol.asdpl.pixivez.sql.UserEntity
import io.noties.markwon.Markwon
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.*

class LoginActivity : RinkActivity() {
    private var username: String? = null
    private var password: String? = null
    lateinit var sharedPreferencesServices: SharedPreferencesServices
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//        window.decorView.systemUiVisibility =
//            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//        window.statusBarColor = Color.TRANSPARENT

        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        initBind()
        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            Log.e(className,"onRxJavaErrorHandler ---->: $it")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initBind() {
        sharedPreferencesServices = SharedPreferencesServices.getInstance()
        try {

            if (sharedPreferencesServices.getString("password") != null) {
                binding.editPassword.setText(sharedPreferencesServices.getString("password"))
                binding.editUsername.setText(sharedPreferencesServices.getString("username"))
            }
            if (!sharedPreferencesServices.getBoolean("firstinfo")) {
                FirstInfoDialog().show(this.supportFragmentManager, "infodialog")
            }
        } catch (e: Exception) {

        }
        binding.textviewHelp.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
            val view = layoutInflater.inflate(R.layout.new_dialog_user_help, null)
            val webView = view.findViewById(R.id.web_user_help) as TextView
            // obtain an instance of Markwon
            val markwon = Markwon.create(this)

            val node = markwon.parse(getString(R.string.login_help_md))

            val markdown = markwon.render(node)

            // use it on a TextView
            markwon.setParsedMarkdown(webView, markdown)
            builder.setPositiveButton(android.R.string.ok) { _, _ ->

            }
            builder.setView(view)
            builder.create().show()
        }
        /*binding.editUsername.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Ignore.
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Ignore.
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.accountTextInputLayout.isErrorEnabled = false
            }
        })
        binding.editPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Ignore.
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Ignore.
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.passwordTextInputLayout.isErrorEnabled = false
            }
        })
*/
        binding.loginBtn.setOnClickListener {
/*
            username = binding.editUsername.text.toString().trim()
            password = binding.editPassword.text.toString()

            if (username.isNullOrBlank()) binding.accountTextInputLayout.error =
                getString(R.string.error_blank_account)

            if (password.isNullOrBlank()) binding.passwordTextInputLayout.error =
                getString(R.string.error_blank_password)

            if (username.isNullOrBlank() || password.isNullOrBlank()) {
                return@setOnClickListener
            }
            binding.loginBtn.isEnabled = false*/
            //sharedPreferencesServices.setString("username", username)
            //sharedPreferencesServices.setString("password", password)
            MaterialDialog(this).show {
                title(R.string.login_help)
                message(R.string.login_help_new)
                positiveButton(R.string.I_know) {
                    val url: String = "https://app-api.pixiv.net/web/v1/login?code_challenge=" +
                            Pkce.getPkce().challenge + "&code_challenge_method=S256&client=pixiv-android"
                    //WeissUtil.start();
                    //WeissUtil.proxy();
                    val intent = Intent(this@LoginActivity, WebViewActivity::class.java)
                    intent.putExtra("url", url)
                    startActivity(intent)
                }
            }
        }

        binding.loginBtn.setOnClickListener {
            binding.loginBtn.isEnabled = false

            val intent = Intent(this@LoginActivity, NewUserActivity::class.java)
            startActivityForResult(intent, 8080)
        }
/*        println(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this))
        if(BuildConfig.ISGOOGLEPLAY)
        if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)==ConnectionResult.SERVICE_MISSING){
            MaterialDialog(this).show {
                title(text="Google Play service丢失！")
                message(text = "你使用的是Google play的版本，请确保应用是从Google play安装的，而不是第三方打包（如QQ传应用，APKPURE，应用备份等等），因为这会导致play版本的诸多问题bug\n" +
                        "如果无法使用play service，请前往项目地址下载适合中国内陆地区更新的全量包版本")
                negativeButton(android.R.string.cancel)
                positiveButton(R.string.action_select)
                lifecycleOwner(this@LoginActivity)
            }
        }*/
    }
    fun showHelp(view: View) {
//        val intent = Intent(this@LoginActivity, NewUserActivity::class.java)
//        startActivity(intent)
        Toasty.info(this, this.resources.getString(R.string.registerclose), Toast.LENGTH_LONG)
            .show()
    }

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode != 8080) return
    val code = data!!.getStringExtra("code")!!

    val map = HashMap<String, Any>()
    map["client_id"] = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    map["client_secret"] = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    map["grant_type"] = "authorization_code"
    map["code"] = code
    map["code_verifier"] = Pkce.getPkce().verify
    map["redirect_uri"] = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback"
    map["include_policy"] = true

    val oAuthSecureService =
        RestClient.retrofitOauthSecure.create(OAuthSecureService::class.java)
    oAuthSecureService.postAuthToken(map).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(object : Observer<PixivOAuthResponse> {
            override fun onSubscribe(d: Disposable) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.try_to_login),
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onNext(pixivOAuthResponse: PixivOAuthResponse) {
                val user = pixivOAuthResponse.response.user
                GlobalScope.launch {
                    AppDataRepository.insertUser(
                        UserEntity(
                            user.profile_image_urls.px_170x170,
                            user.id.toLong(),
                            user.name,
                            user.mail_address,
                            user.isIs_premium,
                            "OAuth2",
                            pixivOAuthResponse.response.refresh_token,
                            "Bearer " + pixivOAuthResponse.response.access_token
                        )
                    )

                    sharedPreferencesServices.setBoolean("isnone", false)
                }
            }

            override fun onError(e: Throwable) {
                binding.loginBtn.isEnabled = true

                binding.textviewHelp.visibility = View.VISIBLE
                if (e is HttpException) {
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        val gson = Gson()
                        val errorResponse = gson.fromJson<ErrorResponse>(
                            errorBody,
                            ErrorResponse::class.java
                        )
                        var errMsg = "${e.message}\n${errorResponse.errors.system.message}"
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

            override fun onComplete() {
                Toast.makeText(applicationContext, getString(R.string.login_success), Toast.LENGTH_LONG).show()
                val intent = Intent(this@LoginActivity, HelloMActivity::class.java).apply {
                    // 避免循环添加账号导致相同页面嵌套。或者在添加账号（登录）成功时回到账号列表页面而不是导航至新的主页
                    flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK // Or launchMode = "singleTop|singleTask"
                }
                startActivity(intent)
            }
        })
}
}
