package com.perol.asdpl.pixivez.view

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import java.lang.ref.WeakReference

//Viewpager2 in ViewPager, ViewPager 会遍历所有子view判断是否canScroll, 必须重写以实现阻断 Viewpager2 nested scroll
//isUserInputEnabled可以禁止滑动但外层Viewpager仍会放弃阻断触摸
class NotCrossScrollableLinearLayoutManager(
    context: Context,
    private val mRecyclerView: RecyclerView,
    private val mViewpager: ViewPager2,
) :
    LinearLayoutManager(context, RecyclerView.HORIZONTAL, false) {

    private fun getPageSize(): Int {
        return if (orientation == ViewPager2.ORIENTATION_HORIZONTAL)
            mRecyclerView.width - mRecyclerView.paddingLeft - mRecyclerView.paddingRight
        else
            mRecyclerView.height - mRecyclerView.paddingTop - mRecyclerView.paddingBottom
    }

    override fun calculateExtraLayoutSpace(
        state: RecyclerView.State,
        extraLayoutSpace: IntArray
    ) {
        val pageLimit: Int = mViewpager.offscreenPageLimit
        if (pageLimit == ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT) {
            // Only do custom prefetching of offscreen pages if requested
            super.calculateExtraLayoutSpace(state, extraLayoutSpace)
            return
        }
        val offscreenSpace: Int = getPageSize() * pageLimit
        extraLayoutSpace[0] = offscreenSpace
        extraLayoutSpace[1] = offscreenSpace
    }

    override fun requestChildRectangleOnScreen(
        parent: RecyclerView,
        child: View, rect: Rect, immediate: Boolean,
        focusedChildVisible: Boolean
    ): Boolean {
        return false // users should use setCurrentItem instead
    }

    override fun canScrollHorizontally(): Boolean {
        return false
    }
}

/**
 * A callback interface that must be implemented to set the text and styling of newly created
 * tabs.
 */
fun interface TabSelectedStrategy {
    fun invoke(tab: TabLayout.Tab)
}

