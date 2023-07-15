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

import android.util.Log
import com.perol.asdpl.pixivez.services.CloudflareService
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.InetAddress

object RubyHttpDns : Dns {
    private val addressCache = mutableMapOf<String, InetAddress>()
    private val addressCacheX = mutableMapOf<String, List<InetAddress>>()
    private val service =
        ServiceFactory.create<CloudflareService>(CloudflareService.URL_DNS_RESOLVER.toHttpUrl())
    private val apiAddress = listOf(
        "app-api.pixiv.net",
        "oauth.secure.pixiv.net",
        "accounts.pixiv.net",
        "s.pximg.net",
        "i.pximg.net",
        "imgaz.pixiv.net", // /210.140.131.144
        "sketch.pixiv.net",
        "www.pixiv.net"
    )

/* 200-250ms
D/httpdns addressList: {app-api.pixiv.net=[ ],
oauth.secure.pixiv.net=[oauth.secure.pixiv.net.cdn.cloudflare.net./104.18.31.199 ],
accounts.pixiv.net=[accounts.pixiv.net.cdn.cloudflare.net./104.18.31.199 ],
s.pximg.net=[/210.140.92.142, /210.140.92.138 ],
i.pximg.net=[/210.140.92.144, /210.140.92.139, /210.140.92.142 ],
imgaz.pixiv.net=[/210.140.131.153, /210.140.131.147, /210.140.131.144 ],
sketch.pixiv.net=[ ],
www.pixiv.net=[]}
D/httpdns addressListX: {app-api.pixiv.net=[app-api.pixiv.net/104.18.30.199 ],
oauth.secure.pixiv.net=[oauth.secure.pixiv.net/104.18.30.199, oauth.secure.pixiv.net/104.18.31.199 ],
accounts.pixiv.net=[accounts.pixiv.net/104.18.30.199, accounts.pixiv.net/104.18.31.199 ],
s.pximg.net=[s.pximg.net/210.140.92.138, s.pximg.net/210.140.92.140, s.pximg.net/210.140.92.144, s.pximg.net/210.140.92.147 ],
i.pximg.net=[i.pximg.net/210.140.92.138, i.pximg.net/210.140.92.139, i.pximg.net/210.140.92.143, i.pximg.net/210.140.92.147 ],
imgaz.pixiv.net=[ ],
sketch.pixiv.net=[ ],
www.pixiv.net=[]}
 */
/* 250-300ms
D/httpdns addressList: {app-api.pixiv.net=[app-api.pixiv.net.cdn.cloudflare.net./104.18.31.199, app-api.pixiv.net.cdn.cloudflare.net./104.18.30.199, /104.18.30.199, /104.18.31.199 ],
oauth.secure.pixiv.net=[oauth.secure.pixiv.net.cdn.cloudflare.net./104.18.31.199, oauth.secure.pixiv.net.cdn.cloudflare.net./104.18.30.199, /104.18.30.199, /104.18.31.199 ],
accounts.pixiv.net=[accounts.pixiv.net.cdn.cloudflare.net./104.18.30.199, /104.18.31.199, /104.18.30.199 ],
s.pximg.net=[/210.140.92.145, /210.140.92.144, /210.140.92.140, /210.140.92.143, /210.140.92.139, /210.140.92.142, /210.140.92.147, /210.140.92.141, /210.140.92.138, /210.140.92.146 ],
i.pximg.net=[/210.140.92.140, /210.140.92.147, /210.140.92.145, /210.140.92.143, /210.140.92.139, /210.140.92.144, /210.140.92.141, /210.140.92.138, /210.140.92.142, /210.140.92.146 ],
imgaz.pixiv.net=[/210.140.131.147, /210.140.131.144, /210.140.131.153 ],
sketch.pixiv.net=[ ],
www.pixiv.net=[www.pixiv.net.cdn.cloudflare.net./104.18.30.199, www.pixiv.net.cdn.cloudflare.net./104.18.31.199, /104.18.31.199]}
D/httpdns addressListX: {app-api.pixiv.net=[app-api.pixiv.net/104.18.31.199 ],
oauth.secure.pixiv.net=[oauth.secure.pixiv.net/104.18.31.199 ],
accounts.pixiv.net=[accounts.pixiv.net/104.18.30.199, accounts.pixiv.net/104.18.31.199 ],
s.pximg.net=[s.pximg.net/210.140.92.138, s.pximg.net/210.140.92.139, s.pximg.net/210.140.92.140, s.pximg.net/210.140.92.142, s.pximg.net/210.140.92.143, s.pximg.net/210.140.92.144, s.pximg.net/210.140.92.145, s.pximg.net/210.140.92.146, s.pximg.net/210.140.92.147 ],
i.pximg.net=[i.pximg.net/210.140.92.138, i.pximg.net/210.140.92.140, i.pximg.net/210.140.92.141, i.pximg.net/210.140.92.142, i.pximg.net/210.140.92.143, i.pximg.net/210.140.92.144, i.pximg.net/210.140.92.145, i.pximg.net/210.140.92.146, i.pximg.net/210.140.92.147 ],
imgaz.pixiv.net=[imgaz.pixiv.net/210.140.131.144, imgaz.pixiv.net/210.140.131.145, imgaz.pixiv.net/210.140.131.147, imgaz.pixiv.net/210.140.131.153 ],
sketch.pixiv.net=[ ],
www.pixiv.net=[]}
 */
    /* 300-500ms
D/httpdns addressList: {
app-api.pixiv.net=[app-api.pixiv.net.cdn.cloudflare.net./104.18.31.199, app-api.pixiv.net.cdn.cloudflare.net./104.18.30.199, /104.18.30.199 ],
oauth.secure.pixiv.net=[oauth.secure.pixiv.net.cdn.cloudflare.net./104.18.31.199, oauth.secure.pixiv.net.cdn.cloudflare.net./104.18.30.199, /104.18.31.199, /104.18.30.199 ],
accounts.pixiv.net=[accounts.pixiv.net.cdn.cloudflare.net./104.18.30.199, /104.18.31.199 ],
s.pximg.net=[/210.140.92.143, /210.140.92.141, /210.140.92.145, /210.140.92.140, /210.140.92.138, /210.140.92.147 ],
i.pximg.net=[/210.140.92.146, /210.140.92.138, /210.140.92.144, /210.140.92.147, /210.140.92.139, /210.140.92.140, /210.140.92.145, /210.140.92.143 ],
imgaz.pixiv.net=[/210.140.131.144, /210.140.131.153, /210.140.131.145 ],
sketch.pixiv.net=[ ],
www.pixiv.net=[www.pixiv.net.cdn.cloudflare.net./104.18.30.199, /104.18.31.199, /104.18.30.199]}

D/httpdns addressListX: {
app-api.pixiv.net=[app-api.pixiv.net/104.18.30.199, app-api.pixiv.net/104.18.31.199 ],
oauth.secure.pixiv.net=[oauth.secure.pixiv.net/104.18.30.199, oauth.secure.pixiv.net/104.18.31.199 ],
accounts.pixiv.net=[accounts.pixiv.net/104.18.31.199 ],
s.pximg.net=[s.pximg.net/210.140.92.146, s.pximg.net/210.140.92.138, s.pximg.net/210.140.92.141, s.pximg.net/210.140.92.142, s.pximg.net/210.140.92.147, s.pximg.net/210.140.92.144, s.pximg.net/210.140.92.145, s.pximg.net/210.140.92.139, s.pximg.net/210.140.92.143 ],
i.pximg.net=[ ],
imgaz.pixiv.net=[imgaz.pixiv.net/210.140.131.144, imgaz.pixiv.net/210.140.131.147, imgaz.pixiv.net/210.140.131.153 ],
sketch.pixiv.net=[ ],
www.pixiv.net=[www.pixiv.net/104.18.30.199, www.pixiv.net/104.18.31.199]}
     */
private var inited = true
    private fun dlookup(): List<InetAddress> {
        val addressList = mutableMapOf<String, List<InetAddress>>()
        val addressListX = mutableMapOf<String, List<InetAddress>>()
        apiAddress.forEach {
            addressListX[it] = InetAddress.getAllByName(it)
                .filter { it.isReachable(250) }
        }
        Log.d("httpdns", "========================================")
        apiAddress.forEach { k ->
            try {
                val response = service.queryDns(name = k).blockingSingle()
                response.answer.flatMap {
                    InetAddress.getAllByName(it.data)
                        .filter { it.isReachable(250) }
                }.also {
                    addressList[k] = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Log.d("httpdns addressList", addressList.toString())
        Log.d("httpdns addressListX", addressListX.toString())
        Log.d("httpdns init end", "========================================")
        return addressList.map {
            it.value[0]
        }
    }
    override fun lookup(hostname: String): List<InetAddress> {
        if (!inited) {
            inited = true
            Log.d("httpdns init", "========================================")
            Log.d("httpdns", dlookup().toString())
            Log.d("httpdns", "========================================")
        }
        val addressList = mutableListOf<InetAddress>()
        InetAddress.getByName(hostname).also {
            if (it.hostAddress?.equals(hostname) == true) {
                return listOf(it)
            }
        }
        if (addressCache.contains(hostname)) {
            return listOf(addressCache[hostname]!!)
        }
        if (addressCacheX.contains(hostname)) {
            return addressCacheX[hostname]!!
        }
        // if (addressList.isNotEmpty()) return addressList
        val defaultList = listOf( // app-api.pixiv.net
            "210.140.131.218",
            "210.140.131.188",
            "210.140.131.209",
            "210.140.131.187",
            "210.140.131.189"
        ).map { InetAddress.getByName(it) }.filter {
            it.isReachable(250)
        }
        Log.d("httpdns", "========================================")
        try {
            val response = service.queryDns(name = hostname).blockingSingle()

            response.answer.flatMap { InetAddress.getAllByName(it.data).asList() }
                .filter { it.isReachable(250) }.also {
                    addressList.addAll(it)
                }
        } catch (e: Exception) {
        }
        // if (addressList.isEmpty())
        addressList.addAll(defaultList)
        Log.d("httpdns", addressList.toString())
        Log.d("httpdns end", "========================================")
        if (addressList.isNotEmpty()) {
            addressCache[hostname] = addressList[0]
            addressCacheX[hostname] = addressList
        }
        return addressList
    }
}
