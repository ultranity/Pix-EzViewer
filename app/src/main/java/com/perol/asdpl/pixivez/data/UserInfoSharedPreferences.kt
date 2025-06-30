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
package com.perol.asdpl.pixivez.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.perol.asdpl.pixivez.services.PxEZApp

class UserInfoSharedPreferences {
    private val sp: SharedPreferences
    private val FILE_NAME = "userinfo"

    constructor(context: Context) {
        sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    constructor() {
        sp = PxEZApp.instance.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    private object InstanceHolder {
        val instance: UserInfoSharedPreferences = UserInfoSharedPreferences() //单例模式
    }

    val all: Map<String, *>
        get() = sp.all

    fun setString(key: String?, value: String?) {
        sp.edit { putString(key, value) }
    }

    fun getString(key: String?): String? {
        return sp.getString(key, null)
    }

    fun getString(key: String?, defValue: String?): String? {
        return sp.getString(key, defValue)
    }

    fun setBoolean(key: String?, value: Boolean) {
        sp.edit { putBoolean(key, value) }
    }

    fun getBoolean(key: String?): Boolean {
        return sp.getBoolean(key, false)
    }

    fun getBoolean(key: String?, value: Boolean): Boolean {
        return sp.getBoolean(key, value)
    }

    fun setInt(key: String?, value: Int) {
        sp.edit { putInt(key, value) }
    }

    fun setIntpp(key: String?) {
        sp.edit { putInt(key, sp.getInt(key, 0)) }
    }

    fun getInt(key: String?): Int {
        return sp.getInt(key, 0)
    }

    fun getInt(key: String?, value: Int): Int {
        return sp.getInt(key, value)
    }

    fun setLong(key: String?, value: Long) {
        sp.edit().putLong(key, value).apply()
    }

    fun getLong(key: String?): Long {
        return sp.getLong(key, 0)
    }

    fun getLong(key: String?, value: Long): Long {
        return sp.getLong(key, value)
    }

    companion object {
        fun getInstance(): UserInfoSharedPreferences {
            return InstanceHolder.instance
        }
    }
}
