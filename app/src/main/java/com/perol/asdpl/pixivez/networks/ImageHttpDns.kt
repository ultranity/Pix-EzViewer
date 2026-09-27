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

import androidx.core.content.edit
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Dns
import java.net.InetAddress

object ImageHttpDns : Dns {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile var shuffleIP = PxEZApp.instance.pre.getBoolean("shuffleIP", true)
    private val defaultList = listOf(
        "210.140.139.133", "210.140.139.129", "210.140.139.137",
    ).map { InetAddress.getByName(it) }
    const val IP_LIST_PATTERN =
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(,\\s*((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))*\$"
    val ipPattern = Regex(IP_LIST_PATTERN)
    @Volatile var customIPs: List<String> = emptyList()
        private set
    @Volatile private var customList: List<InetAddress> = emptyList()

    private val resolver = RefreshingDns(
        upstream = DnsLookup { ServiceFactory.CFDNS.lookup(it) },
        fallback = { defaultList },
        accepts = ImageDnsHosts::contains,
        preferred = { customList },
        order = { if (shuffleIP) it.shuffled() else it },
        load = { host ->
            val pre = PxEZApp.instance.pre
            val raw = pre.getString("imageDns.$host.addresses", null)
            val ips = parseIPs(raw).map { InetAddress.getByName(it) }
            ips.takeIf { it.isNotEmpty() }?.let {
                RefreshingDns.Saved(pre.getLong("imageDns.$host.at", 0), it)
            }
        },
        save = { host, answer ->
            PxEZApp.instance.pre.edit {
                putLong("imageDns.$host.at", answer.at)
                putString("imageDns.$host.addresses", answer.addresses.joinToString(",") { it.hostAddress!! })
            }
        },
    )

    // Settings reads a snapshot; no caller can mutate a pool used by another request.
    internal val addressList: List<InetAddress>
        get() = (customList + resolver.snapshot() + defaultList).distinct()

    init { setCustomIPs(PxEZApp.instance.pre.getString("customIPs", null)) }

    private fun parseIPs(raw: String?): List<String> = raw?.trim()?.takeIf(ipPattern::matches)
        ?.split(",")?.map { it.trim() }?.distinct().orEmpty()

    fun setCustomIPs(string: String?) {
        customIPs = parseIPs(string)
        customList = customIPs.map { InetAddress.getByName(it) }
    }

    /** Expire on a real HTTPS failure, never on ICMP reachability (often blocked). */
    fun connectionFailed(host: String) {
        resolver.invalidate(host)
    }

    fun fetchIPs() {
        scope.launch {
            for (host in listOf("i.pximg.net", "s.pximg.net")) {
                resolver.invalidate(host)
                resolver.lookup(host)
            }
        }
    }

    /** Legacy force-IP URL generation may run from a download button on the main thread. */
    fun cachedForUrl(host: String): List<InetAddress> {
        if (!ImageDnsHosts.contains(host)) return emptyList()
        val cached = resolver.cached(host)
        scope.launch { resolver.lookup(host) }
        return cached
    }

    override fun lookup(hostname: String): List<InetAddress> = resolver.lookup(hostname)
}
