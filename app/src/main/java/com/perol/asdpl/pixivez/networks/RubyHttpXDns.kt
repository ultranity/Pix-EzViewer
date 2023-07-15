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
import okhttp3.Dns
import java.net.InetAddress

object RubyHttpXDns : Dns {
    private val addressCache = mutableMapOf<String, InetAddress>()
    private val addressCacheX = mutableMapOf<String, List<InetAddress>>()
    private val service = ServiceFactory.cloudflareService
    private val apiAddress = listOf(
        "app-api.pixiv.net",
        "oauth.secure.pixiv.net",
        "accounts.pixiv.net",
        "s.pximg.net",
        "i.pximg.net",
        "imgaz.pixiv.net",
        "sketch.pixiv.net",
        "www.pixiv.net",
        "www.recaptcha.net",
        "www.gstatic.cn"
    )

    private val defaultApiAddress = listOf(
        "210.140.131.220", // "210.140.131.208" //app-api.pixiv.net
        "210.140.131.219", // "oauth.secure.pixiv.net",
        "210.140.131.219", // "accounts.pixiv.net",
        "210.140.92.141", // "s.pximg.net"
        "210.140.92.140", // "i.pximg.net",
        "210.140.131.145", // "imgaz.pixiv.net",
        "210.140.170.179", // "sketch.pixiv.net",
        "210.140.131.223", // "www.pixiv.net",
        "203.208.41.34", // "www.recaptcha.net",
        "203.208.40.66" // "www.gstatic.cn"

    )

    /*
    D/httpdns addressList: {app-api.pixiv.net=[app-api.pixiv.net.cdn.cloudflare.net./104.18.31.199, app-api.pixiv.net.cdn.cloudflare.net./104.18.30.199, /104.18.30.199, /104.18.31.199], oauth.secure.pixiv.net=[oauth.secure.pixiv.net.cdn.cloudflare.net./104.18.30.199, oauth.secure.pixiv.net.cdn.cloudflare.net./104.18.31.199, /104.18.31.199, /104.18.30.199], accounts.pixiv.net=[accounts.pixiv.net.cdn.cloudflare.net./104.18.30.199, accounts.pixiv.net.cdn.cloudflare.net./104.18.31.199, /104.18.30.199, /104.18.31.199], s.pximg.net=[/210.140.92.138, /210.140.92.142, /210.140.92.146, /210.140.92.139, /210.140.92.140, /210.140.92.143, /210.140.92.147, /210.140.92.141, /210.140.92.144, /210.140.92.145], i.pximg.net=[/210.140.92.140, /210.140.92.141, /210.140.92.144, /210.140.92.145, /210.140.92.147, /210.140.92.142, /210.140.92.146, /210.140.92.139, /210.140.92.143, /210.140.92.138], imgaz.pixiv.net=[/210.140.131.147, /210.140.131.144, /210.140.131.153, /210.140.131.145], sketch.pixiv.net=[/210.140.174.37, /210.140.170.179, /210.140.175.130], www.pixiv.net=[www.pixiv.net.cdn.cloudflare.net./104.18.30.199, www.pixiv.net.cdn.cloudflare.net./104.18.31.199, /104.18.31.199, /104.18.30.199]}
D/httpdns addressListX: {app-api.pixiv.net=[app-api.pixiv.net/103.228.130.27, app-api.pixiv.net/2001::45ab:ef0b], oauth.secure.pixiv.net=[oauth.secure.pixiv.net/31.13.64.33, oauth.secure.pixiv.net/2001::9a55:661e], accounts.pixiv.net=[accounts.pixiv.net/108.160.163.116, accounts.pixiv.net/2001::453f:b00b], s.pximg.net=[s.pximg.net/210.140.92.140, s.pximg.net/210.140.92.141, s.pximg.net/210.140.92.146, s.pximg.net/210.140.92.138, s.pximg.net/210.140.92.143, s.pximg.net/210.140.92.144, s.pximg.net/210.140.92.145, s.pximg.net/210.140.92.142, s.pximg.net/210.140.92.147, s.pximg.net/210.140.92.139], i.pximg.net=[i.pximg.net/108.160.169.186, i.pximg.net/2001::6ca0:a36a], imgaz.pixiv.net=[imgaz.pixiv.net/128.121.146.101, imgaz.pixiv.net/2001::48e8:aa10], sketch.pixiv.net=[sketch.pixiv.net/31.13.66.6, sketch.pixiv.net/2001::7a0a:5504], www.pixiv.net=[www.pixiv.net/31.13.65.1, www.pixiv.net/2001::453f:ba1e]}
D/httpdns init end: ========================================
D/httpdns: [app-api.pixiv.net.cdn.cloudflare.net./104.18.31.199, oauth.secure.pixiv.net.cdn.cloudflare.net./104.18.30.199, accounts.pixiv.net.cdn.cloudflare.net./104.18.30.199, /210.140.92.138, /210.140.92.140, /210.140.131.147, /210.140.174.37, www.pixiv.net.cdn.cloudflare.net./104.18.30.199]
     */
    private var inited = false
    fun dlookup(): List<InetAddress> {
        val addressList = mutableMapOf<String, List<InetAddress>>()
        val addressListX = mutableMapOf<String, List<InetAddress>>()
        apiAddress.forEach { addressListX[it] = InetAddress.getAllByName(it).asList() }
        Log.d("httpdns", "========================================")
        apiAddress.forEach { k ->
            try {
                val response = service.queryDns(name = k).blockingSingle()
                response.answer.flatMap { InetAddress.getAllByName(it.data).asList() }.also {
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
            // Log.d("httpdns init", "========================================")
            // Log.d("httpdns", dlookup().toString())
            // Log.d("httpdns", "========================================")
            apiAddress.forEachIndexed { index, host ->
                InetAddress.getByName(defaultApiAddress[index]).also {
                    // addressCache[host]= it
                    addressCacheX[host] = listOf(it)
                }
            }
        }
        // return if full ip
        if (hostname.matches("((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}".toRegex())) {
            return listOf(InetAddress.getByName(hostname))
        }
        /*try {
            InetAddress.getByName(hostname).also {
                if (it.hostAddress.equals(hostname))
                    return listOf(it)
            }
        }catch (e: UnknownHostException){
            Log.d("httpdns", "UnknownHostException $e")

        }*/
        // if (addressCache.contains(hostname))
        //    return listOf(addressCache[hostname]!!)
        if (addressCacheX.contains(hostname)) {
            return addressCacheX[hostname]!!
        }
        // if (addressList.isNotEmpty()) return addressList
        Log.d("httpdns", "========================================")
        val addressList = mutableListOf<InetAddress>()
        try {
            val response = service.queryDns(name = hostname).blockingSingle()
            response.answer.flatMap { InetAddress.getAllByName(it.data).asList() }.also {
                addressList.addAll(it)
            }
        } catch (e: Exception) {
            addressList.addAll(Dns.SYSTEM.lookup(hostname))
        }
        Log.d("httpdns", addressList.toString())
        Log.d("httpdns end", "========================================")
        if (addressList.isNotEmpty()) {
            addressCache[hostname] = addressList[0]
            addressCacheX[hostname] = addressList
        }
        return addressList
    }
}
