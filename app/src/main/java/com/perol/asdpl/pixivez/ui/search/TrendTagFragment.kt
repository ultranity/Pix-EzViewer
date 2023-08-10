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
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.databinding.FragmentSearchTrendBinding
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.getMaxColumn
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.pic.PictureActivity

class TrendTagFragment : Fragment() {

    private val viewModel: TrendTagViewModel by viewModels()
    private lateinit var binding: FragmentSearchTrendBinding
    private var foldedChipIndex: Int = 15

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
        //binding.recyclerview.isNestedScrollingEnabled = false
        binding.recyclerview.layoutManager =
            StaggeredGridLayoutManager(
                getMaxColumn(50 + requireContext().resources.configuration.orientation * 100),
                StaggeredGridLayoutManager.VERTICAL
            )
        val trendTagAdapter = TrendingTagAdapter(null)
        binding.recyclerview.adapter = trendTagAdapter
        trendTagAdapter.setOnItemClickListener { adapter, view, position ->
            val keyword = trendTagAdapter.data[position].tag
            upToPage(keyword)
            viewModel.addHistory(keyword)
        }
        trendTagAdapter.setOnItemLongClickListener { adapter, view, position ->
            DataHolder.setIllustList(
                trendTagAdapter.data.map { it.illust }
                        as MutableList<Illust> //TODO: check if need toMutableList()
            )
            val options = if (PxEZApp.animationEnable) {
                val mainimage = view.findViewById<ImageView>(R.id.imageview_trendingtag)
                ActivityOptions.makeSceneTransitionAnimation(
                    context as Activity,
                    Pair(mainimage, "mainimage")
                ).toBundle()
            } else null
            PictureActivity.start(
                requireContext(),
                trendTagAdapter.data[position].illust.id, position, options = options
            )
            true
        }
        viewModel.trendTags.observe(viewLifecycleOwner) {
            if (it != null) {
                trendTagAdapter.setNewInstance(it)
            }
        }
        viewModel.searchHistory.observe(viewLifecycleOwner) {
            binding.chipgroup.removeAllViews()
            it.take(foldedChipIndex).forEachIndexed { index, s ->
                val chip = getChip(s)
                binding.chipgroup.addView(chip)
            }
            //NOT WORK: if (binding.chipgroup.getRowIndex(chip) > 3){
            //binding.chipgroup.removeViewAt(index)
            if (it.size > foldedChipIndex) binding.chipgroup.addView(getExpandChip())
            if (it.isNotEmpty()) binding.chipgroup.addView(getClearChip())
        }
        viewModel.getIllustTrendTags()
    }

    private fun getChip(word: String): Chip {
        val chip = Chip(requireContext())
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

    private fun getClearChip(): Chip {
        val chip = Chip(requireContext())
        chip.setChipIconResource(R.drawable.ic_action_del)
        chip.setChipIconTintResource(ThemeUtil.getTextColorPrimaryResID(requireContext()))
        chip.setText(R.string.clearhistory)
        chip.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clearhistory)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    viewModel.clearHistory()
                    binding.chipgroup.removeAllViews()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
        }
        return chip
    }

    private fun getExpandChip(): Chip {
        val chip = Chip(requireContext())
        chip.setChipIconResource(R.drawable.ic_menu_more)
        chip.setChipIconTintResource(ThemeUtil.getTextColorPrimaryResID(requireContext()))
        chip.iconEndPadding = 0F
        chip.setOnClickListener {
            binding.chipgroup.removeViewAt(foldedChipIndex)
            viewModel.searchHistory.value!!.drop(foldedChipIndex).forEachIndexed { index, s ->
                val chip = getChip(s)
                binding.chipgroup.addView(chip, index + foldedChipIndex)
            }
            binding.chipgroup.addView(getFoldChip(), viewModel.searchHistory.value!!.size)
        }
        return chip
    }

    private fun getFoldChip(): Chip {
        val chip = Chip(requireContext())
        chip.setChipIconResource(R.drawable.ic_action_fold)
        chip.setChipIconTintResource(ThemeUtil.getTextColorPrimaryResID(requireContext()))
        chip.setOnClickListener {
            (foldedChipIndex.until(binding.chipgroup.size - 1)).forEach { _ ->
                binding.chipgroup.removeViewAt(foldedChipIndex)
            }
            binding.chipgroup.addView(getExpandChip(), foldedChipIndex)
        }
        return chip
    }

    private fun upToPage(query: String) {
        val bundle = Bundle()
        val nameQuery = query.split('|')[0]
        bundle.putString("keyword", nameQuery)
        val intent = Intent(requireActivity(), SearchResultActivity::class.java)
        intent.putExtras(bundle)
        startActivityForResult(intent, 775)
    }
}