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
package com.perol.asdpl.pixivez.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.TagsAdapter
import com.perol.asdpl.pixivez.responses.TagsBean
import com.perol.asdpl.pixivez.viewmodel.PictureXViewModel

class TagsBookMarkDialog : DialogFragment() {
    companion object;

    lateinit var recyclerView: RecyclerView
    lateinit var editText: EditText
    private lateinit var pictureXViewModel: PictureXViewModel
    lateinit var imageButton: ImageButton

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_bookmark, null)
            val tagsAdapter = TagsAdapter(R.layout.view_tags_item, null)
            recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView).apply {
                layoutManager = LinearLayoutManager(activity)
                adapter = tagsAdapter
            }
            editText = view.findViewById(R.id.edittext)
            imageButton = view.findViewById(R.id.add)

            imageButton.setOnClickListener {
                if (editText.text.isNotBlank() && pictureXViewModel.tags.value != null) {
                    tagsAdapter.addData(
                        0,
                        TagsBean(
                            is_registered = true,
                            name = editText.text.toString()
                        )
                    )
                    editText.text.clear()
                    recyclerView.smoothScrollToPosition(0)
                }
            }
            pictureXViewModel =
                ViewModelProvider(requireParentFragment())[PictureXViewModel::class.java]
            pictureXViewModel.illustDetail.value?.let{
                tagsAdapter.setNewInstance(it.tags.map {
                    TagsBean(
                        is_registered = false,
                        name = it.toString()
                        )
                }.toMutableList())
            }
            pictureXViewModel.tags.observe(this) {
                tagsAdapter.setNewInstance(it.tags.toMutableList())
            }
            pictureXViewModel.fabOnLongClick()
            builder
                .setView(view)
                .setNegativeButton(
                    android.R.string.cancel
                ) { dialog, id ->

                }
                .setPositiveButton(R.string.bookmark_public) { _, _ ->
                    //if (pictureXViewModel.tags.value != null)
                        pictureXViewModel.onDialogClick(false)
                }

                .setNeutralButton(R.string.bookmark_private) { _, _ ->
                    //if (pictureXViewModel.tags.value != null)
                        pictureXViewModel.onDialogClick(true)
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}