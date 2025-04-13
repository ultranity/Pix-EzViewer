package com.perol.asdpl.pixivez.ui.settings

import com.perol.asdpl.pixivez.data.AppDatabase
import com.perol.asdpl.pixivez.data.entity.BlockTagEntity
import com.perol.asdpl.pixivez.data.entity.BlockUserEntity
import com.perol.asdpl.pixivez.services.Event
import com.perol.asdpl.pixivez.services.FlowEventBus
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object BlockViewModel{
    private val appDatabase = AppDatabase.getInstance(PxEZApp.instance)
    private var allTags: MutableList<BlockTagEntity>? = null
    private var allTagString: MutableSet<String>? = null
    private var allUIDs: MutableSet<Int>? = null

    fun getBlockTagString(): Set<String> {
        return allTagString ?: run {
            runBlocking(Dispatchers.IO) { fetchAllTags() }
            allTagString!!
        }
    }

    suspend fun fetchAllTags() = withContext(Dispatchers.IO) {
        allTags = appDatabase.blockTagDao().getAll()
        allTagString = HashSet(allTags!!.map { it.name })
        allTags!!
    }

    suspend fun deleteBlockTag(blockTagEntity: BlockTagEntity) = withContext(Dispatchers.IO) {
        appDatabase.blockTagDao().delete(blockTagEntity)
        allTags?.remove(blockTagEntity)
        allTagString?.remove(blockTagEntity.name)?.apply {
            FlowEventBus.post(Event.BlockTagsChanged(allTagString!!, blockTagEntity.name, false))
        }
    }

    suspend fun insertBlockTag(blockTagEntity: BlockTagEntity) = withContext(Dispatchers.IO) {
        appDatabase.blockTagDao().insert(blockTagEntity)
        allTags?.add(blockTagEntity)//.name)
        allTagString?.add(blockTagEntity.name)?.apply {
            FlowEventBus.post(Event.BlockTagsChanged(allTagString!!, blockTagEntity.name, true))
        }
    }

    fun getBlockUIDs(): Set<Int> {
        return allUIDs ?: run {
            runBlocking(Dispatchers.IO) { fetchAllUIDs() }
            allUIDs!!
        }
    }

    suspend fun fetchAllUIDs(): Set<Int> = withContext(Dispatchers.IO) {
        allUIDs = HashSet(appDatabase.blockUserDao().getAll().map { it.uid })
        allUIDs!!
    }

    suspend fun deleteBlockUser(uid: Int) = withContext(Dispatchers.IO) {
        appDatabase.blockUserDao().delete(BlockUserEntity(uid))
        allUIDs?.apply {
            remove(uid)
            FlowEventBus.post(Event.BlockUsersChanged(allUIDs!!, uid, false))
        }
    }

    suspend fun insertBlockUser(uid: Int) = withContext(Dispatchers.IO) {
        appDatabase.blockUserDao().insert(BlockUserEntity(uid))
        allUIDs?.apply {
            add(uid)
            FlowEventBus.post(Event.BlockUsersChanged(allUIDs!!, uid, true))
        }
    }

    fun export(): Map<String, Set<Any>> {
        return mapOf("blockTags" to getBlockTagString(), "blockUIDs" to getBlockUIDs())
    }
}
