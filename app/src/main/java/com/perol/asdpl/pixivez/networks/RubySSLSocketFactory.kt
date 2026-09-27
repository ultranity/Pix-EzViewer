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

import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

/** Suppresses automatic SNI while reusing OkHttp's connected socket and timeout. */
class RubySSLSocketFactory(
    private val delegate: SSLSocketFactory = getDefault() as SSLSocketFactory,
) : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    override fun createSocket(socket: Socket?, host: String?, port: Int, autoClose: Boolean): Socket {
        requireNotNull(socket)
        // A literal peer address avoids implicit SNI. OkHttp still verifies the URL hostname.
        // Do not createSocket(address, port): that opens a second, unbounded TCP connection.
        return delegate.createSocket(socket, socket.inetAddress.hostAddress, port, autoClose)
    }

    override fun createSocket(host: String?, port: Int): Socket =
        delegate.createSocket(InetAddress.getByName(host), port)

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
        delegate.createSocket(InetAddress.getByName(host), port, localHost, localPort)

    override fun createSocket(address: InetAddress?, port: Int): Socket =
        delegate.createSocket(address, port)

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
        delegate.createSocket(address, port, localAddress, localPort)
}
