/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package com.perol.asdpl.pixivez.objects

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager.widget.PagerAdapter
import com.perol.asdpl.pixivez.base.KotlinUtil.asMutableList
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.data.model.User
import java.util.Stack
import java.util.Timer
import kotlin.collections.set
import kotlin.concurrent.schedule

/**
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

 * copy member from another instance
fun <T : Any> T.copyFrom(src:T) {
    //if (!this::class.isData) {
    //    return
    //}
    this::class.memberProperties
        .filterIsInstance<KMutableProperty<*>>()
        .forEach {
            it.setter.call(this, it.getter.call(src))
        }
return
this.javaClass.declaredFields
//.filter{ it.modifiers == Modifier.PUBLIC }
.forEach {
it.isAccessible = true
it.set(this, it.get(src))
}
}
 */

interface CopyFrom<T> {
    fun copyFrom(src: T)
}

class DataHolder {
    companion object {
        private var illustListStack: Stack<List<Illust>?> = Stack<List<Illust>?>()
        var picPagerAdapter: PagerAdapter? = null

        fun peekIllustList(): List<Illust>? {
            return if (this.illustListStack.empty()) {
                null
            } else {
                this.illustListStack.peek()
            }
        }

        fun checkIllustList(pos: Int, id: Int): Boolean {
            return if (this.illustListStack.empty()) {
                false
            } else {
                (this.illustListStack.peek()?.get(pos)?.id ?: -1) == id
            }
        }

        // --------DownloadFragment tmp ref---------
        var nextUrlRef: String? = null
        var dataListRef: MutableList<Illust>? = null

        //var dataAddedRef: MutableList<Illust>? = null
        // -----------------
        fun getIllustList(): List<Illust>? {
            return if (this.illustListStack.empty()) {
                null
            } else {
                this.illustListStack.pop()
            }
        }

        fun setIllustList(illustList: List<Illust>) {
            this.illustListStack.push(illustList)
        }
    }
}

val HoldingData = HashMap<String, DataStore<*>>()

@Deprecated("use CacheRepo instead")
class DataStore<T>(private val key: String, private val clearBindDelay: Long = 2000) {
    companion object {
        inline fun <reified T> save(id: String, data: T): DataStore<T> {
            val ds = DataStore<T>(id).also { it.data = data }
            HoldingData[id] = ds
            return ds
        }

        /*
        inline fun <reified T : Any> update(id: String, data: T):T? {
            return (HoldingData[id]?.data as? T)?.apply{ copyFrom(data) }
        }*/

        /**
         * update data if exists and return the holding data object
         */
        fun update(id: String, data: User): User? {
            return (HoldingData[id]?.data as? User)?.apply { copyFrom(data) }
        }

        inline fun <reified T> retrieve(id: String): T? {
            return HoldingData[id]?.data as? T
        }

        fun register(id: String, host: LifecycleOwner) {
            HoldingData[id]?.register(host)
        }
    }

    var data: T? = null
    private val bindTargets = ArrayList<LifecycleOwner>()

    fun register(host: LifecycleOwner) {
        if (!bindTargets.contains(host)) {
            bindTargets.add(host)
            host.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        host.lifecycle.removeObserver(this)
                        bindTargets.remove(host)
                        Timer().schedule(clearBindDelay) {
                            if (bindTargets.isEmpty()) { // 如果当前没有关联对象，则释放资源
                                data = null
                                HoldingData.remove(key)
                            }
                        }
                    }
                }
            })
        }
    }
}

/*TODO: LRU cache
class MetricWrapper<T : CopyFrom<T>>(
    val id: Int,
    val obj: T
) {
    var referer = 0
    var needs_update = false
}*/

open class CacheRepo<T : CopyFrom<T>> {
    private val objectStore = WeakValueLinkedHashMap<Int, T>(32)

    fun getAll(): MutableList<T> {
        return objectStore.values.asMutableList()
    }

    fun get(id: Int): T? {
        return objectStore[id]
    }

    fun update(id: Int, t: T): T {
        if (objectStore.containsKey(id))
            objectStore[id]!!.copyFrom(t)
        else
            objectStore[id] = t
        return objectStore[id]!!
    }

    fun remove(id: Int) {
        objectStore.remove(id)
    }

    fun loadFromDisk() {
        //TODO:
    }

    fun dumpToDisk() {
        //TODO:
    }
}

object IllustCacheRepo : CacheRepo<Illust>() {
    /* TODO: clean binders automatically
    internal const val clearBindDelay: Long = 1000
    internal val bindTargets = ArrayList<LifecycleOwner>()
    val objIDStores = HashMap<String, Collection<Illust>>()
    //val bindingListener = HashMap<Int, ArrayList<Unit>>()
    fun register(host: LifecycleOwner, mData: Collection<Illust>) {
        if (!bindTargets.contains(host)) {
            bindTargets.add(host)
            objIDStores[host.hashCode().toString()] = mData
            host.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        host.lifecycle.removeObserver(this)
                        bindTargets.remove(host)
                        Timer().schedule(clearBindDelay) {
                            if (bindTargets.isEmpty()) { // 如果当前没有关联对象
                                //check if is restoring: https://blog.csdn.net/huweijian5/article/details/114575986
                                if (isChangingConfigurations(source))
                                    return@schedule
                                objIDStores.remove(host.hashCode().toString())
                            }
                        }
                    }
                }
            })
        }
    }*/
}

//class UserDetailCacheRepo: CacheRepo<UserDetail>() {}
object UserCacheRepo : CacheRepo<User>()