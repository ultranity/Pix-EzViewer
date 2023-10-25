/*
 * MIT License
 *
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

package com.perol.asdpl.pixivez.ui.user

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.core.PicListFragment
import com.perol.asdpl.pixivez.core.TAG_TYPE
import com.perol.asdpl.pixivez.objects.WeakValueHashMap

class UserMPagerAdapter(
    var activity: AppCompatActivity,
    var userid: Int
) : FragmentStateAdapter(activity) {

    val fragments = WeakValueHashMap<Int, Fragment>(4)
    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        if (fragments[position] == null) {
            fragments[position] = when (position) {
                0 -> PicListFragment.newInstance(
                    TAG_TYPE.UserIllust.name,
                    0,
                    mutableMapOf("userid" to userid)
                )
                1 -> PicListFragment.newInstance(
                    TAG_TYPE.UserManga.name, 1,
                    mutableMapOf("userid" to userid)
                )
                2 -> PicListFragment.newInstance(
                    TAG_TYPE.UserBookmark.name, 2,
                    mutableMapOf("userid" to userid)
                )
                else -> UserInfoFragment.newInstance(userid)
            }
        }
        return fragments[position]!!
    }

    companion object {
        fun getPageTitle(position: Int) = when (position) {
            0 -> R.string.illust
            1 -> R.string.manga
            2 -> R.string.bookmark
            else -> R.string.abouts
        }
    }
}
