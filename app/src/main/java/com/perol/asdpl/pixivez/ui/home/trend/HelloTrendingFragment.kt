/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
 * Copyright (c) 2019 Perol_Notsfsssf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package com.perol.asdpl.pixivez.ui.home.trend

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.drag.IDraggable
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.onClick
import com.perol.asdpl.pixivez.base.onItemClick
import com.perol.asdpl.pixivez.base.setItems
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.databinding.FragmentHelloTrendingBinding
import com.perol.asdpl.pixivez.databinding.ViewTagsItemBinding
import com.perol.asdpl.pixivez.objects.UpToTopFragment
import com.perol.asdpl.pixivez.objects.UpToTopListener
import com.perol.asdpl.pixivez.objects.dpf
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.view.NotCrossScrollableLinearLayoutManager
import kotlinx.serialization.Serializable

class HelloTrendingFragment : UpToTopFragment() {
    private val titles by lazy { resources.getStringArray(R.array.mode_list).toList() }
    private val restrictLevel by lazy {
        if (!PxEZApp.instance.pre.getBoolean(
                "r18on",
                false
            ) or (AppDataRepo.currentUser.x_restrict == 0)
        ) 5
        else if (AppDataRepo.currentUser.x_restrict == 1) 1 else 0
    }

    @Serializable
    class TagsBindingItem(val name: String, var index: Int) :
        AbstractBindingItem<ViewTagsItemBinding>(), IDraggable {
        override val type: Int = R.id.item
        override val isDraggable: Boolean = true
        override fun bindView(binding: ViewTagsItemBinding, payloads: List<Any>) {
            binding.textview.text = name
            binding.checkBox.isChecked = isSelected
        }

        override fun createBinding(
            inflater: LayoutInflater,
            parent: ViewGroup?
        ): ViewTagsItemBinding {
            return ViewTagsItemBinding.inflate(inflater, parent, false)
        }
    }

    private lateinit var titleModels: List<TagsBindingItem>
    private val adapter by lazy { RankingMAdapter(this, restrictLevel) }
    override val fragmentGetter: (Int) -> Fragment? = { adapter.fragments[it] }

    override fun loadData() {
        binding.viewpager.adapter = adapter

        //fix: 旋转后tab选择错误: viewpager.adapter设置后恢复正确的currentItem
        topTab = binding.tablayout.getTabAt(binding.viewpager.currentItem)
        binding.tablayout.selectTab(topTab)
        /* //do not use TabLayoutMediator to prevent wrong anim after overriding layout manager
        // after overriding layout manager: tab无法显示选择效果
        val upToTopListener = UpToTopListener(this)
        //do not use TabLayoutMediator to prevent wrong anim after overriding layout manager
        AutoTabLayoutMediator(binding.tablayout, binding.viewpager) { tab, position ->
            tab.text = titles[position]
        }.setOnTabReSelectedStrategy{ upToTopListener.onTabReselected(it) }.attach()
        */
        //binding.viewpager.isUserInputEnabled = false
        //binding.tablayout.setupWithViewPager(binding.viewpager)
        binding.tablayout.clearOnTabSelectedListeners()
        binding.tablayout.addOnTabSelectedListener(UpToTopListener(fragmentGetter) {
            binding.viewpager.setCurrentItem(it.tag as Int, false)
            topTab = it
        })
        binding.imageviewRank.setOnClickListener {
            MaterialDialogs(requireActivity()).show {
                setTitle(R.string.sort_by)
                val fastAdapter = setItems(titleModels, true)
                fastAdapter.onClick { _, _, item, position ->
                    item.isSelected = !item.isSelected
                    fastAdapter.notifyAdapterItemChanged(position, true)
                    true
                }.onItemClick(R.id.checkBox) { v, adapter, item, position ->
                    item.isSelected = (v as MaterialCheckBox).isChecked
                    return@onItemClick true
                }
                confirmButton { _, _ -> configTabs() }
            }
        }
        binding.imageviewRank.animate()
            .translationXBy(40.dpf)
            //.translationZBy(40.dpf)
            .setDuration(2000)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.imageviewRank.text = ""
                    binding.imageviewRank.translationX = 0f
                }
            }).start()
    }
    private lateinit var TAG: String
    private lateinit var binding: FragmentHelloTrendingBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHelloTrendingBinding.inflate(inflater, container, false)
        savedInstanceState?.getIntArray("index")?.also {
            titleModels = it.map { i -> TagsBindingItem(titles[i], i) }
            savedInstanceState.getBooleanArray("selected")
                ?.forEachIndexed { i, it -> titleModels[i].isSelected = it }
        } ?: run {
            titleModels = titles.dropLast(restrictLevel).mapIndexed { i, it ->
                TagsBindingItem(it, i).apply { isSelected = true }
            }
        }
        configTabs()
        // use custom layout manager to prevent nested scrolling
        (binding.viewpager.getChildAt(0) as RecyclerView).apply {
            //var m = binding.viewpager.javaClass.getDeclaredField("mAccessibilityProvider")
            //m.isAccessible = true
            //val mAccessibilityProvider = m.get(binding.viewpager)
            val mLayoutManager =
                NotCrossScrollableLinearLayoutManager(this.context, this, binding.viewpager)
            layoutManager = mLayoutManager
            var m = binding.viewpager.javaClass.getDeclaredField("mLayoutManager")
            m.isAccessible = true
            m.set(binding.viewpager, mLayoutManager)
            m = binding.viewpager.javaClass.getDeclaredField("mPageTransformerAdapter")
            m.isAccessible = true
            val s = m.get(binding.viewpager)
            m = s.javaClass.getDeclaredField("mLayoutManager")
            m.isAccessible = true
            m.set(s, mLayoutManager)
        }
        return binding.root
    }

    private fun configTabs() {
        binding.tablayout.removeAllTabs()
        for (i in titleModels) {
            val tab = binding.tablayout.newTab()
            tab.text = i.name
            tab.tag = i.index //important: mark tab position
            if (!i.isSelected) tab.view.visibility = View.GONE
            binding.tablayout.addTab(tab, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("index", titleModels.map { it.index }.toIntArray())
        outState.putBooleanArray("selected", titleModels.map { it.isSelected }.toBooleanArray())
    }

    companion object {
        @JvmStatic
        fun newInstance(tag: String) =
            HelloTrendingFragment().apply {
                TAG = tag
            }
    }
}
