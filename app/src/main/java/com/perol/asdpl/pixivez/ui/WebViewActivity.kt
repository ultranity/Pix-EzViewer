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

package com.perol.asdpl.pixivez.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.perol.asdpl.pixivez.IntentActivity
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivityWebViewBinding
import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import java.io.ByteArrayInputStream

class WebViewActivity : RinkActivity() {

    private lateinit var binding: ActivityWebViewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val local = LanguageUtil.langToLocale(PxEZApp.language).language

//        val additionalHttpHeaders = hashMapOf<String,String>("Accept-Language" to local.displayLanguage)
//        "Accept-Language": "zh-CN"
        binding.webview.loadUrl(intent.getStringExtra("url")!!.replace("ja", local))
        // binding.webview.title
        binding.webview.settings.blockNetworkImage = false
        binding.webview.settings.javaScriptEnabled = true
        // WebView.setWebContentsDebuggingEnabled(true)
        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                if (request.url.host!! == "d.pixiv.org" || request.url.host!! == "connect.facebook.net" || request.url.host!! == "platform.twitter.com" || request.url.host!! == "www.google-analytics.com"
                ) {
                    return WebResourceResponse(
                        "application/javascript",
                        "UTF-8",
                        ByteArrayInputStream("".toByteArray())
                    )
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                try {
                    val uri = request.url
                    CrashHandler.instance.d(className, "loading $uri")
                    if (uri != null) {
                        val scheme = uri.scheme
                        val host = uri.host
                        val segment = uri.pathSegments
                        if (scheme != null) {
                            // pixiv://illusts/
                            if (scheme.contains("pixiv")) {
                                IntentActivity.start(this@WebViewActivity, request.url)
                                finish()
                                return true
                            }
                            else {
                                if (host != null) {
                                    if (uri.host?.contains("www.pixiv.net") == true) {
                                        if (segment.contains("artworks")) {
                                            val id =
                                                segment[segment.indexOf("artworks") + 1].toLong()
                                            PictureActivity.start(this@WebViewActivity, id)
                                            return true
                                        }
                                        else if (segment.contains("users")) {
                                            val userId = segment[segment.indexOf("users") + 1]
                                            UserMActivity.start(
                                                this@WebViewActivity,
                                                userId.toLong()
                                            )
                                            return true
                                        }
                                        else if (segment.size == 1 && request.url.toString()
                                            .contains("/member.php?id=")
                                        ) {
                                            request.url.getQueryParameter("id")?.let {
                                                UserMActivity.start(
                                                    this@WebViewActivity,
                                                    it.toLong()
                                                )
                                            }
                                        }
                                        return false
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        }
        binding.fab.setOnClickListener {
            finish()
        }
    }
}
