/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
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
package com.perol.asdpl.pixivez.ui.manager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.content.ContentResolver
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.bumptech.glide.Glide
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivityImgManagerBinding
import com.perol.asdpl.pixivez.databinding.DialogSaveFormatBinding
import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.Storage
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class ImgManagerActivity : RinkActivity() {
    private val pickFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri ?: return@registerForActivityResult
            try {
                Storage.grant(Storage.KEY_BROWSE_DIR, uri)
            } catch (e: SecurityException) {
                CrashHandler.instance.e("ImgManager", "folder rejected", e, toast = true)
                return@registerForActivityResult
            }
            viewModel.browse(Storage.granted(Storage.KEY_BROWSE_DIR)!!)
        }
    private lateinit var binding: ActivityImgManagerBinding
    private lateinit var viewModel: ImgManagerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImgManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initBind()

        CoroutineScope(Dispatchers.IO).launch {
            FileUtil.getFileList()
        }
    }

    private val imgManagerAdapter = ImgManagerAdapter(R.layout.view_imgmanager_item)
    private fun initView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.recyclerviewImgManager.layoutManager =
            LinearLayoutManager(this)
        // binding.recyclerviewImgManager.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerviewImgManager.adapter = imgManagerAdapter
        imgManagerAdapter.addFooterView(LayoutInflater.from(this).inflate(R.layout.foot_list, null))
        // binding.recyclerviewImgManager.smoothScrollToPosition(ImgManagerAdapter.data.size)

        viewModel = ViewModelProvider(this)[ImgManagerViewModel::class.java]
        viewModel.adapter = imgManagerAdapter
        viewModel.layoutManager =
            binding.recyclerviewImgManager.layoutManager as LinearLayoutManager
        if (viewModel.dir.value == null) {
            viewModel.browse(Storage.granted(Storage.KEY_BROWSE_DIR) ?: Storage.root)
        }
        viewModel.dir.observe(this, ::load)
        binding.swipeRefreshLayout.setOnRefreshListener {
            load(viewModel.dir.value!!)
        }
        binding.swithFilter.isChecked = true
        binding.swithFilter.setOnCheckedChangeListener { compoundButton, state ->
            // viewModel.length_filter= state
            reset()
        }
        binding.swithOnce.setOnCheckedChangeListener { compoundButton, state ->
            // viewModel.rename_once = state
            reset()
        }
    }

    private fun load(dir: Uri) {
        binding.swipeRefreshLayout.isRefreshing = true
        Thread {
            viewModel.files = viewModel.list(dir)
            viewModel.task = viewModel.files!!.map { RenameTask(it) }
            runOnUiThread {
                binding.imgCount.text = viewModel.files!!.size.toString()
                binding.swipeRefreshLayout.isRefreshing = false
                imgManagerAdapter.setNewInstance(viewModel.files)
            }
        }.start()
    }

    private fun reset() {
        getInfo = false
        Glide.with(this).load(R.drawable.ic_action_search).thumbnail(0.5f).into(binding.fabStart)
        viewModel.task?.forEach {
            it.file.checked = false
        }
        imgManagerAdapter.notifyDataSetChanged()
    }

    private var getInfo = false
    private fun initBind() {
        binding.fabStart.setOnClickListener {
            viewModel.length_filter = binding.swithFilter.isChecked
            viewModel.rename_once = binding.swithOnce.isChecked
            if (viewModel.rename_once) {
                viewModel.getInfo()
            } else {
                if (getInfo) {
                    Glide.with(this).load(R.drawable.ic_action_search).into(binding.fabStart)
                    viewModel.renameAll()
                } else {
                    Glide.with(this).load(R.drawable.ic_action_play).into(binding.fabStart)
                    viewModel.getInfo()
                }
                getInfo = !getInfo
                // ImgManagerAdapter.notifyDataSetChanged()
            }
        }
        binding.fabFolder.setOnClickListener {
            pickFolder.launch(viewModel.dir.value?.takeIf { it.scheme == ContentResolver.SCHEME_CONTENT })
        }
        binding.fabSettings.setOnClickListener {
            // Setup custom view content
            val binding = DialogSaveFormatBinding.inflate(layoutInflater)
            val descTable = binding.formatDescTable
            val sampleTable = binding.formatSampleTable
            val customizedFormatInput = binding.customizedformat
            val tagSeparator = binding.tagSeparator
            customizedFormatInput.setText(viewModel.saveformat)
            tagSeparator.setText(viewModel.TagSeparator)
            val dialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT))
            dialog.show {
                title(R.string.saveformat)
                customView(view = binding.root, scrollable = true, horizontalPadding = true)
                positiveButton(R.string.save) { dialog ->
                    viewModel.saveformat = "${customizedFormatInput.text}"
                    viewModel.TagSeparator = "${tagSeparator.text}"
                    viewModel.pre.edit().putString("ImgManagerSaveFormat", viewModel.saveformat)
                        .apply()
                    viewModel.pre.edit {
                        putString("ImgManagerTagSeparator", viewModel.TagSeparator)
                    }
                    // if(getInfo){
                    getInfo = false
                    // }
                    reset()
                }
                negativeButton(android.R.string.cancel)
                lifecycleOwner(this@ImgManagerActivity)
            }
            val inputEditable = customizedFormatInput.editableText
            for (i in 1 until descTable.childCount)
                descTable.getChildAt(i).setOnClickListener {
                    inputEditable.insert(customizedFormatInput.selectionStart, it.tag.toString())
                }
            for (i in 1 until sampleTable.childCount)
                sampleTable.getChildAt(i).setOnClickListener {
                    inputEditable.clear()
                    inputEditable.insert(0, it.tag.toString())
                }
        }

        imgManagerAdapter.setOnItemClickListener { _, _, position ->
            val file = viewModel.files!![position]
            if (file.isDirectory) {
                if (file.name == PARENT) viewModel.up() else viewModel.open(file.uri)
            } else {
                val pid = viewModel.files!![position].pid
                if (pid != null) {
                    val arrayList = viewModel.files!!.subList(
                        max(position - 30, 0),
                        min(
                            viewModel.files!!.size,
                            max(position - 30, 0) + 60
                        )
                    ).mapNotNull { it.pid }.toIntArray()
                    PictureActivity.start(this, pid, arrayList)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
