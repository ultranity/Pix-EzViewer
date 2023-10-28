package com.chad.brvah.loadmore

import android.view.View
import android.view.ViewGroup
import com.chad.brvah.R
import com.chad.brvah.util.getItemView
import com.chad.brvah.viewholder.BaseViewHolder

object EmptyViewConfig {

    /**
     * 设置全局的LodeMoreView
     */
    @JvmStatic
    var defEmptyView: BaseEmptyView = SimpleEmptyView()
}

enum class EmptyViewStatus {
    Hide, Fail, Show
}

/**
 * 继承此类，实行自定义Empty视图
 */
abstract class BaseEmptyView {

    /**
     * 根布局
     * @param parent ViewGroup
     * @return View
     */
    abstract fun getRootView(parent: ViewGroup): View

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
     * @param emptyViewStatus EmptyViewStatus
     */
    open fun convert(holder: BaseViewHolder, position: Int, emptyViewStatus: EmptyViewStatus) {
        when (emptyViewStatus) {
            EmptyViewStatus.Hide -> {
                getLoadFailView(holder).isVisible(false)
                getLoadEndView(holder).isVisible(false)
            }

            EmptyViewStatus.Show -> {
                getLoadFailView(holder).isVisible(false)
                getLoadEndView(holder).isVisible(true)
            }

            EmptyViewStatus.Fail -> {
                getLoadFailView(holder).isVisible(true)
                getLoadEndView(holder).isVisible(false)
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


class SimpleEmptyView : BaseEmptyView() {

    override fun getRootView(parent: ViewGroup): View =
        parent.getItemView(R.layout.brvah_quick_view_empty)

    override fun getLoadEndView(holder: BaseViewHolder): View =
        holder.getView(R.id.empty_view_load_end_view)

    override fun getLoadFailView(holder: BaseViewHolder): View =
        holder.getView(R.id.empty_view_load_failed_view)
}