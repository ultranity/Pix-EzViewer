package com.perol.asdpl.pixivez.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// TODO: fix duplicate
@Entity(tableName = "blockTag", indices = [Index(value = ["name"], unique = true)])
class BlockTagEntity(
    // @PrimaryKey
    var name: String,
    var translateName: String,
    @PrimaryKey(autoGenerate = true)
    var Id: Int = 0
)
