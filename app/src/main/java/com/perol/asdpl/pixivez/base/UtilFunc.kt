package com.perol.asdpl.pixivez.base

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.perol.asdpl.pixivez.base.KotlinUtil.Int
import com.perol.asdpl.pixivez.data.model.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import org.roaringbitmap.RoaringBitmap
import java.util.BitSet
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.pow

object UtilFunc {
    fun <T> firstCommon(c1: Collection<T>, c2: Collection<T>): T? {
        // The collection to be used for contains(). Preference is given to
        // the collection who's contains() has lower O() complexity.
        var contains = c2
        // The collection to be iterated. If the collections' contains() impl
        // are of different O() complexity, the collection with slower
        // contains() will be used for iteration. For collections who's
        // contains() are of the same complexity then best performance is
        // achieved by iterating the smaller collection.
        var iterate = c1

        // Performance optimization cases. The heuristics:
        //   1. Generally iterate over c1.
        //   2. If c1 is a Set then iterate over c2.
        //   3. If either collection is empty then result is always true.
        //   4. Iterate over the smaller Collection.
        if (c1 is Set<*>) {
            // Use c1 for contains as a Set's contains() is expected to perform
            // better than O(N/2)
            iterate = c2
            contains = c1
        } else if (c2 !is Set<*>) {
            // Both are mere Collections. Iterate over smaller collection.
            // Example: If c1 contains 3 elements and c2 contains 50 elements and
            // assuming contains() requires ceiling(N/2) comparisons then
            // checking for all c1 elements in c2 would require 75 comparisons
            // (3 * ceiling(50/2)) vs. checking all c2 elements in c1 requiring
            // 100 comparisons (50 * ceiling(3/2)).
            val c1size = c1.size
            val c2size = c2.size
            if (c1size == 0 || c2size == 0) {
                // At least one collection is empty. Nothing will match.
                return null
            }
            if (c1size > c2size) {
                iterate = c2
                contains = c1
            }
        }
        for (e in iterate) {
            if (contains.contains(e)) {
                // Found a common element. Collections are not disjoint.
                return e
            }
        }

        // No common elements were found.
        return null
    }

    fun firstCommonTags(c1: Collection<String>, tags: List<Tag>): Tag? {
        val c1size = c1.size
        val c2size = tags.size
        if (c1size == 0 || c2size == 0) {
            // At least one collection is empty. Nothing will match.
            return null
        }
        for (t in tags) {
            val e = t.name
            if (c1.contains(e)) {
                // Found a common element. Collections are not disjoint.
                return t
            }
        }

        // No common elements were found.
        return null
    }

    fun FloatingActionButton.setMargins(
        ref: FloatingActionButton,
        extra: (CoordinatorLayout.LayoutParams) -> Unit = {}
    ) {
        layoutParams = (layoutParams as CoordinatorLayout.LayoutParams).apply {
            val refLayout = ref.layoutParams as CoordinatorLayout.LayoutParams
            anchorId = refLayout.anchorId
            anchorGravity = refLayout.anchorGravity
            setMargins(ref.marginLeft, ref.marginTop, ref.marginRight, ref.marginBottom)
            extra(this)
        }
    }

    fun View.rotate() {
        rotation = 0F
        animate().rotation(360F)
            .withLayer()
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    fun BitSet.forEach(block: (it: Int) -> Unit) {
        var i = nextSetBit(0)
        while (i >= 0) {
            block(i)
            i = nextSetBit(i)
        }
    }
    fun BitSet.forEachNot(block: (it: Int) -> Unit) {
        var i = nextClearBit(0)
        while (i >= 0) {
            block(i)
            i = nextClearBit(i)
        }
    }

    fun BitSet.forEachIndexed(block: (it: Int, index: Int) -> Unit) {
        var i = nextSetBit(0)
        var index = 0
        while (i >= 0) {
            block(i, index)
            i = nextSetBit(i + 1)
            index++
        }
    }

    fun BitSet.forEachNotIndexed(block: (it: Int, index: Int) -> Unit) {
        var i = nextClearBit(0)
        var index = 0
        var size = size
        while (i < size) {
            block(i, index)
            i = nextClearBit(i + 1)
            index++
        }
    }

    fun BitSet.previousCount(index: Int): Int {
        var count = get(index).Int()
        var i = previousSetBit(index) - 1
        while (i >= 0) {
            count++
            i = previousSetBit(i) - 1
        }
        return count
    }

    fun BitSet.getPosition(index: Int): Int {
        var i = nextSetBit(0)
        for (j in (index - 1).downTo(0)) {
            i = nextSetBit(i + 1)
        }
        return i
    }

    fun <T> BitSet.mapIndexed(block: (it: Int) -> T): List<T> {
        val list = mutableListOf<T>()
        forEach {
            list.add(block(it))
        }
        return list
    }
    val BitSet.first
        get() = nextSetBit(0)
    val BitSet.last
        get() = previousSetBit(size())
    val BitSet.size: Int
        get() = cardinality()
    inline fun BitSet.compute(block: BitSet.() -> Unit): BitSet {
        return (this.clone() as BitSet).apply(block)
    }

    fun BitSet.flip(): BitSet {
        flip(0, size)
        return this
    }
    val RoaringBitmap.size: Int
        get() = cardinality

    fun RoaringBitmap.set(x: Int, status: Boolean) {
        if (status)
            add(x)
        else
            remove(x)
    }
}

object KotlinUtil {
    inline fun <T> T.transformIf(condition: Boolean, block: T.() -> T): T {
        return if (condition) block() else this
    }

