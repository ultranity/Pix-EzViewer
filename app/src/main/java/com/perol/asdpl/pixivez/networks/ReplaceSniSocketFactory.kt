/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
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

import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/*
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ ReplaceSniSocketFactory —— 把 ClientHello 的 SNI 替换为指定主机名。        │
 * │                                                                            │
 * │ 用途:对已解析的直连 IP 建 SSL socket,但 SNI 填一个【未被 GFW 封、且能让   │
 * │ 服务端选中覆盖目标 Host 的证书】的主机名(默认 pixiv.me)。                 │
 * │ - 避开空 SNI 在某些入口拿到不覆盖三段域名的默认证书而被判 421;            │
 * │ - 避开明文真实 SNI(*.pixiv.net)被 GFW SNI 过滤 RST。                      │
 * │ 与 [RubySSLSocketFactory](空 SNI)同构,仅多设 serverNames。              │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
class ReplaceSniSocketFactory(private val sniHost: String) : SSLSocketFactory() {

    override fun getDefaultCipherSuites(): Array<String> = arrayOf()

    override fun getSupportedCipherSuites(): Array<String> = arrayOf()

    override fun createSocket(socket: Socket?, host: String?, port: Int, autoClose: Boolean): Socket {
        // 复用 OkHttp 已建立连接的对端地址,但用 InetAddress 重载建 SSLSocket(不自动带 SNI),
        // 再手动把 SNI 设为 sniHost。
        val address = socket!!.inetAddress
        return (getDefault().createSocket(address, port) as SSLSocket).apply {
            enabledProtocols = supportedProtocols
            sslParameters = sslParameters.apply {
                serverNames = listOf(SNIHostName(sniHost))
            }
        }
    }

    override fun createSocket(host: String?, port: Int): Socket? = null
    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket? = null
    override fun createSocket(address: InetAddress?, port: Int): Socket? = null
    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket? = null
}
