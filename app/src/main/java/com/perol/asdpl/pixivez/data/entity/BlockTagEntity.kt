package com.perol.asdpl.pixivez.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// TODO: fix duplicate
@Entity(tableName = "blockTag")
class BlockTagEntity(
    // @PrimaryKey
    var name: String,
    var translateName: String,
    @PrimaryKey(autoGenerate = true)
    var Id: Long = 0
)
