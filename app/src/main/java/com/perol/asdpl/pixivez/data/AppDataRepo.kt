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

import com.perol.asdpl.pixivez.data.entity.UserEntity
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.runBlocking

object AppDataRepo {
    private val appDatabase = AppDatabase.getInstance(PxEZApp.instance)
    val pre: UserInfoSharedPreferences = UserInfoSharedPreferences.getInstance()
    private var _currentUser: UserEntity? = null
    val currentUser: UserEntity
        get() = _currentUser!!

    fun setCurrentUser(user: UserEntity) {
        _currentUser = user
    }

    fun userInited() = _currentUser != null

    fun isSelfPage(id: Int): Boolean {
        return currentUser.userid == id
    }

    suspend fun getUser(): UserEntity? {
        val result = appDatabase.userDao().getUsers()
        if (result.isEmpty()) {
            return null
        }
        _currentUser = if (result.size == 1) {
            result[0]
        } else {
            val num = pre.getInt("usernum", 0)
            if ((result.size <= num)) {
                result[0]
            } else {
                result[num]
            }
        }
        return currentUser
    }

    suspend fun getAllUser(): List<UserEntity> = appDatabase.userDao().getUsers()

    suspend fun deleteAllUser() {
        appDatabase.userDao().deleteUsers()
        getUser()
    }

    suspend fun updateUser(query: UserEntity) {
        appDatabase.userDao().updateUser(query)
        _currentUser = query
    }

    suspend fun insertUser(query: UserEntity) {
        appDatabase.userDao().insert(query)
        _currentUser = query
    }

    suspend fun deleteUser(query: UserEntity) {
        appDatabase.userDao().deleteUser(query)
        getUser()
    }

    suspend fun findUser(id: Int): List<UserEntity> = appDatabase.userDao().findUsers(id)

    fun export() {
        return runBlocking {
            mutableMapOf("users" to getAllUser(), "pre" to pre.all)
        }
    }
}