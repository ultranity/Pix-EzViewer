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

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.databinding.FragmentHelloTrendingBinding
import com.perol.asdpl.pixivez.base.LazyFragment
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.view.NotCrossScrollableLinearLayoutManager
import java.util.Calendar

/**
 * A simple [Fragment] subclass.
 * Use the [HelloTrendingFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class HelloTrendingFragment : LazyFragment() {
    private val titles by lazy { resources.getStringArray(R.array.modellist) }
    var exitTime: Long = 0
    override fun loadData() {
        val shareModel =
            ViewModelProvider(requireActivity())[RankingShareViewModel::class.java]
        val isR18on = PxEZApp.instance.pre.getBoolean("r18on", false)
        binding.viewpager.adapter = RankingMAdapter(this, isR18on)
        //binding.viewpager.isUserInputEnabled = false

        /* do not use TabLayoutMediator to prevent wrong anim after overriding layout manager
        AutoTabLayoutMediator(binding.tablayout, binding.viewpager) { tab, position ->
            tab.text = titles[position]
        }.apply {
            onTabReSelectedStrategy = object : TabSelectedStrategy {
                override fun invoke(tab: Tab) {
                    if ((System.currentTimeMillis() - exitTime) > 3000) {
                        Toast.makeText(
                            PxEZApp.instance,
                            getString(R.string.back_to_the_top),
                            Toast.LENGTH_SHORT
                        ).show()
                        exitTime = System.currentTimeMillis()
                    }
                    else {
                        (binding.viewpager.adapter as RankingMAdapter)
                            .fragments[tab.position]?.view
                            ?.findViewById<RecyclerView>(R.id.recyclerview)
                            ?.scrollToPosition(0)
                    }
                }
            }
        }.attach()
        */
        binding.tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: Tab) {
                if ((System.currentTimeMillis() - exitTime) > 3000) {
                    Toast.makeText(
                        PxEZApp.instance,
                        getString(R.string.back_to_the_top),
                        Toast.LENGTH_SHORT
                    ).show()
                    exitTime = System.currentTimeMillis()
                } else {
                    (binding.viewpager.adapter as RankingMAdapter)
                        .fragments[tab.position]?.view
                        ?.findViewById<RecyclerView>(R.id.recyclerview)
                        ?.scrollToPosition(0)
                }
            }

            override fun onTabUnselected(tab: Tab) {}

            override fun onTabSelected(tab: Tab) {
                binding.viewpager.setCurrentItem(tab.position, false)
            }
        })
        //binding.viewpager.isUserInputEnabled = false
        //binding.tablayout.setupWithViewPager(binding.viewpager)
        val calendar = Calendar.getInstance()
        val yearNow = calendar.get(Calendar.YEAR)
        val monthNow = calendar.get(Calendar.MONTH) + 1
        val dayNow = calendar.get(Calendar.DAY_OF_MONTH)
        val dateNow = "$yearNow-$monthNow-$dayNow"
        binding.imageviewDate.apply {
            setOnClickListener {
                shareModel.apply {
                    val dateDialog = DatePickerDialog(
                        requireActivity(),
                        { p0, year1, month1, day1 ->
                            val monthR = month1 + 1
                            picDateShare.value = if ("$year1-$monthR-$day1" == dateNow) {
                                null
                            } else {
                                "$year1-$monthR-$day1"
                            }
                            year.value = year1
                            month.value = month1
                            day.value = day1
                        },
                        year.value!!,
                        month.value!!,
                        day.value!!
                    )
                    dateDialog.datePicker.maxDate = System.currentTimeMillis()
                    dateDialog.show()
                }
            }
            setOnLongClickListener {
                shareModel.hideBookmarked.value = 1 - shareModel.hideBookmarked.value!!
                true
            }
        }
    }

    private lateinit var TAG: String
    private lateinit var binding: FragmentHelloTrendingBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHelloTrendingBinding.inflate(inflater, container, false)
        binding.tablayout.removeAllTabs()
        for (i in titles) {
            val tab = binding.tablayout.newTab()
            tab.text = i
            binding.tablayout.addTab(tab, false)
        }
        binding.tablayout.selectTab(binding.tablayout.getTabAt(0))

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

    companion object {
        @JvmStatic
        fun newInstance(tag: String) =
            HelloTrendingFragment().apply {
                TAG = tag
            }
    }
}