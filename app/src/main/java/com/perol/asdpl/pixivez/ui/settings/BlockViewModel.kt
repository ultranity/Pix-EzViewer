package com.perol.asdpl.pixivez.ui.settings

import com.perol.asdpl.pixivez.data.AppDatabase
import com.perol.asdpl.pixivez.data.entity.BlockTagEntity
import com.perol.asdpl.pixivez.services.Event
import com.perol.asdpl.pixivez.services.FlowEventBus
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object BlockViewModel{
    private val appDatabase = AppDatabase.getInstance(PxEZApp.instance)
    private var allTags: MutableSet<BlockTagEntity>? = null
    private var allTagString: MutableSet<String>? = null
    /*private val initTask: Job
    init {
        initTask = CoroutineScope(Dispatchers.IO).launch{
            getAllTags()
        }
    }*/
    fun getBlockTagString(): Set<String> {
        return allTagString ?: run {
            runBlocking(Dispatchers.IO) { fetchAllTags() }
            allTagString!!
        }
    }

    suspend fun getAllTags(): Set<BlockTagEntity> = allTags ?: fetchAllTags()

    suspend fun fetchAllTags() = withContext(Dispatchers.IO) {
        allTags = HashSet(appDatabase.blockTagDao().getAllTags())
        allTagString = HashSet(allTags!!.map { it.name })
        allTags!!
    }

    suspend fun deleteBlockTag(blockTagEntity: BlockTagEntity) = withContext(Dispatchers.IO) {
        appDatabase.blockTagDao().deleteTag(blockTagEntity)
        allTags?.remove(blockTagEntity)
        allTagString?.remove(blockTagEntity.name)?.apply {
            FlowEventBus.post(Event.BlockTagsChanged(allTagString!!))
        }
    }

    suspend fun insertBlockTag(blockTagEntity: BlockTagEntity) = withContext(Dispatchers.IO) {
        appDatabase.blockTagDao().insert(blockTagEntity)
        allTags?.add(blockTagEntity)//.name)
        allTagString?.add(blockTagEntity.name)?.apply {
            FlowEventBus.post(Event.BlockTagsChanged(allTagString!!))
        }
    }
}
