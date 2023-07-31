package com.perol.asdpl.pixivez.networks

import android.util.Log
import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.services.CloudflareService
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
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
        //ignoreUnknownKeys = true
        //namingStrategy = JsonNamingStrategy.SnakeCase
    }

    /**
     * API declarations([T]) must be interfaces.
     */
    inline fun <reified T : Any> create(
        httpUrl: HttpUrl = "https://0.0.0.0/".toHttpUrl(),
        httpClient: OkHttpClient = HttpClient.DEFAULT,
        callAdapterFactory: CallAdapter.Factory? = null,
        converterFactory: Converter.Factory? = gson.asConverterFactory(contentType)
    ): T {
        require(T::class.java.isInterface && T::class.java.interfaces.isEmpty()) {
            "API declarations must be interfaces and API interfaces must not extend other interfaces."
        }

        val retrofit = Retrofit.Builder()
            .apply {
                callAdapterFactory?.let { addCallAdapterFactory(it) }
                converterFactory?.let { addConverterFactory(it) }
            }
            .baseUrl(httpUrl)
            .client(httpClient)
            .validateEagerly(BuildConfig.DEBUG)
            .build()

        return retrofit.create()
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
            HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    Log.d("GlideInterceptor", message)
                }
            }).apply {
                level =
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    val cloudflareService: CloudflareService =
        create(CloudflareService.URL_DNS_RESOLVER.toHttpUrl())
}
