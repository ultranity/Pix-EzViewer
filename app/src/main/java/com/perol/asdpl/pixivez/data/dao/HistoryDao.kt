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

package com.perol.asdpl.pixivez.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.perol.asdpl.pixivez.data.entity.IllustBeanEntity
import com.perol.asdpl.pixivez.data.entity.SearchHistoryEntity

/*@Dao
abstract class DownIllustsDao {
    @Query("SELECT * FROM downillusts WHERE userid=(:userid)")
    abstract fun getDownUser(userid: Long): Flowable<List<DownIllustsEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(query: DownIllustsEntity)
    @Query("DELETE FROM downillusts WHERE userid=(:userid)")
    abstract fun deleteDownIllust(userid:Long)
}
@Dao
abstract class  DownUserDao {
    @Query("SELECT * FROM downuser")
    abstract fun getDownUser(): Flowable<List<DownUserEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(query: DownUserEntity)
    @Query("DELETE FROM downuser WHERE userid=(:userid)")
    abstract fun deleteDownUser(userid:Long)
}*/
@Dao
abstract class SearchHistoryDao {

    @Query("SELECT * FROM history")
    abstract suspend fun getSearchHistory(): List<SearchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(query: SearchHistoryEntity)

    @Query("DELETE FROM history")
    abstract suspend fun deletehistory()

    @Query("DELETE FROM history WHERE word = (:word)")
    abstract suspend fun deleteHistory(word: String)

    @Delete
    abstract suspend fun deleteHistoryEntity(searchHistoryEntity: SearchHistoryEntity)
}

@Dao
interface IllustHistoryDao {
    @Query("SELECT * FROM illusthistory")
    suspend fun getIllustHistory(): List<IllustBeanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(query: IllustBeanEntity)

    @Query("DELETE FROM illusthistory")
    suspend fun deleteHistory()

    @Query("SELECT * FROM illusthistory WHERE illustid=:illustid")
    suspend fun getHistoryOne(illustid: Long): List<IllustBeanEntity>

    @Delete
    suspend fun deleteOne(query: IllustBeanEntity)
}
