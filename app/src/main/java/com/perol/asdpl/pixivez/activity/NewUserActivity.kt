/*
 * MIT License
 *
 * Copyright (c) 2021 Austin Huang
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

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.*
import android.widget.Toast
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.networks.RestClient
import com.perol.asdpl.pixivez.networks.SharedPreferencesServices
import com.perol.asdpl.pixivez.objects.PkceUtil
import com.perol.asdpl.pixivez.responses.PixivAccountsResponse
import com.perol.asdpl.pixivez.responses.PixivOAuthResponse
import com.perol.asdpl.pixivez.services.AccountPixivService
import com.perol.asdpl.pixivez.services.OAuthSecureService
import kotlinx.android.synthetic.main.activity_new_user.*
import java.util.*

class NewUserActivity : RinkActivity() {
    private var webViewUrl: String? = null
    private var ready = false
    private var codeVerifier: String? = null

    private val webChromeClient = WebChromeClient()
    private val webViewClient: WebViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (url.startsWith("pixiv://account/login")) {
                val code = url.split("?")[1].split("&").find{ it.startsWith("code=") }
                if (code == null || code.length < 6) {
                    Toast.makeText(applicationContext, R.string.error_unknown, Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
                val intent = Intent()
                intent.putExtra("code", code.substring(5))
                intent.putExtra("codeVerifier", codeVerifier)
                setResult(8080, intent)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_user)
        initWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView.webChromeClient = webChromeClient
        webView.webViewClient = webViewClient
        val webSettings = webView.settings
        webSettings.userAgentString = "PixivAndroidApp/5.0.234 (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } else {
            val cookieSyncMngr = CookieSyncManager.createInstance(applicationContext)
            cookieSyncMngr.startSync()
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookie()
            cookieManager.removeSessionCookie()
            cookieSyncMngr.stopSync()
            cookieSyncMngr.sync()
        }

        codeVerifier = PkceUtil.generateCodeVerifier()
        val codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier!!)
        webView.loadUrl("https://app-api.pixiv.net/web/v1/login?code_challenge_method=S256&client=pixiv-android&code_challenge=" + codeChallenge)
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
