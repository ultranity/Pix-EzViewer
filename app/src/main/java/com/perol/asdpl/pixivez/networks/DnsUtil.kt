package com.perol.asdpl.pixivez.networks

import com.perol.asdpl.pixivez.objects.CrashHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import kotlin.math.roundToInt

class DnsUtil {
    companion object {
        const val TAG = "DnsUtil"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun run() {
        CrashHandler.instance.d(TAG, "Starting Connection Tests...")

        runBlocking {
            val failurePercent = listOf(
                "8.8.8.8",
                "1.1.1.1",
                "4.2.2.1",
            ).asFlow().flatMapMerge {
                flow { emit(verify(it)) }
            }.toList()
            val maxFailurePercent = failurePercent.max()
            CrashHandler.instance.d(TAG, "Max Failure Rate: $maxFailurePercent%")
            if (maxFailurePercent < 1) {
                CrashHandler.instance.d(TAG, "Connection is Stable.")
            } else {
                CrashHandler.instance.e(TAG, "Connection is NOT Stable.")
            }
        }

    }

    private fun verify(host: String, times: Int = 20): Int {
        val results = mutableListOf<Pair<Boolean, Long>>()

        (1..times).forEach {
            val result = ping(host)
            CrashHandler.instance.d(
                TAG,
                "Ping #$it for $host: ${if (result.first) "Success" else "Failure"} time: ${result.second}ms"
            )
            results.add(result)
            Thread.sleep(500) // add a little delay for better verification
        }
        val failTotal = results
            .sumOf { result -> if (result.first) 0.0 else 1.0 }
        val failPercentage = ((failTotal / times) * 100).roundToInt()
        val averageTime = results
            .map { it.second }
            .average()
        CrashHandler.instance.d(
            TAG,
            "Test for $host has fail rate of $failPercentage% and an average response time of ${averageTime}ms"
        )
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
            CrashHandler.instance.e(TAG, ex.localizedMessage ?: "")
            return false to elapsedTime(startTime)
        }
    }

    private fun elapsedTime(startTimeMillis: Long): Long = System.currentTimeMillis() - startTimeMillis
}