//点击
class AutoTabLayoutMediator(
    private val tabLayout: TabLayout,
    private val viewPager: ViewPager2,
    private val autoRefresh: Boolean = true,
    private val smoothScroll: Boolean = false,
    private val tabConfigurationStrategy: TabLayoutMediator.TabConfigurationStrategy
) {
    var adapter: RecyclerView.Adapter<*>? = null
    private var attached = false
    private var onPageChangeCallback: TabLayoutOnPageChangeCallback? = null
    private var onTabSelectedListener: OnTabSelectedListener? = null
    private var pagerAdapterObserver: RecyclerView.AdapterDataObserver? = null
    private var onTabSelectedStrategy: TabSelectedStrategy? = null
    private var onTabUnSelectedStrategy: TabSelectedStrategy? = null
    private var onTabReSelectedStrategy: TabSelectedStrategy? = null
    fun setOnTabSelectedStrategy(strategy: TabSelectedStrategy){
        onTabSelectedStrategy = strategy
    }
    fun setOnTabUnSelectedStrategy(strategy: TabSelectedStrategy){
        onTabUnSelectedStrategy = strategy
    }
    fun setOnTabReSelectedStrategy(strategy: TabSelectedStrategy){
        onTabReSelectedStrategy = strategy
    }

    /**
     * Link the TabLayout and the ViewPager2 together. Must be called after ViewPager2 has an adapter
     * set. To be called on a new instance of TabLayoutMediator or if the ViewPager2's adapter
     * changes.
     *
     * @throws IllegalStateException If the mediator is already attached, or the ViewPager2 has no
     * adapter.
     */
    fun attach(): AutoTabLayoutMediator {
        check(!attached) { "TabLayoutMediator is already attached" }
        adapter = viewPager.adapter
        checkNotNull(adapter) { "TabLayoutMediator attached before ViewPager2 has an " + "adapter" }
        attached = true

        // Add our custom OnPageChangeCallback to the ViewPager
        onPageChangeCallback = TabLayoutOnPageChangeCallback(tabLayout)
        viewPager.registerOnPageChangeCallback(onPageChangeCallback!!)

        // Now we'll add a tab selected listener to set ViewPager's current item
        onTabSelectedListener = ViewPagerOnTabSelectedListener(
            viewPager, smoothScroll
        )
        tabLayout.addOnTabSelectedListener(onTabSelectedListener)

        // Now we'll populate ourselves from the pager adapter, adding an observer if
        // autoRefresh is enabled
        if (autoRefresh) {
            // Register our observer on the new adapter
            pagerAdapterObserver = PagerAdapterObserver()
            adapter!!.registerAdapterDataObserver(pagerAdapterObserver!!)
        }
        populateTabsFromPagerAdapter()

        // Now update the scroll position to match the ViewPager's current item
        tabLayout.setScrollPosition(viewPager.currentItem, 0f, true)
        return this
    }

    /**
     * Unlink the TabLayout and the ViewPager. To be called on a stale TabLayoutMediator if a new one
     * is instantiated, to prevent holding on to a view that should be garbage collected. Also to be
     * called before [.attach] when a ViewPager2's adapter is changed.
     */
    fun detach() {
        if (autoRefresh && adapter != null) {
            if (pagerAdapterObserver != null) {
                adapter!!.unregisterAdapterDataObserver(pagerAdapterObserver!!)
            }
            pagerAdapterObserver = null
        }
        if (onTabSelectedListener != null) {
            tabLayout.removeOnTabSelectedListener(onTabSelectedListener)
        }
        if (onPageChangeCallback != null) {
            viewPager.unregisterOnPageChangeCallback(onPageChangeCallback!!)
        }
        onTabSelectedListener = null
        onPageChangeCallback = null
        adapter = null
        attached = false
    }

    fun populateTabsFromPagerAdapter() {
        tabLayout.removeAllTabs()
        if (adapter != null) {
            val adapterCount = adapter!!.itemCount
            for (i in 0 until adapterCount) {
                val tab = tabLayout.newTab()
                tabConfigurationStrategy.onConfigureTab(tab, i)
                tabLayout.addTab(tab, false)
            }
            // Make sure we reflect the currently set ViewPager item
            if (adapterCount > 0) {
                val lastItem = tabLayout.tabCount - 1
                val currItem = viewPager.currentItem.coerceAtMost(lastItem)
                if (currItem != tabLayout.selectedTabPosition) {
                    tabLayout.selectTab(tabLayout.getTabAt(currItem))
                }
            }
        }
    }

    /**
     * A [ViewPager2.OnPageChangeCallback] class which contains the necessary calls back to the
     * provided [TabLayout] so that the tab position is kept in sync.
     *
     *
     * This class stores the provided TabLayout weakly, meaning that you can use [ ][ViewPager2.registerOnPageChangeCallback] without removing the
     * callback and not cause a leak.
     */
    private class TabLayoutOnPageChangeCallback(tabLayout: TabLayout) :
        OnPageChangeCallback() {
        private val tabLayoutRef: WeakReference<TabLayout>
        var previousScrollState = 0
        private var scrollState = 0

        init {
            tabLayoutRef = WeakReference(tabLayout)
            reset()
        }

        override fun onPageScrollStateChanged(state: Int) {
            previousScrollState = scrollState
            scrollState = state
        }

        override fun onPageScrolled(
            position: Int, positionOffset: Float, positionOffsetPixels: Int
        ) {
            val tabLayout = tabLayoutRef.get()
            if (tabLayout != null) {
                // Only update the text selection if we're not settling, or we are settling after
                // being dragged
                val updateText =
                    scrollState != ViewPager2.SCROLL_STATE_SETTLING || previousScrollState == ViewPager2.SCROLL_STATE_DRAGGING
                // Update the indicator if we're not settling after being idle. This is caused
                // from a setCurrentItem() call and will be handled by an animation from
                // onPageSelected() instead.
                val updateIndicator =
                    !(scrollState == ViewPager2.SCROLL_STATE_SETTLING && previousScrollState == ViewPager2.SCROLL_STATE_IDLE)
                tabLayout.setScrollPosition(position, positionOffset, updateText, updateIndicator)
            }
        }

        override fun onPageSelected(position: Int) {
            val tabLayout = tabLayoutRef.get()
            if (tabLayout != null && tabLayout.selectedTabPosition != position && position < tabLayout.tabCount) {
                // Select the tab, only updating the indicator if we're not being dragged/settled
                // (since onPageScrolled will handle that).
                val updateIndicator =
                    (scrollState == ViewPager2.SCROLL_STATE_IDLE || (scrollState == ViewPager2.SCROLL_STATE_SETTLING && previousScrollState == ViewPager2.SCROLL_STATE_IDLE))
                tabLayout.selectTab(tabLayout.getTabAt(position), updateIndicator)
            }
        }

        fun reset() {
            scrollState = ViewPager2.SCROLL_STATE_IDLE
            previousScrollState = scrollState
        }
    }

    /**
     * A [TabLayout.OnTabSelectedListener] class which contains the necessary calls back to the
     * provided [ViewPager2] so that the tab position is kept in sync.
     */
    private inner class ViewPagerOnTabSelectedListener(
        private val viewPager: ViewPager2, private val smoothScroll: Boolean
    ) : OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            if (viewPager.currentItem == tab.position) {
                return
            }
            if (onPageChangeCallback != null) {
                if (onPageChangeCallback!!.previousScrollState == ViewPager2.SCROLL_STATE_DRAGGING) {
                    viewPager.setCurrentItem(tab.position, true)
                } else {
                    viewPager.setCurrentItem(tab.position, smoothScroll)
                }
            }
            onTabSelectedStrategy?.invoke(tab)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {
            onTabUnSelectedStrategy?.invoke(tab)
            // No-op
        }

        override fun onTabReselected(tab: TabLayout.Tab) {
            // No-op
            onTabReSelectedStrategy?.invoke(tab)
        }
    }

    private inner class PagerAdapterObserver : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }
    }
}
