package com.perol.asdpl.pixivez.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.perol.asdpl.pixivez.data.entity.BlockTagEntity

@Dao
abstract class BlockTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(query: BlockTagEntity)

    @Query("SELECT * FROM blockTag ORDER BY id DESC")
    abstract suspend fun getAll(): MutableList<BlockTagEntity>

    @Delete
    abstract suspend fun delete(blockTagEntity: BlockTagEntity)
}
