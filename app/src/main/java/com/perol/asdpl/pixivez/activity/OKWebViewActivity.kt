/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
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
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.net.http.SslCertificate
import android.net.http.SslError
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.*
import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.databinding.ActivityWebViewBinding
import com.perol.asdpl.pixivez.networks.RestClient
import com.perol.asdpl.pixivez.networks.RubyHttpXDns
import com.perol.asdpl.pixivez.networks.RubySSLSocketFactory
import com.perol.asdpl.pixivez.networks.RubyX509TrustManager
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.security.SecureRandom
import javax.net.ssl.*

/*
object GlideUtil {
    fun syncLoad(url: String?, type: String): ByteArray? {
        val isGif = type.endsWith("gif")
        if (isGif) {
            try {
                val target: FutureTarget<ByteArray> = Glide.with(PxEZApp.instance)
                    .`as`(ByteArray::class.java)
                    .load(url)
                    .decode(GifDrawable::class.java).submit()
                return target.get()
            } catch (e: java.lang.Exception) {
                Log.e(TAG, e.printStackTrace().toString())
            }
            return null
        }
        val target: FutureTarget<Bitmap> = Glide.with(PxEZApp.instance)
            .asBitmap().load(url).submit()
        try {
            val bitmap = target.get()
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            return baos.toByteArray()
        } catch (e: java.lang.Exception) {
            Log.e(TAG, e.printStackTrace().toString())
        }
        return null
    }
    fun isImage(url:String):Boolean{
        return url.endsWith(".png") ||url.endsWith(".jpeg")
                ||url.endsWith(".jpeg")
    }
}
*/
object WebviewDnsInterceptUtil {
    private const val TAG = "WebviewDnsInterceptUtil"
    lateinit var userAgentString: String
    fun getDnsInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        return if (request != null && request.url != null &&
            request.method.equals("get", ignoreCase = true) &&
            request.url.scheme?.matches(Regex("https?")) == true
        ) {
            getWebResourceFromUrl(request.url)
        } else null
    }

    fun getDnsInterceptUrl(view: WebView?, url: Uri): WebResourceResponse? {
        return if (url.toString().isNotEmpty() && url.scheme != null) {
            getWebResourceFromUrl(url) // .toString())
        } else null
    }

    private fun getWebResourceFromUrl(url: Uri): WebResourceResponse? {
        /*val builder = OkHttpClient.Builder()
        builder.addInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header(
                        "Accept-Language",
                        "${RestClient.local.language}_${RestClient.local.country}"
                    )
                    .header("Access-Control-Allow-Origin", "*")
                val request = requestBuilder.build()
                return chain.proceed(request)
            }
        }).httpProxySocket()*/
        Log.d(TAG, "try load url: $url")
        if (url.toString().contains("recaptcha")) {
            Log.d(TAG, "recaptcha")
        }
        try {
            val url = URL(url.toString())
            var response = RestClient.pixivOkHttpClient.newCall(
                Request.Builder().get().url(url)
                    .header("Host", url.host.toString())
                    .header("User-Agent", userAgentString).build()
            ).execute()
            if (!response.isSuccessful && !response.request.url.equals(url)) {
                response = RestClient.pixivOkHttpClient.newCall(
                    Request.Builder().get().url(response.request.url)
                        .header("Host", response.request.url.host)
                        .header("User-Agent", userAgentString).build()
                ).execute()
            }
            val type = response.headers["content-type"]?.split(";")?.get(0) ?: "text/html"
            val res = response.body?.byteStream()
            Log.d(
                TAG,
                "url:$type $url\n" + if (res == null) "$url response empty" else "loaded"
            )
            return WebResourceResponse(type, "UTF-8", res).also {
                it.responseHeaders = response.headers.toMap()
                it.responseHeaders.remove("access-control-allow-origin")
                it.responseHeaders["Access-Control-Allow-Origin"] = "*"
            }
        } catch (e: Exception) {
            // Log.e(TAG, e.printStackTrace().toString())
            try {
                val url = URL(url.toString())
                val response = RestClient.pixivOkHttpClient.newCall(
                    Request.Builder().get().url(url)
                        .header("Host", url.host.toString())
                        .header("User-Agent", userAgentString).build()
                ).execute()

                val type = response.headers["content-type"]?.split(";")?.get(0) ?: "text/html"
                val res = response.body?.byteStream()
                Log.d(
                    TAG,
                    "url:$type $url\n" + if (res == null) "$url response empty" else "2loaded"
                )
                return WebResourceResponse(type, "UTF-8", res).also {
                    it.responseHeaders = response.headers.toMap()
                    it.responseHeaders.remove("access-control-allow-origin")
                    it.responseHeaders["Access-Control-Allow-Origin"] = "*"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Load $url failed")
                Log.e(TAG, e.printStackTrace().toString())
                return null
            }
        }
    }

    fun getWebResourceFromUrl(url: String): WebResourceResponse? {
        val scheme: String = Uri.parse(url).scheme!!.trim()
        var ip: String? = Uri.parse(url).host?.let { RubyHttpXDns.lookup(it)[0].hostAddress }
        if (ip.isNullOrBlank()) {
            Log.d(TAG, "web log 不拦截：$url")
            return null
        }
        Log.d(TAG, "web log 请求 url: $url ->$ip")

        if (url.contains("/recaptcha/")) {
            Log.d(TAG, "recaptcha")
            ip = URL(url).host.toString()
        }
        // HttpDns解析css文件的网络请求及图片请求
        if (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) {
            try {
                val oldUrl = URL(url)
                // 获取HttpDns域名解析结果 // 通过HTTPDNS获取IP成功，进行URL替换和HOST头设置
                Log.d(TAG, "HttpDns ips are: " + ip + " for host: " + oldUrl.host)
                val newUrl: String = url.replaceFirst(oldUrl.host.toRegex(), ip)
                Log.d(TAG, "newUrl a is: $newUrl")
                val connection: HttpsURLConnection =
                    URL(newUrl).openConnection() as HttpsURLConnection // 设置HTTP请求头Host域
                connection.hostnameVerifier = getNullHostNameVerifier()
                connection.sslSocketFactory = getIgnoreSSLContext()?.socketFactory ?: RubySSLSocketFactory()
                connection.setRequestProperty("Host", oldUrl.host)
                connection.setRequestProperty("Referer", "https://app-api.pixiv.net/")
                Log.d(TAG, "ContentType a: " + connection.contentType)
                // 有可能是text/html; charset=utf-8的形式，只需要第一个
                val type = connection.contentType?.split(";")?.get(0) ?: "text/html"
                return WebResourceResponse(type, "UTF-8", connection.inputStream)
            } catch (e: Exception) {
                Log.e(TAG, e.printStackTrace().toString())
            }
        }
        return null
    }

    @Volatile
    private var mIgnoreSSLContext: SSLContext? = null

    @Volatile
    private var mNullHostNameVerifier: HostnameVerifier? = null

    private fun getIgnoreSSLContext(): SSLContext? {
        if (mIgnoreSSLContext == null) {
            synchronized(WebviewDnsInterceptUtil::class.java) {
                if (mIgnoreSSLContext == null) {
                    try {
                        mIgnoreSSLContext = SSLContext.getInstance("TLS").apply {
                            init(
                                null,
                                arrayOf<TrustManager>(RubyX509TrustManager()),
                                SecureRandom()
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, e.printStackTrace().toString())
                    }
                }
            }
        }
        return mIgnoreSSLContext
    }

    private fun getNullHostNameVerifier(): HostnameVerifier? {
        if (mNullHostNameVerifier == null) {
            synchronized(WebviewDnsInterceptUtil::class.java) {
                mNullHostNameVerifier = NullHostNameVerifier()
            }
        }
        return mNullHostNameVerifier
    }
    class NullHostNameVerifier : HostnameVerifier {
        @SuppressLint("BadHostnameVerifier")
        override fun verify(hostname: String?, session: SSLSession?): Boolean {
            return true
        }
    }
}

class OKWebViewActivity : RinkActivity() {
    private lateinit var binding: ActivityWebViewBinding
    private lateinit var mWebview: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        mWebview = binding.webview
        setContentView(binding.root)
        val local = LanguageUtil.langToLocale(PxEZApp.language).language

//        val additionalHttpHeaders = hashMapOf<String,String>("Accept-Language" to local.displayLanguage)
//        "Accept-Language": "zh-CN"
        mWebview.loadUrl(intent.getStringExtra("url")!!.replace("/ja/", "/$local/"))
        // mWebview.title
        val settings = mWebview.settings
        settings.blockNetworkImage = false
        settings.blockNetworkLoads = false
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        // settings.setAppCacheEnabled(true)
        WebviewDnsInterceptUtil.userAgentString = settings.userAgentString
        // settings.builtInZoomControls = true
        // settings.displayZoomControls = false
        // settings.loadWithOverviewMode = true
        val isNightMode = (
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES
            )
        /*
        mUiModeManager = (UiModeManager) mContext.getSystemService(Context.UI_MODE_SERVICE);
        if (mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES)
         */
        if (isNightMode) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                mWebview.settings.forceDark = WebSettings.FORCE_DARK_ON
            } 
            else {
                mWebview.setBackgroundColor(Color.BLACK)
                injectCSS(mWebview)
            }
        }
        mWebview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
            }
        }
        // mWebview.loadData("","text/html","UTF-8")
        mWebview.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                Log.d("shouldInterceptRequest", request.url.toString())
                // no analytics & platform
                if (listOf(
                        "d.pixiv.org",
                        "connect.facebook.net",
                        "platform.twitter.com",
                        "www.google-analytics.com"
                    )
                    .contains(request.url.host)
                ) {
                    return WebResourceResponse(
                        "application/javascript",
                        "UTF-8",
                        ByteArrayInputStream("".toByteArray())
                    )
                }

                /*if (GlideUtil.isImage(request.url.toString())){

                    return WebResourceResponse(
                        "image/jpeg",
                        "UTF-8",
                        ByteArrayInputStream(GlideUtil.syncLoad(request.url.toString(),"image/jpeg"))
                    )
                }*/
                if ((request.url.host?.contains("pximg.net") == true)
                    or (request.url.host?.contains("pixiv.net") == true)
                    or (request.url.host?.contains("gstatic") == true)
                ) {
                    return WebviewDnsInterceptUtil.getDnsInterceptRequest(view, request)
                }
                /*if (listOf(
                        "www.recaptcha.net",
                        "www.gstatic.cn"
                    ).contains(request.url.host))*/
                if ((request.url.host?.contains("recaptcha.net") == true)) {
                    return WebviewDnsInterceptUtil.getWebResourceFromUrl(request.url.toString())
                }
                return super.shouldInterceptRequest(view, request)
            }
            // override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            //    return false
            // }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                try {
                    val uri = request.url
                    Log.d(className, "loading $uri")
                    if (uri != null) {
                        val scheme = uri.scheme
                        val host = uri.host
                        val segment = uri.pathSegments
                        if (scheme != null) {
                            // pixiv://illusts/
                            if (scheme.contains("pixiv")) {
                                IntentActivity.start(this@OKWebViewActivity, request.url)
                                finish()
                                return true
                            }
                            else {
                                if (host != null) {
                                    if (uri.host?.contains("www.pixiv.net") == true) {
                                        if (segment.contains("artworks")) {
                                            val id =
                                                segment[segment.indexOf("artworks") + 1].toLong()
                                            PictureActivity.start(this@OKWebViewActivity, id)
                                            return true
                                        }
                                        else if (segment.contains("users")) {
                                            val userId = segment[segment.indexOf("users") + 1]
                                            UserMActivity.start(
                                                this@OKWebViewActivity,
                                                userId.toLong()
                                            )
                                            return true
                                        }
                                        else if (segment.size == 1 && request.url.toString()
                                            .contains("/member.php?id=")
                                        ) {
                                            request.url.getQueryParameter("id")?.let {
                                                UserMActivity.start(
                                                    this@OKWebViewActivity,
                                                    it.toLong()
                                                )
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
                    Log.e("OverrideUrlLoading", e.printStackTrace().toString())
                }
                return false
            }

            override fun onLoadResource(view: WebView, url: String) {
                if (BuildConfig.DEBUG) {
                    Log.d("onLoadResource", url)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (BuildConfig.DEBUG) {
                    Log.d("onReceivedError", "$request $error")
                }
            }
            val sslErrors: Array<String> = arrayOf(
                "Not yet valid",
                "Expired",
                "Hostname mismatch",
                "Untrusted CA",
                "Invalid date",
                "Unknown error"
            )
            fun certificateToStr(certificate: SslCertificate?): String? {
                if (certificate == null) {
                    return null
                }
                var s = ""
                val issuedTo = certificate.issuedTo
                if (issuedTo != null) {
                    s += "Issued to: " + issuedTo.dName + "\n"
                }
                val issuedBy = certificate.issuedBy
                if (issuedBy != null) {
                    s += "Issued by: " + issuedBy.dName + "\n"
                }
                val issueDate = certificate.validNotBeforeDate
                if (issueDate != null) {
                    s += String.format("Issued on: %tF %tT %tz\n", issueDate, issueDate, issueDate)
                }
                val expiryDate = certificate.validNotAfterDate
                if (expiryDate != null) {
                    s += String.format(
                        "Expires on: %tF %tT %tz\n",
                        expiryDate,
                        expiryDate,
                        expiryDate
                    )
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
                    if (primaryError >= 0 && primaryError < sslErrors.size) sslErrors[primaryError] else "Unknown error $primaryError"
                AlertDialog.Builder(this@OKWebViewActivity)
                    .setTitle("Insecure connection")
                    .setMessage(
                        String.format(
                            "Error: %s\nURL: %s\n\nCertificate:\n%s",
                            errorStr,
                            error.url,
                            certificateToStr(error.certificate)
                        )
                    )
                    .setPositiveButton(
                        "Proceed"
                    ) { dialog: DialogInterface?, which: Int -> handler.proceed() }
                    .setNegativeButton(
                        "Cancel"
                    ) { dialog: DialogInterface?, which: Int -> handler.cancel() }
                    .show()
            }
        }
        binding.fab.setOnLongClickListener {
            mWebview.reload()
            true
        }
        binding.fab.setOnClickListener {
            finish()
        }
    }

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
            /*
            String cssDolphin = "*,:before,:after,html *{color:#61615f!important;-webkit-border-image:none!important;border-image:none!important;background:none!important;background-image:none!important;box-shadow:none!important;text-shadow:none!important;border-color:#212a32!important}\n" +
                    "\n" +
                    "body{background-color:#000000!important}\n" +
                    "html a,html a *{text-decoration:none!important;color:#394c65!important}\n" +
                    "html a:hover,html a:hover *{color:#394c65!important;background:#1b1e23!important}\n" +
                    "html a:visited,html a:visited *,html a:active,html a:active *{color:#58325b!important}\n" +
                    "html select,html option,html textarea,html button{color:#aaa!important;border:1px solid #212a32!important;background:#161a1e!important;border-color:#212a32!important;border-style:solid}\n" +
                    "html select:hover,html option:hover,html button:hover,html textarea:hover,html select:focus,html option:focus,html button:focus,html textarea:focus{color:#bbb!important;background:#161a1e!important;border-color:#777 #999 #999 #777 !important}\n" +
                    "html input,html input[type=text],html input[type=search],html input[type=password]{color:#4e4e4e!important;background-color:#161a1e!important;box-shadow:1px 0 4px rgba(16,18,23,.75) inset,0 1px 4px rgba(16,18,23,.75) inset!important;border-color:#1a1c27!important;border-style:solid!important}\n" +
                    "html input:focus,html input[type=text]:focus,html input[type=search]:focus,html input[type=password]:focus{color:#bbb!important;outline:none!important;background:#161a1e!important;border-color:#1a3973}\n" +
                    "html input:hover,html select:hover,html option:hover,html button:hover,html textarea:hover,html input:focus,html select:focus,html option:focus,html button:focus,html textarea:focus{color:#bbb!important;background:#093681!important;opacity:0.4!important;border-color:#777 #999 #999 #777 !important}\n" +
                    "html input[type=button],html input[type=submit],html input[type=reset],html input[type=image]{color:#4e4e4e!important;border-color:#888 #666 #666 #888 !important}\n" +
                    "html input[type=button],html input[type=submit],html input[type=reset]{border:1px solid #212a32!important;background-image:0 color-stop(1,#181a23))!important}\n" +
                    "html input[type=button]:hover,html input[type=submit]:hover,html input[type=reset]:hover,html input[type=image]:hover{border-color:#777 #999 #999 #777 !important}\n" +
                    "html input[type=button]:hover,html input[type=submit]:hover,html input[type=reset]:hover{border:1px solid #666!important;background-image:0 color-stop(1,#262939))!important}\n" +
                    "html img,html svg{opacity:.5!important;border-color:#111!important}\n" +
                    "html ::-webkit-input-placeholder{color:#4e4e4e!important}\n";
*/
            val styleElementId = "night_mode_style_4398357" // should be unique
            // if (isNightMode) {
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
            /*if (isDesktopUA) {
                webview.evaluateJavascript(
                    "javascript:document.querySelector('meta[name=viewport]').content='width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=1';",
                    null
                )
            }*/
        } catch (e: Exception) {
            Log.e("injectCSS", e.printStackTrace().toString())
        }
    }
    private fun injectCSS() {
        /*val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(this))
            .addPathHandler("/res/", ResourcesPathHandler(this))
            .build()

        mWebview.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }
        mWebview.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")*/
        try {
            assets.open("pixivision_dark.css")
            val inputStream: InputStream = resources.openRawResource(R.raw.pixivision_dark)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
            mWebview.loadUrl(
                "javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" + // Tell the browser to BASE64-decode the string into your script !!!
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()"
            )
        } catch (e: Exception) {
            Log.e("injectCSS", e.printStackTrace().toString())
        }
    }
}
/*
class TlsSniSocketFactory(private val conn: HttpsURLConnection) :
    SSLSocketFactory() {
    private val TAG: String = "TlsSniSocketFactory"
    private var hostnameVerifier: HostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()

    @Throws(IOException::class)
    override fun createSocket(): Socket? {
        return null
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int): Socket? {
        return null
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int
    ): Socket? {
        return null
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket? {
        return null
    }

    @Throws(IOException::class)
    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int
    ): Socket? {
        return null
    }

    // TLS layer
    override fun getDefaultCipherSuites(): Array<String?> {
        return arrayOfNulls(0)
    }

    override fun getSupportedCipherSuites(): Array<String?> {
        return arrayOfNulls(0)
    }

    @Throws(IOException::class)
    override fun createSocket(
        plainSocket: Socket,
        host: String,
        port: Int,
        autoClose: Boolean
    ): Socket {
        var peerHost = conn.getRequestProperty("Host")
        if (peerHost == null) peerHost = host
        Log.i(TAG, "customized createSocket. host: $peerHost")
        val address = plainSocket.inetAddress
        if (autoClose) {
            // we don't need the plainSocket
            plainSocket.close()
        }
        // create and connect SSL socket, but don't do hostname/certificate verification yet
        val sslSocketFactory =
            SSLCertificateSocketFactory.getDefault(0) as SSLCertificateSocketFactory
        val ssl = sslSocketFactory.createSocket(address, port) as SSLSocket

        // enable TLSv1.1/1.2 if available
        ssl.enabledProtocols = ssl.supportedProtocols

        // set up SNI before the handshake
        Log.i(TAG, "Setting SNI hostname")
        sslSocketFactory.setHostname(ssl, peerHost)

        // verify hostname and certificate
        val session = ssl.session
        if (!hostnameVerifier.verify(peerHost, session)) throw SSLPeerUnverifiedException(
            "Cannot verify hostname: $peerHost"
        )
        Log.i(
            TAG, "Established " + session.protocol + " connection with " + session.peerHost +
                    " using " + session.cipherSuite
        )
        return ssl
    }
}*/
