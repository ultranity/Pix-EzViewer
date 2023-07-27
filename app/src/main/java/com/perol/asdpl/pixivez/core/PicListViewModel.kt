/*
 * MIT License
 *
 * Copyright (c) 2023 Ultranity
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

package com.perol.asdpl.pixivez.core

import androidx.lifecycle.MutableLiveData
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.base.DMutableLiveData
import com.perol.asdpl.pixivez.data.model.BookMarkTagsResponse
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.data.model.IllustNext
import com.perol.asdpl.pixivez.objects.DataHolder
import io.reactivex.Observable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class RESTRICT_TYPE {
    all,
    public,
    private,
}
enum class TAG_TYPE {
    WalkThrough,
    Recommend,
    Rank,
    MyFollow,
    UserIllust,
    UserManga,
    UserBookmark,
    Collect,
    Else;

    companion object {
        @JvmStatic
        fun check(value: String): TAG_TYPE {
            return try {
                TAG_TYPE.valueOf(value)
            } catch (e: Exception) {
                Else
            }
        }
    }
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

fun <T> extraArg(defaultValue: T? = null) = ExtraArgumentProperty(defaultValue)
class ExtraArgumentProperty<T>(private val defaultValue: T? = null) :
    ReadWriteProperty<PicListFragment, T> {

    override fun getValue(thisRef: PicListFragment, property: KProperty<*>): T {
        return thisRef.extraArgs?.getValue(property.name) as? T
            ?: defaultValue
            ?: throw IllegalStateException("Property ${property.name} could not be read")
    }

    override fun setValue(thisRef: PicListFragment, property: KProperty<*>, value: T) {
            thisRef.extraArgs?.set(property.name, value)
    }
}

fun <T> arg(defaultValue: T? = null) = ArgumentProperty(defaultValue)
class ArgumentProperty<T>(private val defaultValue: T? = null) :
    ReadWriteProperty<PicListViewModel, T> {

    override fun getValue(thisRef: PicListViewModel, property: KProperty<*>): T {
        return thisRef.args.getValue(property.name) as? T
            ?: defaultValue
            ?: throw IllegalStateException("Property ${property.name} could not be read")
    }

    override fun setValue(thisRef: PicListViewModel, property: KProperty<*>, value: T) {
        thisRef.args.set(property.name, value)
    }
}
open class PicListViewModel : BaseViewModel() {
    private var TAG: String = javaClass.simpleName
    lateinit var filterModel: FilterViewModel
    val data = MutableLiveData<MutableList<Illust>?>()
    val dataAdded = MutableLiveData<MutableList<Illust>?>()
    val nextUrl = MutableLiveData<String?>()
    val isRefreshing = DMutableLiveData(false)
    val restrict = DMutableLiveData(RESTRICT_TYPE.all)
    lateinit var args: MutableMap<String, Any?>
    protected lateinit var onLoadFirstRx: () -> Observable<IllustNext>

    open fun setonLoadFirstRx(mode: String, extraArgs: MutableMap<String, Any?>? = null) {
        TAG = mode
        if (extraArgs != null) {
            this.args = extraArgs
        }
        when (TAG_TYPE.check(mode)) {
            TAG_TYPE.WalkThrough -> {
                { retrofit.getWalkThrough() }
            }

            TAG_TYPE.Recommend -> {
                { retrofit.getRecommend().map { IllustNext(it.illusts, it.next_url) } }
            }

            TAG_TYPE.Rank -> {
                { retrofit.getIllustRanking(args["mode"] as String, args["pickDate"] as String?) }
            }

            TAG_TYPE.MyFollow -> {
                { retrofit.getFollowIllusts(restrict.value!!.name) }
            }

            TAG_TYPE.UserIllust -> {
                { retrofit.getUserIllusts(args["userid"] as Long, "illust") }
            }

            TAG_TYPE.UserManga -> {
                { retrofit.getUserIllusts(args["userid"] as Long, "manga") }
            }

            TAG_TYPE.UserBookmark -> {
                {
                    val id = args["userid"] as Long
                    val pub = args["pub"] as String? ?: "public"
                    if (tags == null) {
                        getTags(id)
                    }
                    retrofit.getLikeIllust(
                        id, pub,
                        args["tag"] as String?
                    )
                }
            }

            TAG_TYPE.Collect -> {
                {
                    Observable.just(1).map {
                        IllustNext(
                            DataHolder.dataListRef ?: arrayListOf(),
                            DataHolder.nextUrlRef
                        )
                    }
                }
            }

            TAG_TYPE.Else -> null
        }?.let {
            onLoadFirstRx = it
        }
    }

    open fun onLoadFirst() {
        onLoadFirst(onLoadFirstRx)
    }

    open fun onLoadFirst(onLoadFirstRx: () -> Observable<IllustNext> = this.onLoadFirstRx) {
        isRefreshing.value = true
        onLoadFirstRx().subscribeNext(data, nextUrl) {
            isRefreshing.value = false
        }
    }

    //fun onLoadMoreRx(nextUrl: String): Observable<IllustNext> = retrofit.getNext(nextUrl)
    open fun onLoadMore() {
        if (nextUrl.value != null) {
            retrofit.getNextIllusts(nextUrl.value!!).subscribeNext(dataAdded, nextUrl)
        }
    }

    // for UserBookmark
    var tags: BookMarkTagsResponse? = null

    fun getTags(id: Long) {
        retrofit.getIllustBookmarkTags(id, "public").subscribe({
            tags = it
        }, {}, {}).add()
    }
}
