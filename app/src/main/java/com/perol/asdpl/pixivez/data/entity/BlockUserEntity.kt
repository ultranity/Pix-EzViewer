package com.perol.asdpl.pixivez.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// TODO: fix duplicate
@Entity(tableName = "blockUser")
class BlockUserEntity(
    @PrimaryKey
    var uid: Int,
    val createdAt: Long = System.currentTimeMillis()
)
