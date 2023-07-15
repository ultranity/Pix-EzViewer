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

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.chip.Chip
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.activity.SearchResultActivity
import com.perol.asdpl.pixivez.adapters.TrendingTagAdapter
import com.perol.asdpl.pixivez.databinding.FragmentSearchTrendBinding
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.viewmodel.TrendTagViewModel
import io.reactivex.disposables.CompositeDisposable

class TrendTagFragment : Fragment() {
    private val mDisposable = CompositeDisposable()

    companion object {
        fun newInstance() = TrendTagFragment()
    }

    private lateinit var viewModel: TrendTagViewModel

    private lateinit var binding: FragmentSearchTrendBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchTrendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textViewClearn.setOnClickListener {
            viewModel.sethis()
        }
        viewModel = ViewModelProvider(this)[TrendTagViewModel::class.java]
        mDisposable.add(
            viewModel.getIllustTrendTags().subscribe({
                if (it != null) {
                    binding.recyclerviewSearhm.layoutManager =
                        StaggeredGridLayoutManager(requireContext().resources.configuration.orientation * 3, StaggeredGridLayoutManager.VERTICAL)
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
                        // var id = it.trend_tags[position].illust.id
                        // val arrayList = LongArray(1)
                        // arrayList[0] = id
                        // bundle.putLongArray("illustidlist", arrayList)
                        // bundle.putLong("illustid", id)
                        // //val arrayList = ArrayList<Illust>(1)
                        // //arrayList.add(it.trend_tags[position].illust)
                        bundle.putInt("position", position)
                        bundle.putLong("illustid", it.trend_tags[position].illust.id)
                        DataHolder.setIllustsList(it.trend_tags.map { it.illust })
                        val intent = Intent(requireActivity(), PictureActivity::class.java)
                        intent.putExtras(bundle)
                        val options = if (PxEZApp.animationEnable) {
                            val mainimage = view.findViewById<ImageView>(R.id.imageview_trendingtag)
                            ActivityOptions.makeSceneTransitionAnimation(
                                context as Activity,
                                Pair(mainimage, "mainimage")
                            ).toBundle()
                        } else null
                        startActivity(intent, options)
                        true
                    }
                }
            }, {})
        )
        viewModel.searchhistroy.observe(viewLifecycleOwner) { it ->
            binding.chipgroup.removeAllViews()
            it.forEach {
                binding.chipgroup.addView(getChip(it))
            }

            if (it.isNotEmpty()) {
                binding.textViewClearn.visibility = View.VISIBLE
            }
            else {
                binding.textViewClearn.visibility = View.GONE
            }
        }
    }

    override fun onStop() {
        super.onStop()

        // clear all the subscriptions
        mDisposable.clear()
    }

    private fun getChip(word: String): Chip {
        val chip = Chip(activity)
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
        val nameQuery = query.split('|')[0]
        bundle.putString("searchword", nameQuery)
        val intent = Intent(requireActivity(), SearchResultActivity::class.java)
        intent.putExtras(bundle)
        startActivityForResult(intent, 775)
    }
}
