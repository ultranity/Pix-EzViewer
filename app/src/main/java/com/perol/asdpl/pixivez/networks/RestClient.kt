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
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.repository.AppDataRepository
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.X509TrustManager


class RestClient {
    private val httpsDns by lazy { RubyHttpDns() }
    private val local = LanguageUtil.langToLocale(PxEZApp.language)
    private val disableProxy by lazy { PreferenceManager.getDefaultSharedPreferences(PxEZApp.instance).getBoolean("disableproxy",false)}
    private val pixivOkHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .addHeader("Accept-Language", "${local.language}_${local.country}")
                    .removeHeader("User-Agent")
                    .addHeader(
                        "User-Agent",
                        "PixivAndroidApp/5.0.155 (Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MODEL})"
                    )
                val request = requestBuilder.build()
                return chain.proceed(request)
            }


        })
        proxySocket(builder)

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
                        .removeHeader("User-Agent")
                        .addHeader(
                            "User-Agent",
                            "PixivAndroidApp/5.0.155 (Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MODEL})"
                        )
                        .addHeader("referer", "https://app-api.pixiv.net/")
                    val request = requestBuilder.build()
                    return chain.proceed(request)
                }
            }).dns(ImageHttpDns())
            return builder.build()
        }


    private val gson = GsonBuilder().create()
    val retrofitAppApi = buildRetrofit(if(disableProxy) "https://app-api.pixiv.net" else "https://210.140.131.188",okHttpClient("app-api.pixiv.net"))

    val pixivisionAppApi = buildRetrofit(if(disableProxy) "https://app-api.pixiv.net" else "https://210.140.131.188",pixivOkHttpClient)

    val retrofitAccount = buildRetrofit(if(disableProxy) "https://accounts.pixiv.net" else "https://210.140.131.219",okHttpClient("accounts.pixiv.net"))
    private val HashSalt =
        "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"

    fun encode(text: String): String {
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

    private fun okHttpClient(host:String): OkHttpClient {
        val httpLoggingInterceptor =
            HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    Log.d("GlideInterceptor", message)
                }
            }).apply {
                level =
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }
        val builder = OkHttpClient.Builder()

        builder.addInterceptor(object : Interceptor {

            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                val ISO8601DATETIMEFORMAT =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", local)
                val isoDate = ISO8601DATETIMEFORMAT.format(Date())
                val original = chain.request()
                var Authorization = ""
                runBlocking {
                    try {
                        Authorization = AppDataRepository.getUser().Authorization
                    } catch (e: Exception) {
                    }
                }
                Log.d("init","Request $Authorization and ${original.header("Authorization")}")
                val requestBuilder = original.newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader(
                        "User-Agent",
                        "PixivAndroidApp/5.0.155 (Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MODEL})"
                    )
                    .addHeader("Accept-Language", "${local.language}_${local.country}")
                    .header("Authorization", Authorization)
                    .addHeader("App-OS", "Android")
                    .addHeader("App-OS-Version", android.os.Build.VERSION.RELEASE)
                    .header("App-Version", "5.0.166")
                    .addHeader("X-Client-Time", isoDate)
                    .addHeader("X-Client-Hash", encode("$isoDate$HashSalt"))
                    .addHeader("Host", host)
                val request = requestBuilder.build()
                return chain.proceed(request)
            }
        })
//                .addInterceptor(httpLoggingInterceptor)
        proxySocket(builder)
        builder.connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
        return builder
            .build()
    }

    private fun proxySocket(builder: OkHttpClient.Builder) {
        if (!PreferenceManager.getDefaultSharedPreferences(PxEZApp.instance).getBoolean(
                "disableproxy",
                false
            )
        ) {
            builder.sslSocketFactory(RubySSLSocketFactory(), RubyX509TrustManager())
                .hostnameVerifier { p0, p1 -> true }
            builder.dns(httpsDns)
        }
    }

    fun getRetrofitGIF(): Retrofit {
        return retrofit{
            baseUrl("https://oauth.secure.pixiv.net/")
                .client(imageHttpClient)
        }
    }

    private fun buildRetrofit(baseUrl:String, client: OkHttpClient)
            = retrofit {
                baseUrl(baseUrl)
                .client(client)
    }

    private fun retrofit(block: Retrofit.Builder.() -> Unit): Retrofit {
        return Retrofit.Builder().apply(block)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
    }

    fun getRetrofitOauthSecure(): Retrofit {
        return buildRetrofit(if(disableProxy) "https://app-api.pixiv.net" else "https://210.140.131.188",okHttpClient("oauth.secure.pixiv.net"))
    }

}
