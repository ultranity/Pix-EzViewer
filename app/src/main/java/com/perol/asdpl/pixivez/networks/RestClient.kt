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
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.networks.ServiceFactory.build
import com.perol.asdpl.pixivez.objects.ToastQ
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.internal.closeQuietly
import retrofit2.Retrofit
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

object RestClient {
    private val apiDns by lazy { RubyHttpXDns }
    private val imageDns by lazy { ImageHttpDns }
    private const val APP_OS = "Android"
    private val App_OS_Version = android.os.Build.VERSION.RELEASE
    private const val APP_VER = "7.13.3"
    val UA =
        "Pixiv${APP_OS}App/#{App_Version} (${APP_OS} ${App_OS_Version}; ${android.os.Build.MODEL})"
    private val ISO8601DATETIMEFORMAT by lazy {
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssZZZZZ",
            PxEZApp.locale
        )
    }
    private const val HASH_SALT =
        "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"
    private val dnsProxy
        get() = PxEZApp.instance.pre.getBoolean("dnsProxy", false)
    val pixivOkHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept-Language", "${PxEZApp.locale.toLanguageTag()}")
                .header("Access-Control-Allow-Origin", "*")
                .header("referer", "https://app-api.pixiv.net/")
            // .addHeader("Host", "https://app-api.pixiv.net")
            val request = requestBuilder.build()
            chain.proceed(request)
        }).apiProxySocket().build()
    }

    //private val httpCache = Cache(File(PxEZApp.instance.cacheDir, "http"), 256 * 1024 * 1024)
    val downloadHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("User-Agent", UA)
                .header("referer", "https://www.pixiv.net/")
                .header("host", "i.pximg.net")
            val request = requestBuilder.build()
            chain.proceed(request)
        }).imageProxySocket()//.cache(httpCache)
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    val imageHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("User-Agent", UA)
                .header("Accept-Language", "${PxEZApp.locale.toLanguageTag()}")
                .header("Access-Control-Allow-Origin", "*")
                .header("referer", "https://app-api.pixiv.net/")
                .header("Host", original.url.host.toString())
            val request = requestBuilder.build()
            chain.proceed(request)
        }).imageProxySocket(Works.apiMirror) //.cache(httpCache)
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(ProgressInterceptor())
            //add callback if timeout
            .addNetworkInterceptor {
                try {
                    it.proceed(it.request())
                } catch (e: Exception) {
                    Log.e("imageHttpClient", e.message, e)
                    ImageHttpDns.checkIPConnection()
                    throw e
                }
            }
            .build()
    }

    val retrofitAppApi: Retrofit
        get() {
            return build(
                "https://app-api.pixiv.net",
                okHttpClient("app-api.pixiv.net", needAuth = true)
            )
        }
    val gifAppApi = build("https://oauth.secure.pixiv.net", imageHttpClient)

    // val pixivAppApi = build(if(!dnsProxy) "https://app-api.pixiv.net" else "https://210.140.131.208",pixivOkHttpClient)
    // val retrofitAccount = build("https://accounts.pixiv.net", okHttpClient("accounts.pixiv.net"))
    val retrofitOauthSecure =
        build("https://oauth.secure.pixiv.net", okHttpClient("oauth.secure.pixiv.net"))
    val retrofitOauthSecureDirect = build("https://oauth.secure.pixiv.net", pixivOkHttpClient)
    private val MD5 = MessageDigest.getInstance("MD5")
    private fun encode(text: String) = MD5.digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }

    /*val httpLoggingInterceptor =
        HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Log.d("GlideInterceptor", message)
            }
        }).apply {
            level = HttpLoggingInterceptor.Level.BODY
        }*/

    private fun authHeaderInject(host: String, chain: Interceptor.Chain): Response {
        val isoDate = ISO8601DATETIMEFORMAT.format(Date())
        val original = chain.request()
        val request = original.newBuilder()
            .header("User-Agent", UA)
            .header("App-OS", APP_OS)
            .header("App-OS-Version", App_OS_Version)
            .header("App-Version", APP_VER)
            .header("Accept-Language", "${PxEZApp.locale.toLanguageTag()}")
            .header("Host", host)
            .apply {
                if (AppDataRepo.userInited()) {
                    header("Authorization", AppDataRepo.currentUser.Authorization)
                }
            }
            .header("X-Client-Time", isoDate)
            .header("X-Client-Hash", encode("$isoDate$HASH_SALT"))
            .build()
        return chain.proceed(request)
    }

    class AuthHeaderInterceptor(private val host: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            return authHeaderInject(host, chain)
        }
    }

    enum class HttpStatus(val code: Int) {
        OK(200),
        BadRequest(400),
        Unauthorized(401),
        PaymentRequired(402),
        Forbidden(403),
        NotFound(404),
        Else(-1);

        companion object {
            @JvmStatic
            fun check(value: Int): HttpStatus? {
                return entries.firstOrNull { it.code == value }
            }
        }
    }

    private const val TOKEN_INVALID = "Invalid refresh token"

    class AuthUpdateInterceptor(private val host: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = try {
                chain.proceed(request)
            } catch (e: Exception) {
                Log.e("okhttpAuth", e.message, e)
                //TODO: warn to check API Config
                throw e
            }
            when (HttpStatus.check(response.code)) {
                HttpStatus.BadRequest, HttpStatus.Unauthorized -> {
                    if (response.message.contains(TOKEN_INVALID)) {
                        ToastQ.post(R.string.login_expired)
                    } else { //if (response.message.contains(TOKEN_ERROR)) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toasty.tokenRefreshing()
                        }
                        runBlocking {
                            RefreshToken.getInstance()
                                .refreshToken(AppDataRepo.currentUser.Refresh_token)
                        }
                        response.closeQuietly()
                        return authHeaderInject(host, chain)
                    }
                }

                HttpStatus.NotFound -> {
                    Log.d("404", response.message + response.body)
                    CoroutineScope(Dispatchers.Main).launch {
                        ToastQ.post("404 " + response.message)
                    }
                }

                HttpStatus.OK -> {
                    response.message
                }

                else -> {
                    Log.e("okhttp", "${response.code}:$request\n${response.message}")
                    CoroutineScope(Dispatchers.Main).launch {
                        ToastQ.post("${response.code}:$request\n${response.message}")
                    }
                }
            }
            return response
        }
    }

    private fun okHttpClient(
        host: String,
        needAuth: Boolean = false
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.apply {
            addInterceptor(AuthHeaderInterceptor(host))
            // if (BuildConfig.DEBUG) addInterceptor(httpLoggingInterceptor)
            if (needAuth) addInterceptor(AuthUpdateInterceptor(host))
            apiProxySocket()
            connectTimeout(10, TimeUnit.SECONDS)
            readTimeout(10, TimeUnit.SECONDS)
            writeTimeout(20, TimeUnit.SECONDS)
        }
        return builder.build()
    }

    private fun OkHttpClient.Builder.apiProxySocket() = proxySocket(apiDns)
    fun OkHttpClient.Builder.imageProxySocket(mirror: Boolean = true) = apply {
        if (mirror) {
            addInterceptor {
                val original = it.request()
                val requestBuilder = original.newBuilder()
                val mirror = Works.mirrorLink(original.url.toString())
                requestBuilder.url(mirror)
                // Log.d("mirrorAPI","Request ${original.url} to $mirror")
                it.proceed(requestBuilder.build())
            }
        }
        proxySocket(imageDns)
    }

    private fun OkHttpClient.Builder.proxySocket(dns: Dns = apiDns): OkHttpClient.Builder {
        if (dnsProxy) {
            this.sslSocketFactory(RubySSLSocketFactory(), RubyX509TrustManager())
                .hostnameVerifier { _, _ -> true }
            this.dns(dns)
        }
        return this
    }
}
