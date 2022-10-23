package com.perol.asdpl.pixivez.sql.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
class UserEntity(
    var userid: Long,
    var username: String,
    var useremail: String,
    var ispro: Boolean,
    var userimage: String,
    var Device_token: String,
    var Refresh_token: String,
    var Authorization: String,
    @PrimaryKey(autoGenerate = true)
    var Id: Long = 0
)