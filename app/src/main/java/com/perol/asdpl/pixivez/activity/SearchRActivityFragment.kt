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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.TagsTextAdapter
import com.perol.asdpl.pixivez.databinding.FragmentSearchRBinding
import com.perol.asdpl.pixivez.responses.Tag
import com.perol.asdpl.pixivez.viewmodel.TagsTextViewModel

/**
 * A placeholder fragment containing a simple view.
 */
class SearchRActivityFragment : Fragment() {

    private lateinit var binding: FragmentSearchRBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchRBinding.inflate(inflater, container, false)
        return binding.root
    }

    private lateinit var tagsTextViewModel: TagsTextViewModel
    private lateinit var tagsTextAdapter: TagsTextAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tagsTextAdapter = TagsTextAdapter(R.layout.tagstext_item)
        binding.recyclerview.layoutManager = LinearLayoutManager(activity)
        binding.recyclerview.adapter = tagsTextAdapter
        tagsTextAdapter.setOnItemClickListener { adapter, view, position ->
            val tag = tags[position]
            tagsTextViewModel.addhistory(tag)
            val bundle = Bundle()
            bundle.putString("searchword", tag.name)
            val intent = Intent(requireActivity(), SearchResultActivity::class.java)
            intent.putExtras(bundle)
            startActivityForResult(intent, 775)
        }
        tagsTextViewModel = ViewModelProvider(requireActivity())[TagsTextViewModel::class.java]
        tagsTextViewModel.tags.observe(viewLifecycleOwner) {
            tagsTextAdapter.setNewInstance(it.toMutableList())
            tags.clear()
            tags.addAll(it)
        }
    }

    val tags = ArrayList<Tag>()
}
