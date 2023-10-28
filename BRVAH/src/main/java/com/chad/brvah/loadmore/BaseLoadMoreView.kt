package com.chad.brvah.loadmore

import android.view.View
import android.view.ViewGroup
import com.chad.brvah.R
import com.chad.brvah.util.getItemView
import com.chad.brvah.viewholder.BaseViewHolder

/**
 *
 * @author limuyang
 */

enum class LoadMoreStatus {
    None, Complete, Loading, Fail, End
}

/**
 * 继承此类，实行自定义loadMore视图
 */
abstract class BaseLoadMoreView {

    /**
     * 根布局
     * @param parent ViewGroup
     * @return View
     */
    abstract fun getRootView(parent: ViewGroup): View

    /**
     * 布局中的 加载更多视图
     * @param holder BaseViewHolder
     * @return View
     */
    abstract fun getLoadingView(holder: BaseViewHolder): View

    /**
     * 布局中的 加载完成布局
     * @param holder BaseViewHolder
     * @return View
     */
    abstract fun getLoadComplete(holder: BaseViewHolder): View

    /**
     * 布局中的 加载结束布局
     * @param holder BaseViewHolder
     * @return View
     */
    abstract fun getLoadEndView(holder: BaseViewHolder): View

    /**
     * 布局中的 加载失败布局
     * @param holder BaseViewHolder
     * @return View
     */
    abstract fun getLoadFailView(holder: BaseViewHolder): View

    /**
     * 可重写此方式，实行自定义逻辑
     * @param holder BaseViewHolder
     * @param position Int
     * @param loadMoreStatus LoadMoreStatus
     */
    open fun convert(holder: BaseViewHolder, position: Int, loadMoreStatus: LoadMoreStatus) {
        when (loadMoreStatus) {
            LoadMoreStatus.None -> {
                getLoadingView(holder).isVisible(false)
                getLoadComplete(holder).isVisible(false)
                getLoadFailView(holder).isVisible(false)
                getLoadEndView(holder).isVisible(false)
            }

            LoadMoreStatus.Complete -> {
                getLoadingView(holder).isVisible(false)
                getLoadComplete(holder).isVisible(true)
                getLoadFailView(holder).isVisible(false)
                getLoadEndView(holder).isVisible(false)
            }

            LoadMoreStatus.Loading -> {
                getLoadingView(holder).isVisible(true)
                getLoadComplete(holder).isVisible(false)
                getLoadFailView(holder).isVisible(false)
                getLoadEndView(holder).isVisible(false)
            }

            LoadMoreStatus.Fail -> {
                getLoadingView(holder).isVisible(false)
                getLoadComplete(holder).isVisible(false)
                getLoadFailView(holder).isVisible(true)
                getLoadEndView(holder).isVisible(false)
            }

            LoadMoreStatus.End -> {
                getLoadingView(holder).isVisible(false)
                getLoadComplete(holder).isVisible(false)
                getLoadFailView(holder).isVisible(false)
                getLoadEndView(holder).isVisible(true)
            }
        }
    }

    private fun View.isVisible(visible: Boolean) {
        this.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}


class SimpleLoadMoreView : BaseLoadMoreView() {

    override fun getRootView(parent: ViewGroup): View =
        parent.getItemView(R.layout.brvah_quick_view_load_more)

    override fun getLoadingView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_loading_view)

    override fun getLoadComplete(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_complete_view)

    override fun getLoadEndView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_end_view)

    override fun getLoadFailView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_fail_view)
}