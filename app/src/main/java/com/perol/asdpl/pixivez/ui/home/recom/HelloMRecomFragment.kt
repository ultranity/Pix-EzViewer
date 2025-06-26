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

package com.perol.asdpl.pixivez.ui.home.recom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.perol.asdpl.pixivez.databinding.FragmentHelloMainBinding
import com.perol.asdpl.pixivez.objects.UpToTopFragment
import com.perol.asdpl.pixivez.objects.UpToTopListener
import com.perol.asdpl.pixivez.objects.argument

/**
 * Main page Fragment with Recommended illusts and users
 */
class HelloMRecomFragment : UpToTopFragment() {

    private var TAG: String by argument()
    private lateinit var binding: FragmentHelloMainBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHelloMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun loadData() {
        binding.viewpager.adapter = HelloMRecomViewPager(this, childFragmentManager)
        binding.tablayout.setupWithViewPager(binding.viewpager)
        binding.tablayout.clearOnTabSelectedListeners()
        binding.tablayout.addOnTabSelectedListener(UpToTopListener(this) {
            binding.viewpager.setCurrentItem(it.position, false)
            topTab = it
        })
        topTab = binding.tablayout.getTabAt(binding.viewpager.currentItem)
    }

    companion object {
        @JvmStatic
        fun newInstance(tag: String) =
            HelloMRecomFragment().apply {
                TAG = tag
            }
    }
}
