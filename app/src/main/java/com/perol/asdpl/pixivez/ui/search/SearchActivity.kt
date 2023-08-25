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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivitySearchBinding
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import com.perol.asdpl.pixivez.ui.user.UserMActivity

class SearchActivity : RinkActivity() {
    companion object {
        fun start(context: Context, keyword: String? = null) {
            val intent = Intent(context, SearchActivity::class.java).setAction("search.start")
            keyword?.let {
                intent.putExtra("keyword", it)
            }
            context.startActivity(intent)
        }
    }

    lateinit var searchSuggestionFragment: SearchSuggestionFragment
    lateinit var trendTagFragment: TrendTagFragment
    lateinit var searchSuggestionViewModel: SearchSuggestionViewModel
    lateinit var trendTagViewModel: TrendTagViewModel
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedCallback.handleOnBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (searchSuggestionViewModel.autoCompleteTags.value?.isEmpty() != false) {
                this@SearchActivity.finish()
            } else {
                searchSuggestionViewModel.autoCompleteTags.value = listOf()
                binding.suggestions.visibility = View.GONE
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val word = data.getStringExtra("word")
            binding.searchviewText.setQuery(word, false)
        } else if (intent.extras != null) {
            finish()
        }
    }

    private lateinit var binding: ActivitySearchBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        searchSuggestionViewModel = ViewModelProvider(this)[SearchSuggestionViewModel::class.java]
        trendTagViewModel = ViewModelProvider(this)[TrendTagViewModel::class.java]
        if (savedInstanceState == null) {
            searchSuggestionFragment = SearchSuggestionFragment()
            trendTagFragment = TrendTagFragment()
            supportFragmentManager.commit {
                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack so the user can navigate back
                add(R.id.suggestions, searchSuggestionFragment)
                add(R.id.fragment, trendTagFragment)
            }
        } else {
            searchSuggestionFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                "searchActivityFragment"
            ) as SearchSuggestionFragment
            trendTagFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                "trendTagFragment"
            ) as TrendTagFragment
        }
        binding.tablayoutSearch.clearOnTabSelectedListeners()
        binding.tablayoutSearch.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    if (tab.position == 0) {
                        binding.searchviewText.inputType = EditorInfo.TYPE_CLASS_TEXT
                    } else {
                        binding.searchviewText.inputType = EditorInfo.TYPE_CLASS_NUMBER
                    }
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        // binding.searchviewText.onActionViewExpanded()
        binding.searchviewText.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    when (binding.tablayoutSearch.selectedTabPosition) {
                        0 -> {
                            trendTagViewModel.addHistory(query)
                            searchFor(query)
                        }

                        1 -> {
                            if (!query.isDigitsOnly()) {
                                return true
                            }
                            PictureActivity.start(this@SearchActivity, query.toLong())
                        }

                        2 -> {
                            if (!query.isDigitsOnly()) {
                                return true
                            }
                            UserMActivity.start(this@SearchActivity, query.toLong())
                        }
                    }
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (binding.tablayoutSearch.selectedTabPosition != 0) {
                    return true
                }
                if (!newText.isNullOrBlank()) {
                    searchSuggestionViewModel.onQueryTextChange(newText)
                }
                return true
            }
        })

        intent.extras?.getString("keyword")?.let {
            binding.searchviewText.setQuery(it, true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        supportFragmentManager.putFragment(
            outState,
            "searchActivityFragment",
            searchSuggestionFragment
        )
        supportFragmentManager.putFragment(outState, "trendTagFragment", trendTagFragment)
        super.onSaveInstanceState(outState)
    }

    private fun searchFor(query: String) {
        val bundle = Bundle()
        bundle.putString("keyword", query)
        val intent = Intent(this, SearchResultActivity::class.java)
        intent.putExtras(bundle)
        startActivityForResult(intent, 775)
    }
}
