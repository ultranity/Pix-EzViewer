package com.chad.brvah.module

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.chad.brvah.BaseQuickAdapter
import com.chad.brvah.listener.LoadMoreListenerImp
import com.chad.brvah.listener.OnLoadMoreListener
import com.chad.brvah.loadmore.BaseLoadMoreView
import com.chad.brvah.loadmore.LoadMoreStatus
import com.chad.brvah.loadmore.SimpleLoadMoreView
import com.chad.brvah.viewholder.BaseViewHolder


/**
 * @author: limuyang
 * @date: 2019-11-29
 * @Description: 向下加载更多
 */

/**
 * 需要【向下加载更多】功能的，[BaseQuickAdapter]继承此接口
 */
interface LoadMoreModule {
    /**
     * 重写此方法，返回自定义模块
     * @param baseQuickAdapter BaseQuickAdapter<*, *>
     * @return BaseLoadMoreModule
     */
    fun addLoadMoreModule(baseQuickAdapter: BaseQuickAdapter<*, *>): BaseLoadMoreModule {
        return BaseLoadMoreModule(baseQuickAdapter)
    }
}

object LoadMoreModuleConfig {

    /**
     * 设置全局的LodeMoreView
     */
    @JvmStatic
    var defLoadMoreView: BaseLoadMoreView = SimpleLoadMoreView()
}

/**
 * 加载更多基类
 */
