/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
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

import android.content.res.Resources
import android.util.TypedValue

const val MEDIUM_SCREEN_WIDTH_SIZE = 600
const val LARGE_SCREEN_WIDTH_SIZE = 1240
val Int.dp: Int get() = toFloat().dp.toInt()

val Float.dp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )

fun dp2px(dpValue: Float): Int {
    return (
            0.5f + dpValue * Resources.getSystem()
                .displayMetrics.density
            ).toInt()
}

fun dp2px(dpValue: Int): Int {
    return (
            0.5f + dpValue * Resources.getSystem()
                .displayMetrics.density
            ).toInt()
}

fun px2dp(pxValue: Int): Float {
    return pxValue / Resources.getSystem().displayMetrics.density
}

fun getMaxColumn(widthDp: Int): Int {
    return 1.coerceAtLeast(Resources.getSystem().configuration.screenWidthDp / widthDp)
}

fun screenWidthDp() = Resources.getSystem().configuration.screenWidthDp

fun screenWidthPx() = Resources.getSystem().displayMetrics.widthPixels

fun screenHeightDp() = Resources.getSystem().configuration.screenHeightDp

fun screenHeightPx() = Resources.getSystem().displayMetrics.heightPixels
