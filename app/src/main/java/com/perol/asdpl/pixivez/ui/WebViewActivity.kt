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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslCertificate
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.core.net.toUri
import com.perol.asdpl.pixivez.IntentActivity
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivityWebViewBinding
import com.perol.asdpl.pixivez.networks.RestClient
import com.perol.asdpl.pixivez.networks.bypass.WebViewBypassInterceptor
import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import com.perol.asdpl.pixivez.ui.user.UserMActivity

class WebViewActivity : RinkActivity() {
    private lateinit var binding: ActivityWebViewBinding
    private val bypass by lazy { WebViewBypassInterceptor(RestClient.UA) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        // 状态栏沉浸 —— 与旧 OKWebViewActivity 保持一致
        binding.rootContainer.fitsSystemWindows = true
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (ThemeUtil.isDarkMode(this)) window.statusBarColor = Color.TRANSPARENT
        setContentView(binding.root)

        setupChrome()
        setupWebView()

        val lang = PxEZApp.locale.language
        val url = intent.getStringExtra("url")!!.replace("/ja/", "/$lang/")
        binding.webview.loadUrl(url)
    }

    // -------------------------------------------------------------------------
    // 工具栏 + 地址栏 + 下拉刷新 + 返回键
    // -------------------------------------------------------------------------
    private fun setupChrome() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_refresh      -> binding.webview.reload()
                R.id.action_forward      -> if (binding.webview.canGoForward()) binding.webview.goForward()
                R.id.action_copy         -> copyLink(binding.webview.url)
                R.id.action_share        -> shareLink(binding.webview.url)
                R.id.action_open_external -> openExternal(binding.webview.url)
            }
            true
        }
        binding.addressBar.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                binding.webview.loadUrl(normalizeUrl(v.text.toString()))
                true
            } else false
        }
        binding.swipe.setOnRefreshListener { binding.webview.reload() }
        onBackPressedDispatcher.addCallback(this) {
            if (binding.webview.canGoBack()) binding.webview.goBack() else finish()
        }
    }

    // -------------------------------------------------------------------------
    // WebView 设置:JS、存储、夜间模式 CSS、拦截器
    // -------------------------------------------------------------------------
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() = binding.webview.apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.blockNetworkImage = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // 夜间模式 —— 移植自 OKWebViewActivity
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        if (isNightMode) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                settings.forceDark = WebSettings.FORCE_DARK_ON
            } else {
                setBackgroundColor(Color.BLACK)
                // injectCSS 在 onPageFinished 里调用(需 WebView 实例已设好 client)
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progress.visibility =
                    if (newProgress in 1..99) View.VISIBLE else View.GONE
                binding.progress.setProgressCompat(newProgress, true)
            }
            override fun onReceivedTitle(view: WebView?, title: String?) {
                binding.toolbar.title = title
            }
        }

        webViewClient = object : WebViewClient() {
            // ---------- 拦截器:bypass 引擎统一分发 ----------
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? =
                bypass.intercept(request) ?: super.shouldInterceptRequest(view, request)

            // ---------- 页面加载进度同步地址栏 ----------
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.addressBar.setText(url)
                binding.swipe.isRefreshing = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.swipe.isRefreshing = false
                // 夜间模式 CSS 注入 —— 移植自 OKWebViewActivity
                if (isNightMode && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                    injectCSS(binding.webview)
                }
            }

            // ---------- 移植自旧 OKWebViewActivity：pixiv:// 与 artworks/users/member 原生路由 ----------
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
                            // pixiv://illusts/ 等自定义协议 → IntentActivity 分发
                            if (scheme.contains("pixiv")) {
                                IntentActivity.start(this@WebViewActivity, request.url)
                                finish()
                                return true
                            } else {
                                if (host != null) {
                                    if (uri.host?.contains("www.pixiv.net") == true) {
                                        if (segment.contains("artworks")) {
                                            val id = segment[segment.indexOf("artworks") + 1].toInt()
                                            PictureActivity.start(this@WebViewActivity, id)
                                            return true
                                        } else if (segment.contains("users")) {
                                            val userId = segment[segment.indexOf("users") + 1]
                                            UserMActivity.start(this@WebViewActivity, userId.toInt())
                                            return true
                                        } else if (segment.size == 1 && request.url.toString()
                                            .contains("/member.php?id=")
                                        ) {
                                            request.url.getQueryParameter("id")?.let {
                                                UserMActivity.start(this@WebViewActivity, it.toInt())
                                                return true
                                            }
                                        }
                                        return false
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    CrashHandler.instance.e("OverrideUrlLoading", e.printStackTrace().toString())
                }
                return false
            }

            // ---------- 移植自旧 OKWebViewActivity：SSL 错误对话框 ----------
            val sslErrors: Array<String> = arrayOf(
                "Not yet valid",
                "Expired",
                "Hostname mismatch",
                "Untrusted CA",
                "Invalid date",
                "Unknown error"
            )

            @SuppressLint("DefaultLocale")
            fun certificateToStr(certificate: SslCertificate?): String? {
                if (certificate == null) return null
                var s = ""
                val issuedTo = certificate.issuedTo
                if (issuedTo != null) s += "Issued to: " + issuedTo.dName + "\n"
                val issuedBy = certificate.issuedBy
                if (issuedBy != null) s += "Issued by: " + issuedBy.dName + "\n"
                val issueDate = certificate.validNotBeforeDate
                if (issueDate != null) {
                    s += String.format("Issued on: %tF %tT %tz\n", issueDate, issueDate, issueDate)
                }
                val expDate = certificate.validNotAfterDate
                if (expDate != null) {
                    s += String.format("Expires on: %tF %tT %tz\n", expDate, expDate, expDate)
                }
                return s
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                val primaryError: Int = error.primaryError
                val errorStr: String =
                    if (primaryError >= 0 && primaryError < sslErrors.size) sslErrors[primaryError]
                    else "Unknown error $primaryError"
                MaterialDialogs(this@WebViewActivity).show {
                    setTitle("Insecure connection")
                    setMessage(
                        String.format(
                            "Error: %s\nURL: %s\n\nCertificate:\n%s",
                            errorStr,
                            error.url,
                            certificateToStr(error.certificate)
                        )
                    )
                    setPositiveButton("Proceed") { _, _ -> handler.proceed() }
                    cancelButton { _, _ -> handler.cancel() }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 夜间模式 CSS 注入 —— 移植自旧 OKWebViewActivity（逻辑不变）
    // -------------------------------------------------------------------------
    private fun injectCSS(webview: WebView) {
        try {
            val css =
                (
                    "*, :after, :before {background-color: #161a1e !important; color: #61615f !important; border-color: #212a32 !important; background-image:none !important; outline-color: #161a1e !important; z-index: 1 !important} " +
                        "svg, img {filter: grayscale(100%) brightness(50%) !important; -webkit-filter: grayscale(100%) brightness(50%) !important} " +
                        "input {background-color: black !important;}" +
                        "select, option, textarea, button, input {color:#aaa !important; background-color: black !important; border:1px solid #212a32 !important}" +
                        "a, a * {text-decoration: none !important; color:#32658b !important}" +
                        "a:visited, a:visited * {color: #783b78 !important}" +
                        "* {max-width: 100vw !important} pre {white-space: pre-wrap !important}"
                    )
            val styleElementId = "night_mode_style_4398357"
            val js: String = (
                "if (document.head) {" +
                    "if (!window.night_mode_id_list) night_mode_id_list = new Set();" +
                    "var newset = new Set();" +
                    "   for (var n of document.querySelectorAll(':not(a)')) { " +
                    "     if (n.closest('a') != null) continue;" +
                    "     if (!n.id) n.id = 'night_mode_id_' + (night_mode_id_list.size + newset.size);" +
                    "     if (!night_mode_id_list.has(n.id)) newset.add(n.id); " +
                    "   }" +
                    "for (var item of newset) night_mode_id_list.add(item);" +
                    "var style = document.getElementById('" + styleElementId + "');" +
                    "if (!style) {" +
                    "   style = document.createElement('style');" +
                    "   style.id = '" + styleElementId + "';" +
                    "   style.type = 'text/css';" +
                    "   style.innerHTML = '" + css + "';" +
                    "   document.head.appendChild(style);" +
                    "}" +
                    "   var css2 = ' ';" +
                    "   for (var nid of newset) css2 += ('#' + nid + '#' + nid + ',');" +
                    "   css2 += '#nonexistent {background-color: #161a1e !important; color: #61615f !important; border-color: #212a32 !important; background-image:none !important; outline-color: #161a1e !important; z-index: 1 !important}';" +
                    "   style.innerHTML += css2;" +
                    "}" +
                    "var iframes = document.getElementsByTagName('iframe');" +
                    "for (var i = 0; i < iframes.length; i++) {" +
                    "   var fr = iframes[i];" +
                    "   var style = fr.contentWindow.document.createElement('style');" +
                    "   style.id = '" + styleElementId + "';" +
                    "   style.type = 'text/css';" +
                    "   style.innerHTML = '" + css + "';" +
                    "   fr.contentDocument.head.appendChild(style);" +
                    "}"
                )
            webview.evaluateJavascript("javascript:(function() {$js})()", null)
        } catch (e: Exception) {
            CrashHandler.instance.e("injectCSS", e.printStackTrace().toString())
        }
    }

    // -------------------------------------------------------------------------
    // 辅助：地址规范化 / 复制 / 分享 / 外部打开
    // -------------------------------------------------------------------------
    private fun normalizeUrl(input: String): String {
        val t = input.trim()
        return if (t.contains(".") && !t.contains(" ")) {
            if (t.startsWith("http")) t else "https://$t"
        } else "https://www.google.com/search?q=" + android.net.Uri.encode(t)
    }

    private fun copyLink(url: String?) {
        if (url.isNullOrEmpty()) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("url", url))
    }

    private fun shareLink(url: String?) {
        if (url.isNullOrEmpty()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, null))
    }

    private fun openExternal(url: String?) {
        url?.let { startActivity(Intent(Intent.ACTION_VIEW, it.toUri())) }
    }
}
