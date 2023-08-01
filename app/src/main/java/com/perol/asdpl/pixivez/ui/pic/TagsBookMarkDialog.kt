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
package com.perol.asdpl.pixivez.ui.pic

import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseDialogFragment
import com.perol.asdpl.pixivez.data.model.TagsBean
import com.perol.asdpl.pixivez.databinding.DialogBookmarkBinding

class TagsBookMarkDialog : BaseDialogFragment<DialogBookmarkBinding>() {

    private lateinit var pictureXViewModel: PictureXViewModel
    override fun onCreateDialogBinding(builder: MaterialAlertDialogBuilder) {
        val tagsAdapter = TagsAdapter(R.layout.view_tags_item, null)
        binding.recyclerview.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = tagsAdapter
        }
        binding.add.setOnClickListener {
            if (!(binding.editText.text.isNullOrBlank() || pictureXViewModel.tags.value == null)) {
                tagsAdapter.addData(
                    0,
                    TagsBean(
                        name = binding.editText.text.toString(),
                        is_registered = true,
                    )
                )
                binding.editText.text!!.clear()
                binding.recyclerview.smoothScrollToPosition(0)
            }
        }
        pictureXViewModel =
            ViewModelProvider(requireParentFragment())[PictureXViewModel::class.java]
        pictureXViewModel.illustDetail.value?.let {
            tagsAdapter.setNewInstance(
                it.tags.map {
                    TagsBean(
                        name = it.toString(),
                        is_registered = false,
                    )
                } as MutableList<TagsBean> //TODO: check if need toMutableList()
            )
        }
        pictureXViewModel.tags.observe(this) {
            tagsAdapter.setNewInstance(it.tags)
        }
        pictureXViewModel.onLoadTags() //TODO: refresh when asked
        // Create the AlertDialog object and return it
        builder
            .setNegativeButton(android.R.string.cancel) { dialog, id -> }
            .setPositiveButton(R.string.bookmark_public) { _, _ ->
                // if (pictureXViewModel.tags.value != null)
                pictureXViewModel.onDialogClick(false)
            }
            .setNeutralButton(R.string.bookmark_private) { _, _ ->
                // if (pictureXViewModel.tags.value != null)
                pictureXViewModel.onDialogClick(true)
            }
    }

    companion object {
        const val TAG: String = "BookmarkTags"
    }
}
