package com.perol.asdpl.pixivez.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.perol.asdpl.pixivez.data.entity.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE userid=:userid")
    suspend fun findUsers(userid: Int): List<UserEntity>

    @Query("SELECT * FROM user")
    suspend fun getUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(query: UserEntity)

    @Delete
    suspend fun deleteUser(query: UserEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUser(query: UserEntity)

    @Query("DELETE FROM user")
    suspend fun deleteUsers()
}
