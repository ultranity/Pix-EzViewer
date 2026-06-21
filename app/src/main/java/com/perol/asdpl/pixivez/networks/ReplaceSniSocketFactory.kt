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
 * │ 用途:对已建立连接的 socket 套上 TLS,但 SNI 填一个【未被 GFW 封、且能让    │
 * │ 服务端选中覆盖目标 Host 的证书】的主机名(默认 pixiv.me)。                 │
 * │ - 避开空 SNI 在某些入口拿到不覆盖三段域名的默认证书而被判 421;            │
 * │ - 避开明文真实 SNI(*.pixiv.net)被 GFW SNI 过滤 RST。                      │
 * │                                                                            │
 * │ 实现:用「分层」重载复用 OkHttp 已连接的 socket(尊重其连接超时、不另起     │
 * │ 连接/不泄漏),peerHost 传对端 IP(IP 不会被当 SNI 自动发送),再显式把     │
 * │ serverNames 设为 sniHost。其余从零建 socket 的重载委托默认工厂(OkHttp     │
 * │ 不会调用它们;委托以免返回 null 造成 NPE)。                                │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
class ReplaceSniSocketFactory(private val sniHost: String) : SSLSocketFactory() {

    private val delegate = getDefault() as SSLSocketFactory

    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    override fun createSocket(socket: Socket?, host: String?, port: Int, autoClose: Boolean): Socket {
        // 以对端 IP 作 peerHost:IP 不会被自动当作 SNI 发送,从而由下面的 serverNames 独占控制。
        val ip = socket!!.inetAddress.hostAddress
        return (delegate.createSocket(socket, ip, port, autoClose) as SSLSocket).apply {
            sslParameters = sslParameters.apply {
                serverNames = listOf(SNIHostName(sniHost))
            }
        }
    }

    // 以下重载 OkHttp 不会调用(它只用上面的分层重载套 TLS);委托默认工厂以免 NPE。
    override fun createSocket(host: String?, port: Int): Socket =
        delegate.createSocket(host, port)

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
        delegate.createSocket(host, port, localHost, localPort)

    override fun createSocket(address: InetAddress?, port: Int): Socket =
        delegate.createSocket(address, port)

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
        delegate.createSocket(address, port, localAddress, localPort)
}
