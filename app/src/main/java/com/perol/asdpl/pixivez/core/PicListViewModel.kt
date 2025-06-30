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
import com.perol.asdpl.pixivez.data.model.IIllustNext
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.data.model.IllustNext
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.IllustCacheRepo
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class RESTRICT_TYPE {
    all,
    public,
    private
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
    Cache,
    Else;

    companion object {
        @JvmStatic
        fun check(value: String): TAG_TYPE {
            // WARN: Dangerous magic here: we use TAG of fragment to define special behavior
            // and use Else for those not specified.
            // This should be replaced by using Enum Directly in the future.
            return try {
                TAG_TYPE.valueOf(value)
            } catch (e: Exception) {
                Else
            }
        }
        fun isUserContent(value: String): Boolean {
            return value == UserIllust.name || value == UserManga.name
        }
    }
}

fun <T> PicListExtraArgs(defaultValue: T? = null) = ExtraArgumentProperty(defaultValue)
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

fun <T> PicListArgs(defaultValue: T? = null) = ArgumentProperty(defaultValue)
class ArgumentProperty<T>(private val defaultValue: T? = null) :
    ReadWriteProperty<PicListViewModel, T> {

    override fun getValue(thisRef: PicListViewModel, property: KProperty<*>): T {
        return thisRef.args.getValue(property.name) as? T
            ?: defaultValue
            ?: throw IllegalStateException("Property ${property.name} could not be read")
    }

    override fun setValue(thisRef: PicListViewModel, property: KProperty<*>, value: T) {
        thisRef.args[property.name] = value
    }
}

open class PicListViewModel : BaseViewModel() {
    private var TAG: String = javaClass.simpleName

    val data = MutableLiveData<MutableList<Illust>?>()
    val dataAdded = MutableLiveData<MutableList<Illust>?>()
    val nextUrl = MutableLiveData<String?>()
    val isRefreshing = DMutableLiveData(false)
    val restrict = DMutableLiveData(RESTRICT_TYPE.all)
    lateinit var args: MutableMap<String, Any?>
    protected lateinit var onLoadFirstRx: suspend () -> IIllustNext

    open fun setonLoadFirstRx(mode: String, extraArgs: MutableMap<String, Any?>? = null) {
        TAG = mode
        if (extraArgs != null) {
            this.args = extraArgs
        }
        when (TAG_TYPE.check(mode)) {
            TAG_TYPE.WalkThrough -> {
                { retrofit.api.walkthroughIllusts() }
            }

            TAG_TYPE.Recommend -> {
                { retrofit.api.getIllustRecommend().let { IllustNext(it.illusts, it.next_url) } }
            }

            TAG_TYPE.Rank -> {
                { retrofit.api.getIllustRanking(args["mode"] as String, args["pickDate"] as String?) }
            }

            TAG_TYPE.MyFollow -> {
                { retrofit.api.getFollowIllusts(restrict.value.name) }
            }

            TAG_TYPE.UserIllust -> {
                { retrofit.api.getUserIllusts(args["userid"] as Int, "illust") }
            }

            TAG_TYPE.UserManga -> {
                { retrofit.api.getUserIllusts(args["userid"] as Int, "manga") }
            }

            TAG_TYPE.UserBookmark -> {
                {
                    val id = args["userid"] as Int
                    val pub = args["pub"] as String? ?: "public"
                    retrofit.api.getLikeIllust(
                        id, pub,
                        args["tag"] as String?
                    )
                }
            }
            TAG_TYPE.Collect -> {
                suspend {
                    IllustNext(
                        DataHolder.dataListRef ?: arrayListOf(),
                        DataHolder.nextUrlRef
                    )
                }
            }

            TAG_TYPE.Cache -> {
                suspend {
                    IllustNext(IllustCacheRepo.getAll(), null)
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

    open fun onLoadFirst(onLoadFirstRx: suspend () -> IIllustNext = this.onLoadFirstRx) {
        isRefreshing.value = true
        subscribeNext(onLoadFirstRx, data, nextUrl) { isRefreshing.value = false }
    }

    open fun onLoadMore() {
        if (nextUrl.value != null) {
            subscribeNext({ retrofit.getIllustNext(nextUrl.value!!) }, dataAdded, nextUrl)
        }
    }
}