    fun <T> Collection<T>.asMutableList(): MutableList<T> {
        return when (this) {
            is java.util.ArrayList -> {
                this
            }

            is MutableList -> {
                this
            }

            else -> {
                this.toMutableList()
            }
        }
    }

    fun Boolean.Int(): Int = if (this) 1 else 0
    fun Boolean.alsoIf(block: () -> Unit): Boolean {
        if (this) block()
        return this
    }
    operator fun Boolean.times(i: Int): Int = if (this) i else 0
    operator fun Boolean.plus(i: Int): Int = if (this) i + 1 else i
    operator fun Int.times(b: Boolean): Int = if (b) this else 0
    operator fun Int.plus(b: Boolean): Int = if (b) this + 1 else this

    suspend inline fun <T : Any> launchCatching(
        block: suspend() -> T,
        crossinline onSuccess: suspend(T) -> Unit,
        crossinline onError: suspend(e: Exception) -> Unit,
        contextOnSuccess: CoroutineContext = Dispatchers.Main,
        contextOnError: CoroutineContext = Dispatchers.Main,
        retryCount: Int = 0,
    ) {
        for (i in 0..retryCount) {
            try {
                val it = block()
                withContext(contextOnSuccess) {
                    onSuccess(it)
                }
                return
            } catch (e: Exception) {
                if (i == retryCount) {
                    withContext(contextOnError) {
                        onError(e)
                    }
                }
                // Exponential backoff
                delay((2.0).pow(i).toLong() * 1000)
            }
        }
    }

    inline fun <T : Any> CoroutineScope.launchCatching(
        crossinline block: suspend () -> T,
        crossinline onSuccess: suspend(T) -> Unit,
        crossinline onError: suspend(e: Exception) -> Unit,
        context: CoroutineContext = Dispatchers.IO,
        contextOnSuccess: CoroutineContext = Dispatchers.Main,
        contextOnError: CoroutineContext = Dispatchers.Main,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        retryCount: Int = 0,
    ) {
        launch(context, start) {
            launchCatching(block, onSuccess, onError, contextOnSuccess, contextOnError, retryCount)
        }
    }

    inline fun <R> runSuspendCatching(block: () -> R): Result<R> {
        return try {
            Result.success(block())
        } catch (c: CancellationException) {
            throw c
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun List<Boolean>.all(): Boolean? {
        if (this.isEmpty())
            return true
        if (all { it == first() })
            return first()
        return null
    }

    fun <E> MutableList<E>.swap(index1: Int, index2: Int) {
        this[index1] = this[index2].also { this[index2] = this[index1] }
    }

    // reference:https://gist.github.com/bartekpacia/eb1c92886acf3972c3f030cde2579ebb
    fun <T> LiveData<T>.observeOnce(
        owner: LifecycleOwner,
        reactToChange: (T) -> Unit
    ): Observer<T> {
        val wrappedObserver = object : Observer<T> {
            override fun onChanged(value: T) {
                reactToChange(value)
                removeObserver(this)
            }
        }

        observe(owner, wrappedObserver)
        return wrappedObserver
    }
}

//temp workaround until https://github.com/Kotlin/kotlinx.serialization/issues/1927 fixed
open class EmptyAsNullJsonTransformingSerializer<T>(
    private val tSerializer: KSerializer<T>
) : KSerializer<T> {

    override val descriptor: SerialDescriptor get() = tSerializer.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        if (encoder is JsonEncoder) {
            var element = encoder.json.encodeToJsonElement(tSerializer, value)
            element = transformSerialize(element)
            encoder.encodeJsonElement(element)
        } else {
            encoder.encodeSerializableValue(tSerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            return decoder.json.decodeFromJsonElement(tSerializer, transformDeserialize(element))
        } else {
            return decoder.decodeSerializableValue(tSerializer)
        }
    }

    protected open fun transformDeserialize(element: JsonElement): JsonElement =
        if (element.jsonObject.isEmpty()) JsonNull else element

    protected open fun transformSerialize(element: JsonElement): JsonElement = element
}

/**
 *  check if value update (only then trigger observer)
 */
fun <T> MutableLiveData<T>.checkUpdate(value: T): Boolean {
    return if (this.value != value) {
        this.value = value
        true
    } else false
}