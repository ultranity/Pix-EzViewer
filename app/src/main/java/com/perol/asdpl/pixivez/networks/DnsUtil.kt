package com.perol.asdpl.pixivez.networks

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.util.stream.Collectors
import kotlin.math.roundToInt

class DnsUtil {
    companion object {

        const val TAG = "DnsUtil"
        const val google1 = "8.8.8.8"
        const val cloudFlare = "1.1.1.1"
        const val comcast = "4.2.2.1"
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun run() {
        Log.d(TAG, "Starting Connection Tests...")
        val maxFailurePercent: Int =
            listOf(
                cloudFlare,
                google1,
                comcast
            )
                .parallelStream()
                .map {
                    verify(it)
                }
                .collect(Collectors.toList())
                .maxOrNull() ?: 0

        Log.d(TAG, "Max Failure Rate: $maxFailurePercent%")
        if (maxFailurePercent < 1) {
            Log.d(TAG, "Connection is Stable.")
        }
        else {
            Log.e(TAG, "Connection is NOT Stable.")
        }
    }

    private fun verify(host: String, times: Int = 20): Int {
        val results = mutableListOf<Pair<Boolean, Long>>()

        (1..times).forEach {
            val result = ping(host)
            Log.d(TAG, "Ping #$it for $host: ${if (result.first) "Success" else "Failure"} time: ${result.second}ms")
            results.add(result)
            Thread.sleep(500) // add a little delay for better verification
        }
        val failTotal = results
            .sumOf { result -> if (result.first) 0.0 else 1.0 }
        val failPercentage = ((failTotal / times) * 100).roundToInt()
        val averageTime = results
            .map { it.second }
            .average()
        Log.d(TAG, "Test for $host has fail rate of $failPercentage% and an average response time of ${averageTime}ms")
        return failPercentage
    }

    private fun ping(host: String): Pair<Boolean, Long> {
        val startTime = System.currentTimeMillis()
        try {
            SocketChannel
                .open()
                .use {
                    it.configureBlocking(true)
                    val result = it.connect(InetSocketAddress(host, 443)) // it.connect(InetSocketAddress(host, 443))
                    return result to elapsedTime(startTime)
                }
        } catch (ex: Exception) {
            Log.e(TAG, ex.localizedMessage ?: "")
            return false to elapsedTime(startTime)
        }
    }

    private fun elapsedTime(startTimeMillis: Long): Long = System.currentTimeMillis() - startTimeMillis
}
