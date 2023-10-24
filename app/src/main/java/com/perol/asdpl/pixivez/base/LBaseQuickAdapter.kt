package com.perol.asdpl.pixivez.base

import androidx.annotation.LayoutRes
import com.chad.brvah.BaseQuickAdapter
import com.chad.brvah.listener.OnLoadMoreListener
import com.chad.brvah.module.LoadMoreModule
import com.chad.brvah.viewholder.BaseViewHolder

abstract class LBaseQuickAdapter<T, VH : BaseViewHolder>(@LayoutRes private val layoutResId: Int,
                                                         data: MutableList<T>?=null)
    :BaseQuickAdapter<T,VH>(layoutResId, data), LoadMoreModule {
    fun loadMoreEnd() {
        this.loadMoreModule.loadMoreEnd()
    }

    fun loadMoreComplete() {
        this.loadMoreModule.loadMoreComplete()
    }

    fun loadMoreFail() {
        this.loadMoreModule.loadMoreFail()
    }

    fun setOnLoadMoreListener(onLoadMoreListener: OnLoadMoreListener) {
        this.loadMoreModule.setOnLoadMoreListener(onLoadMoreListener)
    }
}