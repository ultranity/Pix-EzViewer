package com.perol.asdpl.pixivez.objects

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.LazyFragment
import com.perol.asdpl.pixivez.services.PxEZApp

class UpToTopListener(
    private val fragmentGetter: (position: Int) -> Fragment?,
    private val tabReselected: (tab: TabLayout.Tab) -> Unit = {},
    private val tabUnSelected: (tab: TabLayout.Tab) -> Unit = {},
    private val tabSelected: (tab: TabLayout.Tab) -> Unit = {},
) : TabLayout.OnTabSelectedListener {
    constructor(
        contextFragment: Fragment,
        tabReselected: (tab: TabLayout.Tab) -> Unit = {},
        tabUnSelected: (tab: TabLayout.Tab) -> Unit = {},
        tabSelected: (tab: TabLayout.Tab) -> Unit = {},
    ) : this(
        contextFragment.childFragmentManager,
        tabReselected, tabUnSelected, tabSelected,
    )

    constructor(
        fragmentManager: FragmentManager,
        tabReselected: (tab: TabLayout.Tab) -> Unit = {},
        tabUnSelected: (tab: TabLayout.Tab) -> Unit = {},
        tabSelected: (tab: TabLayout.Tab) -> Unit = {},
    ) : this(
        { fragmentManager.fragments[it] },
        tabReselected, tabUnSelected, tabSelected,
    )

    private var exitTime: Long = 0
    override fun onTabReselected(tab: TabLayout.Tab) {
        if ((System.currentTimeMillis() - exitTime) > 3000) {
            Toasty.normal(PxEZApp.instance, R.string.back_to_the_top)
            exitTime = System.currentTimeMillis()
        } else {
            fragmentGetter(tab.position)?.view
                ?.findViewById<RecyclerView>(R.id.recyclerview)
                ?.scrollToPosition(0)
        }
        tabReselected(tab)
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = tabUnSelected(tab)
    override fun onTabSelected(tab: TabLayout.Tab) = tabSelected(tab)
}

abstract class UpToTopFragment : LazyFragment() {
    open val fragmentGetter: (position: Int) -> Fragment? = {
        childFragmentManager.fragments[it]
    }

    var topTab: TabLayout.Tab? = null
    fun upToTop() {
        if (topTab == null) return
        fragmentGetter(topTab!!.position)?.view
            ?.findViewById<RecyclerView>(R.id.recyclerview)
            ?.scrollToPosition(0)
    }
}