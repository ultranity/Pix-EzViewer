package com.perol.asdpl.pixivez.sql.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
class UserEntity(
    @ColumnInfo(name = "userimage")
    var userimage: String,
    @ColumnInfo(name = "userid")
    var userid: Long,
    @ColumnInfo(name = "username")
    var username: String,
    @ColumnInfo(name = "useremail")
    var useremail: String,
    @ColumnInfo(name = "ispro")
    var ispro: Boolean,
    @ColumnInfo
    var Device_token: String,
    @ColumnInfo
    var Refresh_token: String,
    @ColumnInfo
    var Authorization: String
) {
    @PrimaryKey(autoGenerate = true)
    var Id: Long = 0
}