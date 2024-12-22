package com.perol.asdpl.pixivez.networks

import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.objects.CrashHandler
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.create
import java.util.concurrent.TimeUnit

object ServiceFactory {
    val contentType = "application/json".toMediaType()

    val gson = Json {
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
        coerceInputValues = true
        allowSpecialFloatingPointValues = true
        allowStructuredMapKeys = true
        prettyPrint = false
        useArrayPolymorphism = false
        ignoreUnknownKeys = true
        //namingStrategy = JsonNamingStrategy.SnakeCase
        //useAlternativeNames = false
    }

    // Or "https://dns.cloudflare.com/.well-known/dns-query"
    val CFDNS = DnsOverHttps("https://1.1.1.1/dns-query?ct=application/dns-udpwireformat")
    private fun DnsOverHttps(url: String): DnsOverHttps {
        return DnsOverHttps.Builder()
            .client(OkHttpClient())
            .url(url.toHttpUrl())
            .post(true)
            .resolvePrivateAddresses(false)
            .resolvePublicAddresses(true)
            .build()
    }

    /**
     * API declarations([T]) must be interfaces.
     */
    inline fun <reified T : Any> create(
        httpUrl: String,
        httpClient: OkHttpClient = HttpClient.DEFAULT,
        converterFactory: Converter.Factory? = gson.asConverterFactory(contentType),
        callAdapterFactory: CallAdapter.Factory? = null,
    ): T {
        require(T::class.java.isInterface && T::class.java.interfaces.isEmpty()) {
            "API declarations must be interfaces and API interfaces must not extend other interfaces."
        }

        val retrofit = build(httpUrl, httpClient, converterFactory, callAdapterFactory)

        return retrofit.create()
    }

    fun build(
        httpUrl: String, //.toHttpUrl()
        httpClient: OkHttpClient,
        converterFactory: Converter.Factory? = gson.asConverterFactory(contentType),
        callAdapterFactory: CallAdapter.Factory? = null,
        block: (Retrofit.Builder.() -> Unit)? = null
    ): Retrofit {
        val retrofit = Retrofit.Builder()
            .apply {
                callAdapterFactory?.let { addCallAdapterFactory(it) }
                converterFactory?.let { addConverterFactory(it) }
                block?.invoke(this)
            }
            .baseUrl(httpUrl)
            .client(httpClient)
            .validateEagerly(BuildConfig.DEBUG)
            .build()

        return retrofit
    }
    object HttpClient {
        val DEFAULT: OkHttpClient by lazy {
            OkHttpClient.Builder()
//                .addInterceptor(httpLoggingInterceptor)
                .connectTimeout(8L, TimeUnit.SECONDS)
                .readTimeout(8L, TimeUnit.SECONDS)
                .writeTimeout(8L, TimeUnit.SECONDS)
                .build()
        }

        private val httpLoggingInterceptor by lazy {
            HttpLoggingInterceptor { message ->
                CrashHandler.instance.d(
                    "GlideInterceptor",
                    message
                )
            }.apply {
                level =
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }
        }
    }
}
