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

package com.perol.asdpl.pixivez.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search", indices = [androidx.room.Index(value = ["word"], unique = true)])
class SearchHistoryEntity(
    var word: String,
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
)
/*@Entity(tableName = "search")
class SearchHistoryEntity(
    @PrimaryKey val word: String,
    //@ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long = System.currentTimeMillis(),
    //@ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val modifiedAt: Long = System.currentTimeMillis()
){
    constructor(word: String):this(word, System.currentTimeMillis(), System.currentTimeMillis())
    constructor(word: String, modifiedAt: Long):this(word, System.currentTimeMillis(), modifiedAt)
}*/

@Entity(
    tableName = "history",
    primaryKeys = ["id", "isUser"]
)
class HistoryEntity(
    val id: Int,
    val title: String,
    val thumb: String,
    @ColumnInfo(defaultValue = "false")
    val isUser: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    var count: Int = 1,
    //@ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = System.currentTimeMillis()
)
/*class IncreCount(
    var count: Int = 1,
    var modifiedAt: Long = System.currentTimeMillis()
)*/
