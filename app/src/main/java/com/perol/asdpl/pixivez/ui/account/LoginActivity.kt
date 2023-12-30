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

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.KotlinUtil.launchCatching
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.base.getInputField
import com.perol.asdpl.pixivez.base.setInput
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.databinding.ActivityLoginBinding
import com.perol.asdpl.pixivez.networks.Pkce
import com.perol.asdpl.pixivez.networks.RefreshToken
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.ui.MainActivity
import com.perol.asdpl.pixivez.ui.OKWebViewActivity
import com.perol.asdpl.pixivez.ui.settings.FirstInfoDialog
import com.perol.asdpl.pixivez.ui.settings.SettingsActivity
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers

class LoginActivity : RinkActivity() {
    // private var username: String? = null
    // private var password: String? = null
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//        window.decorView.systemUiVisibility =
//            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT

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
                    .setAction("settings.before.login")
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("CheckResult")
    private fun initBind() {
        if (!AppDataRepo.pre.getBoolean("firstinfo")) {
            FirstInfoDialog().show(this.supportFragmentManager, "infoDialog")
        }
        /*try {
            if (AppDataRepo.pre.getString("password") != null) {
                binding.editPassword.setText(AppDataRepo.pre.getString("password"))
                binding.editUsername.setText(AppDataRepo.pre.getString("username"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }*/
        binding.textviewHelp.setOnClickListener {
            // obtain an instance of Markwon
            val markwon = Markwon.create(this)
            MaterialDialogs(this).show {
                confirmButton()
                setMessage(markwon.toMarkdown(getString(R.string.login_help_md)))
            }
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
            // AppDataRepo.pre.setString("username", username)
            // AppDataRepo.pre.setString("password", password)
            val url: String = "https://app-api.pixiv.net/web/v1/login?code_challenge=" +
                    Pkce.getPkce().challenge + "&code_challenge_method=S256&client=pixiv-android"
            // WeissUtil.start()
            // WeissUtil.proxy()
            val intent = Intent(this@LoginActivity, OKWebViewActivity::class.java)
                .setAction("login.try")
            intent.putExtra("url", url)
            startActivity(intent)
            true
        }

        binding.loginBtn.setOnClickListener {
            // binding.loginBtn.isEnabled = false
            MaterialDialogs(this).show {
                setTitle(R.string.login_help)
                setMessage(R.string.login_help_new)
                setPositiveButton(R.string.I_know) { _, _ ->
                    val intent = Intent(this@LoginActivity, NewUserActivity::class.java)
                        .setAction("login.try")
                    startActivity(intent)
                }
            }
        }
        binding.tokenLogin.setOnClickListener {
            MaterialDialogs(this).show {
                setTitle(R.string.token_login)
                setInput {
                    editText!!.inputType = InputType.TYPE_CLASS_TEXT
                    hint = "Token"
                }
                confirmButton { dialog, which ->
                    val token = getInputField(dialog).text.toString()
                    lifecycleScope.launchCatching({
                        RefreshToken.getInstance().refreshToken(token, true)
                    }, {
                        Toasty.shortToast(R.string.login_success)
                        Intent(this@LoginActivity, MainActivity::class.java)
                            .setAction("login.success").apply {
                                // 避免循环添加账号导致相同页面嵌套。或者在添加账号（登录）成功时回到账号列表页面而不是导航至新的主页
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                // Or launchMode = "singleTop|singleTask"
                            }.let { startActivity(it) }
                    }, { Toasty.shortToast(R.string.refresh_token_fail) }, Dispatchers.Main)
                }
                //TODO: token login help
                setNeutralButton(R.string.login_help) { dialog, which -> }
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
