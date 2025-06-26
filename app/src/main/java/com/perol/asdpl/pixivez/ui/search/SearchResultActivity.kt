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

package com.perol.asdpl.pixivez.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivitySearchResultBinding
import com.perol.asdpl.pixivez.objects.UpToTopListener
import com.perol.asdpl.pixivez.ui.FragmentActivity

class SearchResultActivity : RinkActivity() {
    companion object {
        fun start(context: Context, keyword: String, type: Int = 0) {
            val intent =
                Intent(context, SearchResultActivity::class.java).setAction("search.result")
            intent.putExtra("keyword", keyword)
            intent.putExtra("type", type)
            context.startActivity(intent)
        }
    }

    private var keyword: String = ""
    var type: Int = 0

    lateinit var binding: ActivitySearchResultBinding
    var arrayList = ArrayList<Fragment>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySearchResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (intent.extras != null) {
            keyword = intent.extras!!.getString("keyword")!!
            type = intent.extras!!.getInt("type")
        }
        initView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_block_tag, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                this.finish() // back button
                return true
            }

            R.id.action_block_tag -> {
                FragmentActivity.start(this, "Block")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initView() {
        binding.tablayout.setupWithViewPager(binding.contentSearchResult.viewpager)
        binding.contentSearchResult.viewpager.adapter =
            SearchResultAdapter(this, supportFragmentManager, keyword)
        binding.tablayout.addOnTabSelectedListener(
            UpToTopListener(supportFragmentManager) {
                binding.imagebuttonSection.visibility =
                    if (it.position == 0) View.VISIBLE else View.GONE
            }
        )
        binding.tablayout.getTabAt(type)?.select()
        binding.searchtext.text = keyword
        binding.searchtext.setOnClickListener {
            setResult(
                RESULT_OK,
                Intent().apply {
                    putExtra("word", binding.searchtext.text.toString())
                }
            )
            finish()
        }
        binding.imagebuttonSection.setOnClickListener {
            SearchSectionDialog().apply {
                arguments = Bundle().apply {
                    putString("word", keyword)
                }
            }.show(supportFragmentManager)
        }
    }
}
