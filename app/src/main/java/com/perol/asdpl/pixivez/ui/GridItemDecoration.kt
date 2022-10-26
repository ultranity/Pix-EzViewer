package com.perol.asdpl.pixivez.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class GridItemDecoration(private val space: Int = 1) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val layoutParams = view.layoutParams as? StaggeredGridLayoutManager.LayoutParams?: return
        if (layoutParams.height == 0) {
            // outRect.bottom = 0
            layoutParams.topMargin = 5
            view.layoutParams = layoutParams
            outRect.top = 0
        }
        /*
        val spanIndex = layoutParams.spanIndex
        outRect.top = space
        if (spanIndex == 0) {
            // left
            outRect.left = space
            outRect.right = space / 2
        }
        else {
            outRect.right = space
            outRect.left = space / 2
        }*/
    }
}

// from:https://www.jianshu.com/p/3e0749a1cf0c
class AverageGridItemDecoration(private val itemWidthPx:Int, private val padding:Boolean=true) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val layoutManager = parent.layoutManager as? GridLayoutManager ?: return
        val spanCount = layoutManager.spanCount
        val parentWidth = parent.measuredWidth - (parent.paddingStart + parent.paddingEnd)
        //val spanWidth = parentWidth / spanCount
        if ((padding.not()) and (spanCount == 1))
            return
        val spanMargin = (parentWidth - itemWidthPx * spanCount) / (if (padding) (spanCount + 1) else (spanCount - 1))
        val spanSizeLookup = layoutManager.spanSizeLookup
        val adapterPosition = parent.getChildAdapterPosition(view)
        //val columnIndex = spanSizeLookup.getSpanIndex(adapterPosition, spanCount)
        // 核心代码:
        // 左边的间距 = 期望的left- 默认的left
        outRect.left = spanMargin - parent.paddingStart - 5
        /* 不是第一行的情况，设置上边距
        if (spanSizeLookup.getSpanGroupIndex(adapterPosition, spanCount) > 0) {
            outRect.top = dp2px(4f)
        }*/
    }
}