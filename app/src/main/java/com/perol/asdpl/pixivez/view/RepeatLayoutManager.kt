package com.perol.asdpl.pixivez.view

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * @Author : wangjian
 * @Data : 2020-12-23 10:17
 * @Describe :
 */
class RepeatLayoutManager(
    context: Context, @RecyclerView.Orientation orientation: Int,
    reverseLayout: Boolean = false
) :
    LinearLayoutManager(context, orientation, reverseLayout) {

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (itemCount <= 0) {
            return
        }
        if (state.isPreLayout) {
            return
        }
        //将所有Item分离至scrap
        detachAndScrapAttachedViews(recycler)
        layoutChunk(recycler)
    }

    private fun layoutChunk(recycler: RecyclerView.Recycler) {
        if (orientation == RecyclerView.HORIZONTAL) {
            var itemLeft = paddingLeft
            var i = 0
            while (true) {
                if (itemLeft > width - paddingRight) {
                    break
                }
                val itemView = recycler.getViewForPosition(i % itemCount)
                addView(itemView)
                measureChildWithMargins(itemView, 0, 0)
                val top = paddingTop
                val right = itemLeft + getDecoratedMeasuredWidth(itemView)
                val bottom = top + getDecoratedMeasuredHeight(itemView)
                layoutDecorated(itemView, itemLeft, top, right, bottom)
                itemLeft = right
                i++
            }
        } else {
            var itemTop = paddingTop
            var i = 0
            while (true) {
                if (itemTop > height - paddingBottom) {
                    break
                }
                val itemView = recycler.getViewForPosition(i % itemCount)
                addView(itemView)
                measureChildWithMargins(itemView, 0, 0)
                val left = paddingLeft
                val bottom = itemTop + getDecoratedMeasuredHeight(itemView)
                val right = left + getDecoratedMeasuredWidth(itemView)
                layoutDecorated(itemView, left, itemTop, right, bottom)
                itemTop = bottom
                i++
            }
        }
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        fillHorizontal(recycler, dx > 0)
        offsetChildrenHorizontal(-dx)
        recyclerChildView(dx > 0, recycler)
        return dx
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        fillVertical(recycler, dy > 0)
        offsetChildrenVertical(-dy)
        recyclerChildView(dy > 0, recycler)
        return dy
    }

    /*
     *横向填充
     */
    private fun fillHorizontal(recycler: RecyclerView.Recycler, fillEnd: Boolean) {
        if (childCount == 0) return
        if (fillEnd) {
            //填充尾部
            var anchorView = getChildAt(childCount - 1)
            val anchorPosition = getPosition(anchorView!!)
            while (anchorView!!.right < width - paddingRight) {
                var position = (anchorPosition + 1) % itemCount
                if (position < 0) position += itemCount
                val scrapItem = recycler.getViewForPosition(position)
                addView(scrapItem)
                measureChildWithMargins(scrapItem, 0, 0)
                val left = anchorView.right
                val top = paddingTop
                val right = left + getDecoratedMeasuredWidth(scrapItem)
                val bottom = top + getDecoratedMeasuredHeight(scrapItem)
                layoutDecorated(scrapItem, left, top, right, bottom)
                anchorView = scrapItem
            }
        } else {
            //填充首部
            var anchorView = getChildAt(0)
            val anchorPosition = getPosition(anchorView!!)
            while (anchorView!!.left > paddingLeft) {
                var position = (anchorPosition - 1) % itemCount
                if (position < 0) position += itemCount
                val scrapItem = recycler.getViewForPosition(position)
                addView(scrapItem, 0)
                measureChildWithMargins(scrapItem, 0, 0)
                val right = anchorView.left
                val top = paddingTop
                val left = right - getDecoratedMeasuredWidth(scrapItem)
                val bottom = top + getDecoratedMeasuredHeight(scrapItem)
                layoutDecorated(
                    scrapItem, left, top,
                    right, bottom
                )
                anchorView = scrapItem
            }
        }
        return
    }

    /*
     *纵向填充
     */
    private fun fillVertical(recycler: RecyclerView.Recycler, fillEnd: Boolean) {
        if (childCount == 0) return
        if (fillEnd) {
            //填充尾部
            var anchorView = getChildAt(childCount - 1)
            val anchorPosition = getPosition(anchorView!!)
            while (anchorView!!.bottom < height - paddingBottom) {
                var position = (anchorPosition + 1) % itemCount
                if (position < 0) position += itemCount
                val scrapItem = recycler.getViewForPosition(position)
                addView(scrapItem)
                measureChildWithMargins(scrapItem, 0, 0)
                val left = paddingLeft
                val top = anchorView.bottom
                val right = left + getDecoratedMeasuredWidth(scrapItem)
                val bottom = top + getDecoratedMeasuredHeight(scrapItem)
                layoutDecorated(scrapItem, left, top, right, bottom)
                anchorView = scrapItem
            }
        } else {
            //填充首部
            var anchorView = getChildAt(0)
            val anchorPosition = getPosition(anchorView!!)
            while (anchorView!!.top > paddingTop) {
                var position = (anchorPosition - 1) % itemCount
                if (position < 0) position += itemCount
                val scrapItem = recycler.getViewForPosition(position)
                addView(scrapItem, 0)
                measureChildWithMargins(scrapItem, 0, 0)
                val left = paddingLeft
                val right = left + getDecoratedMeasuredWidth(scrapItem)
                val bottom = anchorView.top
                val top = bottom - getDecoratedMeasuredHeight(scrapItem)
                layoutDecorated(
                    scrapItem, left, top,
                    right, bottom
                )
                anchorView = scrapItem
            }
        }
        return
    }

    /**
     * 回收界面不可见的view
     */
    private fun recyclerChildView(fillEnd: Boolean, recycler: RecyclerView.Recycler) {
        if (fillEnd) {
            //回收头部
            var i = 0
            while (true) {
                val view = getChildAt(i) ?: return
                val needRecycler =
                    if (orientation == RecyclerView.HORIZONTAL) view.right < paddingLeft else view.bottom < paddingTop
                if (needRecycler) {
                    removeAndRecycleView(view, recycler)
                } else {
                    return
                }
                i++
            }
        } else {
            //回收尾部
            var i = childCount - 1
            while (true) {
                val view = getChildAt(i) ?: return
                val needRecycler =
                    if (orientation == RecyclerView.HORIZONTAL) view.left > width - paddingRight else view.top > height - paddingBottom
                if (needRecycler) {
                    removeAndRecycleView(view, recycler)
                } else {
                    return
                }
                i--
            }
        }
    }
}