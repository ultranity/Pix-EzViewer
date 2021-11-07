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

package com.perol.asdpl.pixivez.fragments

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.chip.Chip
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.activity.SearchResultActivity
import com.perol.asdpl.pixivez.adapters.TrendingTagAdapter
import com.perol.asdpl.pixivez.databinding.FragmentBlockTagBinding
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.responses.Illust
import io.reactivex.disposables.CompositeDisposable
import com.perol.asdpl.pixivez.databinding.TrendTagFragmentBinding
class TrendTagFragment : Fragment() {
    private val mDisposable = CompositeDisposable()

    companion object {
        fun newInstance() = TrendTagFragment()
    }

    private lateinit var viewModel: TrendTagViewModel

    private lateinit var binding: TrendTagFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
		binding = TrendTagFragmentBinding.inflate(inflater, container, false)
		return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textViewClearn.setOnClickListener {

            viewModel.sethis()
        }
    }

    override fun onStop() {
        super.onStop()

        // clear all the subscriptions
        mDisposable.clear()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TrendTagViewModel::class.java)
        mDisposable.add(viewModel.getIllustTrendTags().subscribe({
            if (it != null) {
                binding.recyclerviewSearhm.layoutManager =
                    StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
                val trendingtagAdapter =
                    TrendingTagAdapter(R.layout.view_trendingtag_item, it.trend_tags)
                binding.recyclerviewSearhm.adapter = trendingtagAdapter
                binding.recyclerviewSearhm.isNestedScrollingEnabled = false
                trendingtagAdapter.setOnItemClickListener { adapter, view, position ->
                    val searchword = it.trend_tags[position].tag
                    upToPage(searchword)
                    viewModel.addhistory(searchword)
                }
                trendingtagAdapter.setOnItemLongClickListener { adapter, view, position ->
                    val bundle = Bundle()
                    //var id = it.trend_tags[position].illust.id
                    //val arrayList = LongArray(1)
                    //arrayList[0] = id
                    //bundle.putLongArray("illustidlist", arrayList)
                    //bundle.putLong("illustid", id)
                    ////val arrayList = ArrayList<Illust>(1)
                    ////arrayList.add(it.trend_tags[position].illust)
                    bundle.putInt("position", position)
                    bundle.putLong("illustid", it.trend_tags[position].illust.id)
                    DataHolder.setIllustsList(it.trend_tags.map { it.illust })
                    val intent2 = Intent(requireActivity(), PictureActivity::class.java)
                    intent2.putExtras(bundle)
                    startActivity(intent2)
                    true
                }
            }
        }, {}))
        viewModel.searchhistroy.observe(viewLifecycleOwner, Observer { it ->
            binding.chipgroup.removeAllViews()
            it.forEach {
                binding.chipgroup.addView(getChip(it))
            }

            if (it.isNotEmpty()) binding.textViewClearn.visibility = View.VISIBLE
            else binding.textViewClearn.visibility = View.GONE
        })
    }

    private fun getChip(word: String): Chip {
        val chip = Chip(activity)
        val paddingDp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 5f,
            resources.displayMetrics
        ).toInt()
        chip.text = word
        chip.setOnClickListener {
            upToPage(word)
        }
        chip.setOnLongClickListener {
            chip.visibility = View.GONE
            viewModel.deleteHistory(word)
            true
        }
        return chip
    }

    private fun upToPage(query: String) {
        val bundle = Bundle()
        var nameQuery = query
        if (query.contains('|')) {
            val splits = query.split('|')
            nameQuery = splits[0]
        }
        bundle.putString("searchword", nameQuery)
        val intent = Intent(requireActivity(), SearchResultActivity::class.java)
        intent.putExtras(bundle)
        startActivityForResult(intent, 775)
    }
}
