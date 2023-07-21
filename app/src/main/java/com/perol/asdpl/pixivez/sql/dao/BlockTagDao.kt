package com.perol.asdpl.pixivez.sql.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.perol.asdpl.pixivez.sql.entity.BlockTagEntity

@Dao
abstract class BlockTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(query: BlockTagEntity)

    @Query("SELECT * FROM blockTag")
    abstract suspend fun getAllTags(): MutableList<BlockTagEntity>

    @Delete
    abstract suspend fun deleteTag(blockTagEntity: BlockTagEntity)
}
