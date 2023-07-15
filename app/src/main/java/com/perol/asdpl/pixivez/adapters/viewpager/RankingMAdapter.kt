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

package com.perol.asdpl.pixivez.adapters.viewpager

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.perol.asdpl.pixivez.fragments.hellom.RankingMFragment
import java.util.*

class RankingMAdapter(var fragment: Fragment, private var isR18on: Boolean) :
    FragmentStateAdapter(fragment) {

    private val modelist =arrayOf(
        "day", "day_male", "day_female", "day_ai", "week_original", "week_rookie", "week", "month",
        "day_r18", "day_male_r18", "day_female_r18", "week_r18", "week_r18g"
    )

    override fun getItemCount() = if (isR18on) modelist.size else modelist.size - 5

    val fragments = WeakHashMap<Int, RankingMFragment>(3)
    override fun createFragment(position: Int): Fragment {
        if (fragments[position] ==null) {
            fragments[position] = RankingMFragment.newInstance(modelist[position], position)
        }
       return fragments[position]!!
    }
/*    override fun getItemCount() = modelist.size
    override fun createFragment(position: Int) = RankingMFragment.newInstance(modelist[position])*/
}
