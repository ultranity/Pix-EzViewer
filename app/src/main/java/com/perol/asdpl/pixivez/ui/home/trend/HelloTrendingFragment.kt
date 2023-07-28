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
import androidx.recyclerview.widget.RecyclerView
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.LazyFragment
import com.perol.asdpl.pixivez.databinding.FragmentHelloTrendingBinding
import com.perol.asdpl.pixivez.objects.UpToTopListener
import com.perol.asdpl.pixivez.objects.dp
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.view.NotCrossScrollableLinearLayoutManager

class HelloTrendingFragment : LazyFragment() {
    private val titles by lazy { resources.getStringArray(R.array.modellist) }

    override fun loadData() {
        val isR18on = PxEZApp.instance.pre.getBoolean("r18on", false)
        val adapter = RankingMAdapter(this, isR18on)
        binding.viewpager.adapter = adapter

        //fix: 旋转后tab选择错误: viewpager.adapter设置后恢复正确的currentItem
        binding.tablayout.selectTab(binding.tablayout.getTabAt(binding.viewpager.currentItem))

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
        binding.tablayout.addOnTabSelectedListener(UpToTopListener(
            requireContext(),
            { adapter.fragments[it] }) {
            binding.viewpager.setCurrentItem(it.position, false)
        })
        binding.imageviewRank.setOnClickListener {
            //TODO: sort/config list
        }
    }

                    /**
                     * 当拖拽动作完成且松开手指时触发
                     */
                    override fun onDrag(
                        source: BindingAdapter.BindingViewHolder,
                        target: BindingAdapter.BindingViewHolder
                    ) {
                        // 这是拖拽交换后回调, 这里可以同步服务器
                    }
                })
            }.models = titleModels
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.sort_by)
                .setView(listView.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    configTabs()
                }.show()
        }
        binding.imageviewRank.animate()
            .translationXBy(binding.imageviewRank.measuredWidth.toFloat() - 24.dp)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun configTabs() {
        binding.tablayout.removeAllTabs()
        for (i in titleModels) {
            val tab = binding.tablayout.newTab()
            tab.text = i.title
            tab.tag = i.index
            if (!i.isChecked) tab.view.visibility = View.GONE
            binding.tablayout.addTab(tab, false)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(tag: String) =
            HelloTrendingFragment().apply {
                TAG = tag
            }
    }
}
