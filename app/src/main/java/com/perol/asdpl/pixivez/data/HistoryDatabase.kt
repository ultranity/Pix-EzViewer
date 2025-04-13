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

package com.perol.asdpl.pixivez.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.perol.asdpl.pixivez.data.dao.SearchHistoryDao
import com.perol.asdpl.pixivez.data.dao.ViewHistoryDao
import com.perol.asdpl.pixivez.data.entity.HistoryEntity
import com.perol.asdpl.pixivez.data.entity.SearchHistoryEntity
import com.perol.asdpl.pixivez.services.PxEZApp

@Database(
    entities = [SearchHistoryEntity::class, HistoryEntity::class],
    version = 2
)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao

    //abstract fun downloadHistoryDao(): DownloadHistoryDao
    abstract fun viewHistoryDao(): ViewHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        fun getInstance(context: Context): HistoryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private val migration1to2 = Migration(1, 2) {
            it.execSQL("ALTER TABLE search RENAME COLUMN Id TO id")
            it.execSQL("DELETE FROM search WHERE id NOT IN (SELECT MIN(id) FROM search GROUP BY word)")
            it.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_word` ON `search` (`word`)")
        }
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                HistoryDatabase::class.java,
                "history.db"
            ).addMigrations(migration1to2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}

object HistoryDataRepo {
    private val appDatabase = HistoryDatabase.getInstance(PxEZApp.instance)
    suspend fun getSearchHistory(): List<SearchHistoryEntity> =
        appDatabase.searchHistoryDao().getAll()

    suspend fun insertSearchHistory(word: String) = appDatabase.searchHistoryDao().insert(word)
    suspend fun deleteSearchHistory(word: String) = appDatabase.searchHistoryDao().delete(word)
    suspend fun deleteAllSearchHistory() = appDatabase.searchHistoryDao().clear()

    suspend fun getViewHistory(): List<HistoryEntity> = appDatabase.viewHistoryDao().getAll()
    suspend fun insertViewHistory(historyEntity: HistoryEntity) =
        appDatabase.viewHistoryDao().insert(historyEntity)

    suspend fun deleteViewHistory(historyEntity: HistoryEntity) =
        appDatabase.viewHistoryDao().delete(historyEntity)

    suspend fun deleteAllViewHistory() = appDatabase.viewHistoryDao().clear()
}