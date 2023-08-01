package com.perol.asdpl.pixivez.networks

import android.annotation.SuppressLint
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
// 忽略对 https 证书的校验
@SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager")
class RubyX509TrustManager : X509TrustManager {
    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}

    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}

    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}
