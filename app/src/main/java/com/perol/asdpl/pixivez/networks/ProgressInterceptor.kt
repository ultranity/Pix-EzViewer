package com.perol.asdpl.pixivez.networks

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.*
import java.io.IOException
import java.util.*


interface ProgressListener {
    fun onProgress(progress: Int)
}


class ProgressInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val url = request.url.toString()
        val body = response.body!!
        return response.newBuilder().body(ProgressResponseBody(url, body)).build()
    }

    companion object {
        val LISTENER_MAP: MutableMap<String, ProgressListener> =
            WeakHashMap()

        //入注册下载监听
        fun addListener(url: String, listener: ProgressListener) {
            LISTENER_MAP[url] = listener
        }

        //取消注册下载监听
        fun removeListener(url: String?) {
            LISTENER_MAP.remove(url)
        }
    }
}

class ProgressResponseBody(url: String?, private val responseBody: ResponseBody) :
    ResponseBody() {
    private var bufferedSource: BufferedSource? = null
    private var listener: ProgressListener?

    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = ProgressSource(responseBody.source()).buffer()
        }
        return bufferedSource as BufferedSource
    }

    private inner class ProgressSource(source: Source) :
        ForwardingSource(source) {
        var totalBytesRead: Long = 0
        var currentProgress = 0

        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            val fullLength = responseBody.contentLength()
            if (bytesRead == -1L) {
                totalBytesRead = fullLength
            } else {
                totalBytesRead += bytesRead
            }
            if (listener != null){
                val progress = (100f * totalBytesRead / fullLength).toInt()
                Log.d("GlideProgress", "download progress is $progress")
                if( progress != currentProgress) {
                    listener!!.onProgress(progress)
                }
                if (listener != null && totalBytesRead == fullLength) {
                    listener = null

                }
                currentProgress = progress
            }
            return bytesRead
        }
    }

    init {
        listener = ProgressInterceptor.LISTENER_MAP[url]
    }
}
