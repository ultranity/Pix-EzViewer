/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
 * Copyright (c) 2019 Perol_Notsfsssf
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

package com.perol.asdpl.pixivez.adapters

import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnLoadMoreListener
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.PxEZApp

// basic Adapter for image item
//TODO: reuse more code
abstract class PicItemAdapter(
    layoutResId: Int,
    data: List<Illust>?
) :
    BaseQuickAdapter<Illust, BaseViewHolder>(layoutResId, data?.toMutableList()), LoadMoreModule {

    abstract var hideBookmarked: Boolean
    var colorPrimary: Int = R.color.colorPrimary
    var badgeTextColor: Int = R.color.yellow
    abstract var blockTags: List<String>
    val retrofitRepository: RetrofitRepository = RetrofitRepository.getInstance()
    fun loadMoreEnd() {
        this.loadMoreModule?.loadMoreEnd()
    }

    fun loadMoreComplete() {
        this.loadMoreModule?.loadMoreComplete()
    }

    fun loadMoreFail() {
        this.loadMoreModule?.loadMoreFail()
    }

    fun x_restrict(item: Illust): String{
        return if (PxEZApp.R18Private && item.x_restrict == 1) {
            "private"
        } else {
            "public"
        }
    }

    fun setOnLoadMoreListener(onLoadMoreListener: OnLoadMoreListener, recyclerView: RecyclerView?) {
        this.loadMoreModule?.setOnLoadMoreListener(onLoadMoreListener)
    }

    /*override fun addData(newData: Collection<Illust>) {
        /*super.addData(newData.mapNotNull {
            if(hideBookmarked) {
                if(it.is_bookmarked) null else it
            } else if (blockTags.isNotEmpty()) {
                if (blockTags.intersect(it.tags.map { it.name }).isNotEmpty())
                    null else it
            }
            else
                it
        })*/
        super.addData(
            newData.apply {
                if (hideBookmarked) {
                    filter {
                        !it.is_bookmarked
                    }
                }
            }.apply {
                if (blockTags.isNotEmpty()) {
                    filter{
                        !blockTags.intersect(it.tags.map { it.name }).isNotEmpty()
                    }
                }
            }
        )
    }

    override fun setNewData(newData: MutableList<Illust>?) {
        super.setNewData(
            newData?.apply {
                if (hideBookmarked) {
                    mapNotNull {
                        if(it.is_bookmarked)
                            null
                        else
                            it
                    }
                }
            }?.apply {
                if (blockTags.isNotEmpty()) {
                    mapNotNull{
                        if (blockTags.intersect(it.tags.map { it.name }).isNotEmpty())
                            null
                        else
                            it
                    }
                }
            }
        )
    }*/
    override fun addData(newData: Collection<Illust>) {
        super.addData(newData)
        DataHolder.pictureAdapter?.notifyDataSetChanged().also{
            DataHolder.pictureAdapter=null
        }
    }
}
