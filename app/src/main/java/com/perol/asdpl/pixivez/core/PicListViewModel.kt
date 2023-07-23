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
    Recommend,
    Rank,
    MyFollow,
    UserIllust,
    UserManga,
    UserBookmark,
    Collect,
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
    lateinit var filterModel: FilterViewModel
    val data = MutableLiveData<List<Illust>?>()
    val dataAdded = MutableLiveData<List<Illust>?>()
    val nextUrl = MutableLiveData<String?>()
    val isRefreshing = MutableLiveData(false)
    val restrict = MutableLiveData(RESTRICT_TYPE.all)
    lateinit var args: MutableMap<String, Any?>
    protected lateinit var onLoadFirstRx: () -> Observable<IllustNext>

    open fun setonLoadFirstRx(mode: String, extraArgs: MutableMap<String, Any?>? = null) {
        if (extraArgs != null) {
            this.args = extraArgs
        }
        onLoadFirstRx = when (mode) {
            "Recommend" -> {
                { retrofit.getRecommend().map { IllustNext(it.illusts, it.next_url) } }
            }
            "Rank" -> {
                { retrofit.getIllustRanking(args["mode"] as String, args["pickDate"] as String?) }
            }
            "MyFollow" -> {
                { retrofit.getFollowIllusts(restrict.value!!.name) }
            }

            "UserIllust" -> {
                { retrofit.getUserIllusts(args["userid"] as Long, "illust") }
            }
            "UserManga" -> {
                { retrofit.getUserIllusts(args["userid"] as Long, "manga") }
            }
            "UserBookmark" -> {
                {
                    val id = args["userid"] as Long
                    val pub = args["pub"] as String??:"public"
                    if (tags == null) {
                        getTags(id)
                    }
                    retrofit.getLikeIllust(
                        id,pub,
                        args["tag"] as String?
                    )

                }
            }

            "Collect" -> {
                { Observable.just(DataHolder.getIllustList()).map { IllustNext(it, null) } }
            }

            else -> {
                { retrofit.getRecommend().map { IllustNext(it.illusts, it.next_url) } }
            }
        }
    }

    open fun onLoadFirst() {
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
