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

package com.perol.asdpl.pixivez.repository

import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.sql.AppDatabase
import com.perol.asdpl.pixivez.sql.entity.BlockTagEntity
import com.perol.asdpl.pixivez.sql.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppDataRepository {
    private val appDatabase = AppDatabase.getInstance(PxEZApp.instance)
    val pre: UserInfoSharedPreferences =
        UserInfoSharedPreferences.getInstance()!! // PreferenceManager.getDefaultSharedPreferences(PxEZApp.instance)!!
    lateinit var currentUser: UserEntity
    suspend fun getUser(): UserEntity? {
        val result = withContext(Dispatchers.IO) {
            appDatabase.userDao().getUsers()
        }
        if (result.isEmpty()) {
            return null
        }
        currentUser = if (result.size == 1) {
            result[0]
        }
        else {
            val num = pre.getInt("usernum", 0)
            if ((result.size <= num)) {
                result[0]
            }
            else {
                result[num]
            }
        }
        return currentUser
    }

    suspend fun getAllUser(): List<UserEntity> {
        return withContext(Dispatchers.IO) {
            appDatabase.userDao().getUsers()
        }
    }

    suspend fun deleteAllUser() {
        withContext(Dispatchers.IO) {
            appDatabase.userDao().deleteUsers()
            getUser()
        }
    }

    suspend fun updateUser(query: UserEntity) {
        withContext(Dispatchers.IO) {
            appDatabase.userDao().updateUser(query)
            currentUser = query
        }
    }

    suspend fun insertUser(query: UserEntity) {
        withContext(Dispatchers.IO) {
            appDatabase.userDao().insert(query)
            currentUser = query
        }
    }

    suspend fun deleteUser(query: UserEntity) {
        withContext(Dispatchers.IO) {
            appDatabase.userDao().deleteUser(query)
            getUser()
        }
    }

    suspend fun findUser(id: Long): List<UserEntity> {
        return withContext(Dispatchers.IO) {
            appDatabase.userDao().findUsers(id)
        }
    }

    suspend fun getAllBlockTags() =
        withContext(Dispatchers.IO) {
            appDatabase.blockTagDao().getAllTags()
        }

    suspend fun deleteSingleBlockTag(blockTagEntity: BlockTagEntity) =
        withContext(Dispatchers.IO) {
            appDatabase.blockTagDao().deleteTag(blockTagEntity)
        }

    suspend fun insertBlockTag(blockTagEntity: BlockTagEntity) =
        withContext(Dispatchers.IO) {
            appDatabase.blockTagDao().insert(blockTagEntity)
        }
}