open class BaseLoadMoreModule(private val baseQuickAdapter: BaseQuickAdapter<*, *>) :
    LoadMoreListenerImp {

    private var mManualLoadMoreListener: OnLoadMoreListener? = null
    private var mLoadMoreListener: OnLoadMoreListener? = null

    /** 不满一屏时，是否可以继续加载的标记位 */
    private var mNextLoadEnable = true

    var loadMoreStatus = LoadMoreStatus.None
        private set

    var isLoadEndMoreGone: Boolean = false
        private set

    /** 设置加载更多布局 */
    var loadMoreView = LoadMoreModuleConfig.defLoadMoreView

    /** 加载完成后是否允许点击 */
    var enableLoadMoreEndClick = false

    /** 是否打开自动加载更多 */
    var isAutoLoadMore = true

    /** 当自动加载开启，同时数据不满一屏时，是否继续执行自动加载更多 */
    var isEnableLoadMoreIfNotFullPage = true

    /**
     * 预加载
     */
    var preLoadNumber = 1
        set(value) {
            if (value > 1) {
                field = value
            }
        }

    /**
     * 是否加载中
     */
    val isLoading: Boolean
        get() {
            return loadMoreStatus == LoadMoreStatus.Loading
        }

    /**
     * Gets to load more locations
     *
     * @return
     */
    val loadMoreViewPosition: Int
        get() {
            if (baseQuickAdapter.hasEmptyView()) {
                return -1
            }
            return baseQuickAdapter.let {
                it.headerLayoutCount + it.data.size + it.footerLayoutCount
            }
        }

    /**
     * 是否打开加载更多
     */
    var isEnableLoadMore = false
        set(value) {
            val oldHasLoadMore = showLoadMoreView()
            field = value
            val newHasLoadMore = showLoadMoreView()

            if (oldHasLoadMore) {
                if (!newHasLoadMore) {
                    baseQuickAdapter.notifyItemRemoved(loadMoreViewPosition)
                }
            } else {
                if (newHasLoadMore) {
                    loadMoreStatus = LoadMoreStatus.Complete
                    baseQuickAdapter.notifyItemInserted(loadMoreViewPosition)
                }
            }
        }


    internal fun setupViewHolder(viewHolder: BaseViewHolder) {
        viewHolder.itemView.setOnClickListener {
            if (loadMoreStatus == LoadMoreStatus.Fail) {
                loadMoreToLoading()
            } else if (loadMoreStatus == LoadMoreStatus.Complete) {
                loadMoreToLoading()
            } else if (enableLoadMoreEndClick && loadMoreStatus == LoadMoreStatus.End) {
                loadMoreToLoading()
            }
        }
    }

    /**
     * The notification starts the callback and loads more
     */
    fun loadMoreToLoading() {
        if (loadMoreStatus == LoadMoreStatus.Loading) {
            return
        }
        loadMoreStatus = LoadMoreStatus.Loading
        baseQuickAdapter.notifyItemChanged(loadMoreViewPosition)
        mManualLoadMoreListener?.invoke()
        invokeLoadMoreListener()
    }


    fun showLoadMoreView(): Boolean {
        if (mLoadMoreListener == null || !isEnableLoadMore) {
            return false
        }
        if (loadMoreStatus == LoadMoreStatus.End && isLoadEndMoreGone) {
            return false
        }

        return loadMoreStatus != LoadMoreStatus.None // TODO: baseQuickAdapter.data.isNotEmpty()
    }

    /**
     * 自动加载数据
     * @param position Int
     */
    internal fun autoLoadMore(position: Int) {
        if (!isAutoLoadMore) {
            //如果不需要自动加载更多，直接返回
            return
        }
        if (!showLoadMoreView()) {
            return
        }
        if (position < baseQuickAdapter.itemCount - preLoadNumber) {
            return
        }
        if (loadMoreStatus != LoadMoreStatus.Complete) {
            return
        }
        if (loadMoreStatus == LoadMoreStatus.Loading) {
            return
        }
        if (!mNextLoadEnable) {
            return
        }

        invokeLoadMoreListener()
    }

    /**
     * 触发加载更多监听
     */
    private fun invokeLoadMoreListener() {
        loadMoreStatus = LoadMoreStatus.Loading
        baseQuickAdapter.recyclerViewOrNull?.let {
            it.post { mLoadMoreListener?.invoke() }
        } ?: mLoadMoreListener?.invoke()
    }

    /**
     * check if full page after [BaseQuickAdapter.setNewInstance] [BaseQuickAdapter.setList],
     * if full, it will enable load more again.
     *
     * 用来检查数据是否满一屏，如果满足条件，再开启
     *
     */
    fun checkDisableLoadMoreIfNotFullPage() {
        if (isEnableLoadMoreIfNotFullPage) {
            return
        }
        // 先把标记位设置为false
        mNextLoadEnable = false
        val recyclerView = baseQuickAdapter.recyclerViewOrNull ?: return
        val manager = recyclerView.layoutManager ?: return
        if (manager is LinearLayoutManager) {
            recyclerView.postDelayed({
                if (isFullScreen(manager)) {
                    mNextLoadEnable = true
                }
            }, 50)
        } else if (manager is StaggeredGridLayoutManager) {
            recyclerView.postDelayed({
                val positions = IntArray(manager.spanCount)
                manager.findLastCompletelyVisibleItemPositions(positions)
                val pos = getTheBiggestNumber(positions) + 1
                if (pos != baseQuickAdapter.itemCount) {
                    mNextLoadEnable = true
                }
            }, 50)
        }
    }

    private fun isFullScreen(llm: LinearLayoutManager): Boolean {
        return (llm.findLastCompletelyVisibleItemPosition() + 1) != baseQuickAdapter.itemCount ||
                llm.findFirstCompletelyVisibleItemPosition() != 0
    }

    private fun getTheBiggestNumber(numbers: IntArray?): Int {
        var tmp = -1
        if (numbers == null || numbers.isEmpty()) {
            return tmp
        }
        for (num in numbers) {
            if (num > tmp) {
                tmp = num
            }
        }
        return tmp
    }

    /**
     * Refresh end, no more data
     *
     * @param gone if true gone the load more view
     */
    @JvmOverloads
    fun loadMoreEnd(gone: Boolean = false) {
//        mNextLoadEnable = false
        isLoadEndMoreGone = gone

        loadMoreStatus = LoadMoreStatus.End

        if (!showLoadMoreView()) {
            return
        }

        if (gone) {
            baseQuickAdapter.notifyItemRemoved(loadMoreViewPosition)
        } else {
            baseQuickAdapter.notifyItemChanged(loadMoreViewPosition)
        }
    }

    /**
     * Refresh complete
     */
    fun loadMoreComplete() {
//        mNextLoadEnable = true
        loadMoreStatus = LoadMoreStatus.Complete

        if (!showLoadMoreView()) {
            return
        }

        baseQuickAdapter.notifyItemChanged(loadMoreViewPosition)

        checkDisableLoadMoreIfNotFullPage()
    }

    /**
     * Refresh failed
     */
    fun loadMoreFail() {
        loadMoreStatus = LoadMoreStatus.Fail
        if (!showLoadMoreView()) {
            return
        }
        baseQuickAdapter.notifyItemChanged(loadMoreViewPosition)
    }

    /**
     * 设置加载监听事件
     * @param listener OnLoadMoreListener?
     */
    fun setOnManualLoadMoreListener(listener: OnLoadMoreListener?) {
        this.mManualLoadMoreListener = listener
    }

    /**
     * 设置加载监听事件
     * @param listener OnLoadMoreListener?
     */
    override fun setOnLoadMoreListener(listener: OnLoadMoreListener?) {
        this.mLoadMoreListener = listener
        isEnableLoadMore = true
    }

    /**
     * 重置状态
     */
    internal fun reset() {
        if (mLoadMoreListener != null) {
            isEnableLoadMore = true
            loadMoreStatus = LoadMoreStatus.None
        }
    }
}