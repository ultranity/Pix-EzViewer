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

package com.perol.asdpl.pixivez.ui.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivitySettingBinding
import com.perol.asdpl.pixivez.objects.screenWidthDp
import com.perol.asdpl.pixivez.objects.screenWidthPx


class SettingsActivity : RinkActivity() {
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                return true
            }

            R.id.menu_question -> {
                FirstInfoDialog().show(supportFragmentManager, "Info")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    private lateinit var binding: ActivitySettingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback {
            if (SupportDialog.checkTime(supportFragmentManager))
                finish()
        }
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.viewpager.offscreenPageLimit = 3
        if (screenWidthDp() > 840) {
            //double pannel from:https://blog.csdn.net/GuoXuan_CHN/article/details/124060216
            val recyclerview = binding.viewpager.getChildAt(0) as RecyclerView
            val padding: Int = screenWidthPx() / 2
            recyclerview.setPadding(0, 0, padding, 0)
            recyclerview.clipToPadding = false
        }

        binding.viewpager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 4

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> SettingsFragment()
                    1 -> ThemeFragment()
                    2 -> ThanksFragment()
                    else -> AboutXFragment()
                }
            }
        }
        TabLayoutMediator(binding.tablayout, binding.viewpager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.setting)
                1 -> "UI"
                2 -> getString(R.string.supporttitle)
                else -> getString(R.string.abouts)
            }
        }.attach()
        binding.viewpager.currentItem = intent.getIntExtra("page", 0)
    }
}
