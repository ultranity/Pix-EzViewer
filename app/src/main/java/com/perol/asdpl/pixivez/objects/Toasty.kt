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

package com.perol.asdpl.pixivez.objects

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.Toast
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

fun Toast.showInMain() {
    CoroutineScope(Dispatchers.Main).launch {
        this@showInMain.show()
    }
}


object ToastQ {
    data class ToastInfo(val content: String, val duration: Int, val context: String)

    private val queue = MutableSharedFlow<ToastInfo>(
        extraBufferCapacity = Int.MAX_VALUE //避免挂起导致数据发送失败
    )

    @SuppressLint("StaticFieldLeak")
    private val lToast: Toast =
        Toast.makeText(PxEZApp.instance, "", Toast.LENGTH_SHORT)

    init {
        CoroutineScope(Dispatchers.Main).launch {
            queue.collect {
                //TODO: it.duration
                lToast.setText(it.content)
                lToast.show()
            }
        }
    }

    fun post(
        stringId: Int, duration: Int = Toast.LENGTH_SHORT, context: String = "", delay: Long = 0,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(delay)
            queue.emit(ToastInfo(PxEZApp.instance.getString(stringId), duration, context))
        }
    }

    fun post(
        content: String, duration: Int = Toast.LENGTH_SHORT, context: String = "", delay: Long = 0,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(delay)
            queue.emit(ToastInfo(content, duration, context))
        }
    }
}

object Toasty {
    private val refreshToast =
        Toast.makeText(PxEZApp.instance, R.string.token_expired, Toast.LENGTH_SHORT)

    fun tokenRefreshing() {
        refreshToast.show()
    }

    fun tokenRefreshed(e: Exception? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            refreshToast.cancel()
            if (e == null)
                info(PxEZApp.instance, R.string.refresh_token)
            else
                info(
                    PxEZApp.instance,
                    PxEZApp.instance.getString(R.string.refresh_token_fail) + ":" + e.message,
                )

        }
    }

    fun success(context: Context, stringId: Int, length: Int = Toast.LENGTH_SHORT) {
        return Toast.makeText(context, context.getText(stringId), length).show()
    }

    fun error(context: Context, stringId: Int, length: Int = Toast.LENGTH_LONG) {
        return Toast.makeText(context, context.getText(stringId), length).show()
    }

    fun info(context: Context, stringId: Int, length: Int = Toast.LENGTH_SHORT) {
        return Toast.makeText(context, context.getText(stringId), length).show()
    }

    fun warning(context: Context, stringId: Int, length: Int = Toast.LENGTH_LONG) {
        return Toast.makeText(context, context.getText(stringId), length).show()
    }

    fun normal(context: Context, stringId: Int, length: Int = Toast.LENGTH_SHORT) {
        return Toast.makeText(context, context.getText(stringId), length).show()
    }

    fun success(context: Context, string: String, length: Int = Toast.LENGTH_SHORT) {
        return Toast.makeText(context, string, length).show()
    }

    fun error(context: Context, string: String, length: Int = Toast.LENGTH_LONG) {
        return Toast.makeText(context, string, length).apply { setGravity(Gravity.CENTER, 0, 0) }
            .show()
    }

    fun info(context: Context, string: String, length: Int = Toast.LENGTH_SHORT) {
        return Toast.makeText(context, string, length).show()
    }

    fun warning(context: Context, string: String, length: Int = Toast.LENGTH_LONG) {
        return Toast.makeText(context, string, length).show()
    }

    fun normal(context: Context, string: String, length: Int = Toast.LENGTH_SHORT) {
        return Toast.makeText(context, string, length).show()
    }
}
