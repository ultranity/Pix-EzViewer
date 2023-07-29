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
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.networks.ServiceFactory.contentType
import com.perol.asdpl.pixivez.networks.ServiceFactory.gson
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object RestClient {
    private val apiDns by lazy { RubyHttpXDns }
    private val httpDns by lazy { RubyHttpDns }
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
    private val disableProxy by lazy { PxEZApp.instance.pre.getBoolean("disableproxy", false) }
    val pixivOkHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept-Language", "${local.language}_${local.country}")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("referer", "https://app-api.pixiv.net/")
                // .addHeader("Host", "https://app-api.pixiv.net")
                val request = requestBuilder.build()
                return chain.proceed(request)
            }
        }).apiProxySocket()

        return@lazy builder.build()
    }

    private val imageHttpClient: OkHttpClient
        get() {
            val builder = OkHttpClient.Builder()
            builder.addInterceptor(object : Interceptor {
                @Throws(IOException::class)
                override fun intercept(chain: Interceptor.Chain): Response {
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                        .addHeader("User-Agent", RestClient.UA)
                        .header("referer", "https://app-api.pixiv.net/")
                    val request = requestBuilder.build()
                    return chain.proceed(request)
                }
            }).imageProxySocket()

            return builder.build()
        }

    val retrofitAppApi: Retrofit
        get() {
            return buildRetrofit("https://app-api.pixiv.net", okHttpClient("app-api.pixiv.net"))
        }
    val gifAppApi = buildRetrofit("https://oauth.secure.pixiv.net", imageHttpClient)

    // val pixivAppApi = buildRetrofit(if(disableProxy) "https://app-api.pixiv.net" else "https://210.140.131.208",pixivOkHttpClient)
    // val retrofitAccount = buildRetrofit("https://accounts.pixiv.net", okHttpClient("accounts.pixiv.net"))
    val retrofitOauthSecure =
        buildRetrofit("https://oauth.secure.pixiv.net", okHttpClient("oauth.secure.pixiv.net"))
    val retrofitOauthSecureDirect = buildRetrofit(
        "https://oauth.secure.pixiv.net",
        okHttpClient("oauth.secure.pixiv.net", true)
    )

    private fun encode(text: String): String {
        try {
            val instance: MessageDigest = MessageDigest.getInstance("MD5")
            val digest: ByteArray = instance.digest(text.toByteArray())
            val sb = StringBuffer()
            for (b in digest) {
                val i: Int = b.toInt() and 0xff
                var hexString = Integer.toHexString(i)
                if (hexString.length < 2) {
                    hexString = "0$hexString"
                }
                sb.append(hexString)
            }
            return sb.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

    private fun okHttpClient(host: String, disableProxy: Boolean = false): OkHttpClient {
        /*val httpLoggingInterceptor =
            HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    Log.d("GlideInterceptor", message)
                }
            }).apply {
                level =
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }*/
        val builder = OkHttpClient.Builder()
        builder.apply {
            addInterceptor(
                Interceptor { chain ->
                    val isoDate = ISO8601DATETIMEFORMAT.format(Date())
                    val original = chain.request()
                    var Authorization = ""
                    try {
                        Authorization = AppDataRepo.currentUser.Authorization
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("OkHttpClient", "get Authorization failed")
                    }
                    Log.d(
                        "OkHttpClient",
                        "Request $Authorization and original ${original.header("Authorization")}"
                    )
                    val requestBuilder = original.newBuilder()
                        .header("User-Agent", UA)
                        .addHeader("App-OS", App_OS)
                        .addHeader("App-OS-Version", App_OS_Version)
                        .addHeader("App-Version", App_Version)
                        .addHeader("Accept-Language", "${local.language}_${local.country}")
                        .header("Authorization", Authorization)
                        .addHeader("X-Client-Time", isoDate)
                        .addHeader("X-Client-Hash", encode("$isoDate$HashSalt"))
                        .addHeader("Host", host)
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
            )
            // addInterceptor(httpLoggingInterceptor)
            if (!disableProxy) {
                apiProxySocket()
            }
            connectTimeout(10, TimeUnit.SECONDS)
            readTimeout(10, TimeUnit.SECONDS)
            writeTimeout(20, TimeUnit.SECONDS)
        }
        return builder.build()
    }

    private fun OkHttpClient.Builder.apiProxySocket() = proxySocket(apiDns)
    fun OkHttpClient.Builder.imageProxySocket() = apply {
        if (Works.mirrorLinkView) {
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
        if (!disableProxy) {
            this.sslSocketFactory(RubySSLSocketFactory(), RubyX509TrustManager())
                .hostnameVerifier { p0, p1 -> true }
            this.dns(dns)
        }
        return this
    }

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient) =
        retrofit { baseUrl(baseUrl).client(client) }

    private fun retrofit(block: Retrofit.Builder.() -> Unit): Retrofit {
        return Retrofit.Builder().apply(block)
            .addConverterFactory(gson.asConverterFactory(contentType) {
                JsonObject(jsonObject.filterKeys { it != "response" && it != "search_span_limit" })
            })
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
    }
}
