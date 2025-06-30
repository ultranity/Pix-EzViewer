/*
 * MIT License
 *
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

import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.ToastQ
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dns
import java.net.InetAddress

object ImageHttpDns : Dns {

    var shuffleIP = PxEZApp.instance.pre.getBoolean("shuffleIP", true)
    internal val addressList = mutableListOf<InetAddress>()
    private var defaultList = listOf(
        "203.137.29.47",
        "210.140.139.129",
        "210.140.139.133",
        "210.140.139.137",
    ).map { InetAddress.getByName(it) }
    const val IP_LIST_PATTERN =
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(,\\s*((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))*\$"
    val ipPattern = Regex(IP_LIST_PATTERN)
    lateinit var customIPs: MutableList<String>
    private var customList: List<InetAddress> = listOf()

    fun setCustomIPs(string: String?) {
        customIPs = string?.takeIf(ipPattern::matches)
            ?.split(",")?.map { it.trim() }?.toMutableList() ?: mutableListOf<String>()
        customList = customIPs.map { InetAddress.getByName(it) }
    }

    init {
        addressList.addAll(defaultList)
        setCustomIPs(PxEZApp.instance.pre.getString("customIPs", null))
        addressList.addAll(customList)
        checkIPConnection()
    }

    private var lastCheckTime = 0L
    fun checkIPConnection() {
        if (System.currentTimeMillis() - lastCheckTime < 60000) return
        lastCheckTime = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            addressList.removeAll {
                try {
                    !it.isReachable(5000)
                } catch (e: Exception) {
                    CrashHandler.instance.e("ImageHttpDns", "$it is not reachable", e)
                    true
                }
            }
        }
    }

    fun fetchIPs() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ServiceFactory.CFDNS.lookup("i.pximg.net")
                addressList.addAll(response)
                //customIPs.addAll(response.mapNotNull { it.hostAddress })
            } catch (e: Exception) {
                CrashHandler.instance.e("ImageHttpDns", "i.pximg.net DNS fetch failed", e)
            }
        }
    }
    override fun lookup(hostname: String): List<InetAddress> {
        if (addressList.isNotEmpty())
            return if (shuffleIP) addressList.shuffled() else addressList
        CoroutineScope(Dispatchers.Main).launch {
            ToastQ.post("All default IP failed, fallback to CF DNS")
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ServiceFactory.CFDNS.lookup(hostname)
                addressList.addAll(response)
            } catch (e: Exception) {
                try {
                    CrashHandler.instance
                        .e("ImageHttpDns", "CF DNS for $hostname failed", e)
                    addressList.addAll(Dns.SYSTEM.lookup(hostname))
                } catch (e: Exception) {
                    CrashHandler.instance
                        .e("ImageHttpDns", "SYSTEM DNS for $hostname failed", e)
                    // ignore
                }
            }
        }

        return addressList.ifEmpty {
            addressList.addAll(defaultList)
            addressList.addAll(customList)
            addressList
        }
    }
}
