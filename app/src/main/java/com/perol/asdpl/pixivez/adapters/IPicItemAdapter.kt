package com.perol.asdpl.pixivez.adapters

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.chad.library.adapter.base.module.LoadMoreModule
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.PxEZApp

interface IPicItemAdapter {
    var R18on: Boolean
    var hideBookmarked: Int
    var sortCoM: Int
    var hideDownloaded: Boolean
    var blockTags: List<String>
    fun x_restrict(item: Illust): String{
        return if (PxEZApp.R18Private && item.x_restrict == 1) {
            "private"
        } else {
            "public"
        }
    }
    fun setFullSpan(holder: RecyclerView.ViewHolder, isFullSpan:Boolean ) {
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            layoutParams.isFullSpan = isFullSpan
        }
    }
}