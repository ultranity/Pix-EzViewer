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
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import com.perol.asdpl.pixivez.data.dao.BlockTagDao
import com.perol.asdpl.pixivez.data.dao.BlockUserDao
import com.perol.asdpl.pixivez.data.dao.UserDao
import com.perol.asdpl.pixivez.data.entity.BlockTagEntity
import com.perol.asdpl.pixivez.data.entity.BlockUserEntity
import com.perol.asdpl.pixivez.data.entity.UserEntity

@Database(
    entities = [UserEntity::class, BlockTagEntity::class, BlockUserEntity::class],
    version = 8,
    autoMigrations = [
        AutoMigration(6, 7, AppDatabase.AutoMigration6to7::class)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    @DeleteTable("illusthistory")
    @DeleteTable("history")
    @DeleteTable("illusts")
    class AutoMigration6to7 : AutoMigrationSpec

    abstract fun userDao(): UserDao
    abstract fun blockTagDao(): BlockTagDao
    abstract fun blockUserDao(): BlockUserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private val migration7to8 = Migration(7, 8) {
            it.execSQL("ALTER TABLE `user` ADD COLUMN `x_restrict` INTEGER NOT NULL DEFAULT 0")
            it.execSQL("CREATE TABLE IF NOT EXISTS `blockUser` (`uid` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`uid`))")
            it.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_blockTag_name` ON `blockTag` (`name`)")
        }
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app.db"
            ).addMigrations(migration7to8)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
