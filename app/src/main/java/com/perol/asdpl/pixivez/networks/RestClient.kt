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
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.objects.showInMain
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val local = LanguageUtil.langToLocale(PxEZApp.language)
    private const val App_OS = "Android"
    private val App_OS_Version = android.os.Build.VERSION.RELEASE
    private const val App_Version = "7.13.3"
    val UA =
        "Pixiv${App_OS}App/#{App_Version} (${App_OS} ${App_OS_Version}; ${android.os.Build.MODEL})"
    private val ISO8601DATETIMEFORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", local)
    private const val HashSalt =
        "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"
    private val dnsProxy
        get() = PxEZApp.instance.pre.getBoolean("dnsProxy", false)
    val pixivOkHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept-Language", "${local.language}_${local.country}")
                .header("Access-Control-Allow-Origin", "*")
                .header("referer", "https://app-api.pixiv.net/")
            // .addHeader("Host", "https://app-api.pixiv.net")
            val request = requestBuilder.build()
            chain.proceed(request)
        }).apiProxySocket()

        return@lazy builder.build()
    }

    private val imageHttpClient: OkHttpClient
        get() {
            val builder = OkHttpClient.Builder()
            builder.addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .addHeader("User-Agent", UA)
                    .header("referer", "https://app-api.pixiv.net/")
                val request = requestBuilder.build()
                chain.proceed(request)
            }).imageProxySocket()

            return builder.build()
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
    val retrofitOauthSecureDirect = build(
        "https://oauth.secure.pixiv.net",
        okHttpClient("oauth.secure.pixiv.net", true)
    )
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
    class HeaderInterceptor(private val host: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            return headerInject(host, chain)
        }

    }

    private fun headerInject(host: String, chain: Interceptor.Chain): Response {
        val isoDate = ISO8601DATETIMEFORMAT.format(Date())
        val original = chain.request()
        var Authorization = ""
        try {
            Authorization = AppDataRepo.currentUser.Authorization
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("OkHttpClient", "get Authorization failed")
        }
        val request = original.newBuilder()
            .header("User-Agent", UA)
            .header("App-OS", App_OS)
            .header("App-OS-Version", App_OS_Version)
            .header("App-Version", App_Version)
            .header("Accept-Language", "${local.language}_${local.country}")
            .header("Host", host)
            .header("Authorization", Authorization)
            .header("X-Client-Time", isoDate)
            .header("X-Client-Hash", encode("$isoDate$HashSalt"))
            .build()
        return chain.proceed(request)
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

    private const val TOKEN_ERROR = "Error occurred at the OAuth process"
    private const val TOKEN_INVALID = "Invalid refresh token"

    class AuthInterceptor(private val host: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)
            when (HttpStatus.check(response.code)) {
                HttpStatus.BadRequest, HttpStatus.Unauthorized -> {
                    if (response.message.contains(TOKEN_INVALID)) {
                        Toasty.error(PxEZApp.instance, R.string.login_expired).showInMain()
                    } else { //if (response.message.contains(TOKEN_ERROR)) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toasty.tokenRefreshing()
                            RefreshToken.getInstance()
                                .refreshToken(AppDataRepo.currentUser.Refresh_token)
                        }
                        response.closeQuietly()
                        return headerInject(host, chain)
                    }
                }

                HttpStatus.NotFound -> {
                    Log.d("404", response.message + response.body)
                    Toasty.warning(PxEZApp.instance, "404 " + response.message).showInMain()
                }

                HttpStatus.OK -> {
                    response.message
                }

                else -> {
                    Log.e("okhttp", request.toString() + "\n" + response.message)
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
            addInterceptor(HeaderInterceptor(host))
            // if (BuildConfig.DEBUG) addInterceptor(httpLoggingInterceptor)
            if (needAuth) addInterceptor(AuthInterceptor(host))
            apiProxySocket()
            connectTimeout(10, TimeUnit.SECONDS)
            readTimeout(10, TimeUnit.SECONDS)
            writeTimeout(20, TimeUnit.SECONDS)
        }
        return builder.build()
    }

    private fun OkHttpClient.Builder.apiProxySocket() = proxySocket(apiDns)
    fun OkHttpClient.Builder.imageProxySocket() = apply {
        if (Works.mirrorForView) {
            addInterceptor {
                val original = it.request()
                val requestBuilder = original.newBuilder()
                val mirror = Works.mirrorLinkView(original.url.toString())
                requestBuilder.url(mirror)
                // Log.d("mirrorLinkView","Request ${original.url} to $mirror")
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
