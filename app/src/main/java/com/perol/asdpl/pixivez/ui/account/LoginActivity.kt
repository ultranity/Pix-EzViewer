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

package com.perol.asdpl.pixivez.ui.account

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.ui.OKWebViewActivity
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivityLoginBinding
import com.perol.asdpl.pixivez.networks.Pkce
import com.perol.asdpl.pixivez.networks.ReFreshFunction
import com.perol.asdpl.pixivez.objects.InteractionUtil.add
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.data.UserInfoSharedPreferences
import com.perol.asdpl.pixivez.ui.HelloMActivity
import com.perol.asdpl.pixivez.ui.settings.FirstInfoDialog
import com.perol.asdpl.pixivez.ui.settings.SettingsActivity
import io.noties.markwon.Markwon
import io.reactivex.Observable

class LoginActivity : RinkActivity() {
    // private var username: String? = null
    // private var password: String? = null
    private lateinit var userInfoSharedPreferences: UserInfoSharedPreferences
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initBind() {
        userInfoSharedPreferences = UserInfoSharedPreferences.getInstance()
        if (!userInfoSharedPreferences.getBoolean("firstinfo")) {
            FirstInfoDialog().show(this.supportFragmentManager, "infoDialog")
        }
        /*try {
            if (userInfoSharedPreferences.getString("password") != null) {
                binding.editPassword.setText(userInfoSharedPreferences.getString("password"))
                binding.editUsername.setText(userInfoSharedPreferences.getString("username"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }*/
        binding.textviewHelp.setOnClickListener {
            // obtain an instance of Markwon
            val markwon = Markwon.create(this)
            MaterialAlertDialogBuilder(this)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                }
                .setMessage(markwon.toMarkdown(getString(R.string.login_help_md)))
                .create().show()
        }

        binding.loginBtn.setOnLongClickListener {
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
            // userInfoSharedPreferences.setString("username", username)
            // userInfoSharedPreferences.setString("password", password)
            val url: String = "https://app-api.pixiv.net/web/v1/login?code_challenge=" +
                    Pkce.getPkce().challenge + "&code_challenge_method=S256&client=pixiv-android"
            // WeissUtil.start()
            // WeissUtil.proxy()
            val intent = Intent(this@LoginActivity, OKWebViewActivity::class.java)
            intent.putExtra("url", url)
            startActivity(intent)
            true
        }

        binding.loginBtn.setOnClickListener {
            // binding.loginBtn.isEnabled = false
            MaterialDialog(this).show {
                title(R.string.login_help)
                message(R.string.login_help_new)
                positiveButton(R.string.I_know) {
                    val intent = Intent(this@LoginActivity, NewUserActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        binding.tokenLogin.setOnClickListener {

            MaterialDialog(this).show {
                title(R.string.token_login)
                message(R.string.jumpto)
                input(inputType = InputType.TYPE_CLASS_TEXT)
                positiveButton(android.R.string.ok) {
                    val token = getInputField().text.toString()
                    Observable.just(1)
                        .flatMap { ReFreshFunction.getInstance().reFreshToken(token, true) }
                        .subscribe({
                            Toasty.shortToast(R.string.login_success)
                            val intent =
                                Intent(this@LoginActivity, HelloMActivity::class.java).apply {
                                    // 避免循环添加账号导致相同页面嵌套。或者在添加账号（登录）成功时回到账号列表页面而不是导航至新的主页
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK // Or launchMode = "singleTop|singleTask"
                                }
                            startActivity(intent)
                        }, {
                            Toasty.shortToast(R.string.refresh_token_fail)
                        }).add()
                }
                neutralButton(R.string.login_help) {
                    it.message(text = "~~~")
                }
            }
        }
        binding.register.setOnClickListener {
            showRegisterHelp(binding.root)
        }
        /*        println(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this))
                if(BuildConfig.FLAVOR == "play")
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

    private fun showRegisterHelp(view: View) {
//        val intent = Intent(this@LoginActivity, NewUserActivity::class.java)
//        startActivity(intent)
        Snackbar.make(view, getString(R.string.registerclose), Snackbar.LENGTH_LONG)
            .setAction(R.string.view) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.pixiv.net")).also {
                    it.resolveActivity(packageManager)?.run {
                        startActivity(it)
                    }
                }
            }
            .show()
    }
}
