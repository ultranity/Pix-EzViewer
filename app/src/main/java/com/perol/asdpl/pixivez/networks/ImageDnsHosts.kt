package com.perol.asdpl.pixivez.networks

import okhttp3.Request

internal object ImageDnsHosts {
    fun contains(hostname: String): Boolean {
        val host = hostname.lowercase().removeSuffix(".")
        return host == "pximg.net" || host.endsWith(".pximg.net")
    }

    /** Image/download clients retain the origin Host even after force-IP URL rewriting. */
    fun origin(request: Request): String =
        request.header("Host")?.takeIf(::contains) ?: request.url.host
}
