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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dns
import java.net.InetAddress

object ImageHttpDns : Dns {

    private val addressList = mutableListOf<InetAddress>()
    private val defaultList = listOf(
        "210.140.92.145",
        "210.140.92.140",
        "210.140.92.137",
    ).map { InetAddress.getByName(it) }

    override fun lookup(hostname: String): List<InetAddress> {
        if (addressList.isNotEmpty()) return addressList
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
            addressList
        }
    }
}
