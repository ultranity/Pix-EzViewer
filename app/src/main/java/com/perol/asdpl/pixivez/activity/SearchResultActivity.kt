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

package com.perol.asdpl.pixivez.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.SearchResultAdapter
import com.perol.asdpl.pixivez.databinding.ActivitySearchResultBinding
import com.perol.asdpl.pixivez.fragments.SearchIllustFragment
import com.perol.asdpl.pixivez.fragments.SearchUsersListFragment

class SearchResultActivity : RinkActivity() {
    companion object{
        fun start(context:Context, searchword:String, type:Int=0){
            val intent = Intent(context, SearchResultActivity::class.java)
            intent.putExtra("searchword", searchword)
            intent.putExtra("type", type)
            context.startActivity(intent)
        }
    }
    private var searchword: String = ""
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
            searchword = intent.extras!!.getString("searchword")!!
            type = intent.extras!!.getInt("type")
        }
        initView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search_block, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                this.finish() // back button
                return true
            }
            R.id.action_bloctag -> {
                startActivity(Intent(this, BlockActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initView() {
        binding.tablayoutSearchresult.setupWithViewPager(binding.contentSearchResult.viewpageSearchresult)
        arrayList.add(SearchIllustFragment.newInstance(searchword))
        arrayList.add(SearchUsersListFragment.newInstance(searchword))
        binding.contentSearchResult.viewpageSearchresult.adapter = SearchResultAdapter(this, supportFragmentManager, arrayList)
        binding.tablayoutSearchresult.getTabAt(type)?.select()
        binding.contentSearchResult.viewpageSearchresult.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {}

            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        binding.pickbar.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.pickbar.visibility = View.GONE
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        binding.searchtext.setOnClickListener {
            setResult(
                Activity.RESULT_OK,
                Intent().apply {
                    putExtra("word", binding.searchtext.text.toString())
                }
            )
            finish()
        }
    }
}
