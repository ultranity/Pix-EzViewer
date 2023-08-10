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
import androidx.room.Update
import com.perol.asdpl.pixivez.data.entity.HistoryEntity
import com.perol.asdpl.pixivez.data.entity.SearchHistoryEntity

/*@Dao
abstract class DownloadHistoryDao {
    @Query("SELECT * FROM download WHERE pid=(:pid)")
    abstract fun getDownloads(pid: Long): List<IllustsEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(query: IllustsEntity)
    @Query("DELETE FROM download WHERE pid=(:pid)")
    abstract fun delete(pid:Long)
}*/

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search")
    suspend fun getSearchHistory(): List<SearchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(query: SearchHistoryEntity)

    suspend fun insert(word: String) {
        insert(SearchHistoryEntity(word))
    }
    /*class UpdateSearchHistory(val word:String, val modifiedAt:Long)
    @Update(entity = SearchHistoryEntity::class)
    abstract suspend fun update(query: UpdateSearchHistory)
    suspend fun update(word: String){
        update(UpdateSearchHistory(word, System.currentTimeMillis()))
    }*/

    @Query("DELETE FROM search")
    suspend fun clear()

    @Query("DELETE FROM search WHERE word = (:word)")
    suspend fun deleteHistory(word: String)

    //@Delete(entity = SearchHistoryEntity::class)
    //suspend fun deleteHistory(word: String)
}

@Dao
interface ViewHistoryDao {
    @Query("SELECT * FROM history ORDER BY modifiedAt DESC")
    suspend fun getViewHistory(): List<HistoryEntity>

    @Query("SELECT * FROM history where id=(:id) and isUser=(:isUser) LIMIT 1")
    suspend fun getEntity(id: Long, isUser: Boolean = false): HistoryEntity?

    //@Query("SELECT * FROM history where id=(:id) and NOT user LIMIT 1")
    //suspend fun getIllust(id: Long): IllustEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(query: HistoryEntity)

    @Query("INSERT INTO history (id,title,thumb,isUser) VALUES (:id,:title,:thumb,:isUser)")
    suspend fun insert(id: Long, title: String, thumb: String, isUser: Boolean = false)

    @Update(entity = HistoryEntity::class)
    suspend fun update(item: HistoryEntity)

    suspend fun increment(item: HistoryEntity) {
        update(item.apply {
            count++
            modifiedAt = System.currentTimeMillis()
        })
    }

    @Delete
    suspend fun delete(query: HistoryEntity)

    //@Delete(entity = HistoryEntity::class)
    @Query("DELETE FROM history WHERE id=(:id) and isUser=(:isUser)")
    suspend fun delete(id: Long, isUser: Boolean)

    @Query("DELETE FROM history")
    suspend fun clear()
}
