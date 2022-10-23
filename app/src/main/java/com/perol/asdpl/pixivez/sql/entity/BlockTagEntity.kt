package com.perol.asdpl.pixivez.sql.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blockTag")
class BlockTagEntity(
    // @PrimaryKey
    var name: String,
    var translateName: String
) {
    @PrimaryKey(autoGenerate = true)
    var Id: Long = 0
}
