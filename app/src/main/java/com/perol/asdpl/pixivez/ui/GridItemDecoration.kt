package com.perol.asdpl.pixivez.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager


class GridItemDecoration(private val space: Int = 1): RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val layoutParams = view.layoutParams as StaggeredGridLayoutManager.LayoutParams
        val spanIndex = layoutParams.spanIndex
        if (layoutParams.height == 0) {
            //outRect.bottom = 0
            layoutParams.topMargin = 5
            view.layoutParams = layoutParams
            outRect.top = 0
        }
        /*
        outRect.top = space
        if (spanIndex == 0) {
            // left
            outRect.left = space
            outRect.right = space / 2
        } else {
            outRect.right = space
            outRect.left = space / 2
        }*/
    }
}