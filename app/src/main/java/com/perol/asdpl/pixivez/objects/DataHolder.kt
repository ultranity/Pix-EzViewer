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
    private var objectStore = WeakValueLinkedHashMap<Int, T>(32)

    @Synchronized
    fun getAll(): MutableList<T> {
        return objectStore.values.asMutableList()
    }

    @Synchronized
    fun get(id: Int): T? {
        return objectStore[id]
    }

    @Synchronized
    open fun update(id: Int, t: T): T {
        // Hold a strong reference throughout the merge; the weak value can disappear
        // between a containsKey check and a second lookup.
        val cached = objectStore[id]
        if (cached != null) {
            if (cached !== t) cached.copyFrom(t)
            return cached
        }
        objectStore[id] = t
        return t
    }

    @Synchronized
    open fun clear() {
        // Replace the store, including its weak-reference queue and iteration chain.
        objectStore = WeakValueLinkedHashMap(32)
    }

    @Synchronized
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

/** A successful local action outranks unversioned API/navigation snapshots in this session. */
object IllustCacheRepo : CacheRepo<Illust>() {
    class BookmarkMutation internal constructor(internal val session: Long, internal val order: Long)
    private data class ConfirmedBookmark(val value: Boolean, val order: Long)

    private val confirmedBookmarks = HashMap<Int, ConfirmedBookmark>()
    private var accountId: Int? = null
    private var session = 0L
    private var mutationOrder = 0L

    @Synchronized
    fun activateAccount(id: Int?) {
        if (accountId != id) {
            clear()
            accountId = id
        }
    }

    @Synchronized
    fun beginBookmarkMutation() = BookmarkMutation(session, ++mutationOrder)

    /** Call only after the server accepted the action, never on an optimistic tap. */
    @Synchronized
    fun confirmBookmark(item: Illust, value: Boolean, mutation: BookmarkMutation): Boolean {
        if (mutation.session != session) return false
        val previous = confirmedBookmarks[item.id]
        if (previous != null && previous.order > mutation.order) return false
        confirmedBookmarks[item.id] = ConfirmedBookmark(value, mutation.order)
        item.is_bookmarked = value
        val cached = get(item.id)
        if (cached == null) super.update(item.id, item) else cached.is_bookmarked = value
        return true
    }

    @Synchronized
    override fun update(id: Int, t: Illust): Illust {
        confirmedBookmarks[id]?.let { t.is_bookmarked = it.value }
        return super.update(id, t)
    }

    @Synchronized
    override fun clear() {
        super.clear()
        confirmedBookmarks.clear()
        // Ignore late successful callbacks from an account that is no longer active.
        session++
    }
}

//class UserDetailCacheRepo: CacheRepo<UserDetail>() {}
object UserCacheRepo : CacheRepo<User>()