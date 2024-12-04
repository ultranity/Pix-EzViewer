package com.perol.asdpl.pixivez.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.perol.asdpl.pixivez.data.entity.BlockUserEntity

@Dao
abstract class BlockUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(query: BlockUserEntity)

    @Query("SELECT * FROM blockUser ORDER BY createdAt DESC")
    abstract suspend fun getAll(): MutableList<BlockUserEntity>

    @Delete
    abstract suspend fun delete(blockUserEntity: BlockUserEntity)
}
