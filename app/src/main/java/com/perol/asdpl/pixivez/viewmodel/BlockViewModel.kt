package com.perol.asdpl.pixivez.viewmodel

import androidx.lifecycle.MutableLiveData
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.sql.AppDatabase
import com.perol.asdpl.pixivez.sql.entity.BlockTagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*

object BlockViewModel{
    private val appDatabase = AppDatabase.getInstance(PxEZApp.instance)
    var allTags:MutableLiveData<LinkedList<BlockTagEntity>>? = null
    private var allTagString:LinkedList<String>? = null
    /*private val initTask: Job
    init {
        initTask = CoroutineScope(Dispatchers.IO).launch{
            getAllTags()
        }
    }*/
    fun getBlockTagString(): List<String> {
        return allTagString?:
        runBlocking(Dispatchers.IO){ fetchAllTags() }.map { it.name }.also { allTagString = LinkedList(it) }
    }
    fun getBlockTags(): List<String> {
        return (allTags?.value?:runBlocking(Dispatchers.IO){ fetchAllTags() }).map { it.name }
    }
    suspend fun getAllTags() = withContext(Dispatchers.IO) {
        //if (initTask.isActive)
        //    initTask.join()
        (allTags?.value?:fetchAllTags()).map{ it.name }
    }
    suspend fun fetchAllTags() = withContext(Dispatchers.IO) {
        allTags = MutableLiveData(LinkedList(appDatabase.blockTagDao().getAllTags()))
        allTagString = LinkedList(allTags!!.value!!.map { it.name })
        allTags!!.value!!
    }

    suspend fun deleteSingleTag(blockTagEntity: BlockTagEntity) = withContext(Dispatchers.IO) {
        appDatabase.blockTagDao().deleteTag(blockTagEntity)
        allTags!!.value!!.remove(blockTagEntity)
        allTagString!!.remove(blockTagEntity.name)
    }

    suspend fun insertBlockTag(blockTagEntity: BlockTagEntity) = withContext(Dispatchers.IO) {
        appDatabase.blockTagDao().insert(blockTagEntity)
        allTags!!.value!!.addFirst(blockTagEntity)//.name)
        allTagString!!.addFirst(blockTagEntity.name)
    }
}
