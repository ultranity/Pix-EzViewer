package com.perol.asdpl.pixivez.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.perol.asdpl.pixivez.data.entity.UserEntity

@Dao
abstract class UserDao {
    @Query("SELECT * FROM user WHERE userid=:userid")
    abstract fun findUsers(userid: Long): List<UserEntity>

    @Query("SELECT * FROM user")
    abstract fun getUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(query: UserEntity)

    @Delete
    abstract fun deleteUser(query: UserEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateUser(query: UserEntity)

    @Query("DELETE FROM user")
    abstract fun deleteUsers()
}
