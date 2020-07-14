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

import com.perol.asdpl.pixivez.responses.Illust
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set

class DataHolder {
    companion object{
        private var illustsList: Stack<ArrayList<Illust>?> = Stack<ArrayList<Illust>?>()

        fun getIllustsList(): ArrayList<Illust>? {
            return if (this.illustsList.empty()) null
                     else this.illustsList.pop()
        }

        fun setIllustsList(illustList: ArrayList<Illust>) {
            this.illustsList.push(illustList)
        }
    }

    var data: MutableMap<String, WeakReference<Any>> =
        HashMap<String, WeakReference<Any>>()

    fun save(id: String, `object`: Any?) {
        data[id] = WeakReference<Any>(`object`)
    }

    fun retrieve(id: String): Any? {
        val objectWeakReference: WeakReference<Any>? = data[id]
        return objectWeakReference?.get()
    }
}