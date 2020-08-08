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
package com.perol.asdpl.pixivez.manager

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.activity.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivityImgManagerBinding
import com.perol.asdpl.pixivez.databinding.CustomformatviewBinding
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.services.GlideApp
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.android.synthetic.main.activity_img_manager.*
import java.io.File


class ImgManagerActivity : RinkActivity() {
    private lateinit var activityImgMgrMBinding: ActivityImgManagerBinding
    private lateinit var viewModel: ImgManagerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityImgMgrMBinding = DataBindingUtil.setContentView(this, R.layout.activity_img_manager)
        initView()
        initBind()
    }

    private val ImgManagerAdapter = ImgManagerAdapter(R.layout.view_imgmanager_item)

    private fun initView() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        recyclerview_img_manager.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        //recyclerview_img_manager.layoutManager = GridLayoutManager(this, 2, RecyclerView.VERTICAL, false)
        recyclerview_img_manager.adapter = ImgManagerAdapter
        ImgManagerAdapter.addFooterView(LayoutInflater.from(this).inflate(R.layout.foot_list, null))
        //recyclerview_img_manager.smoothScrollToPosition(ImgManagerAdapter.data.size)

        viewModel = ViewModelProvider(this).get(ImgManagerViewModel::class.java)
        viewModel.adapter = ImgManagerAdapter
        viewModel.layoutManager = recyclerview_img_manager.layoutManager as LinearLayoutManager
        viewModel.path.value = viewModel.pre.getString("ImgManagerPath", PxEZApp.storepath)!!
        viewModel.path.observe(this, Observer {
            swiperefresh.isRefreshing = true
            Thread(Runnable {
                viewModel.files = FileUtil.getGroupList(
                    it
                )
                //.filter{it.isPic()}.toMutableList()
                viewModel.task = viewModel.files!!.map { renameTask(it) }
                runOnUiThread {
                    img_count.text = viewModel.files!!.size.toString()
                    swiperefresh.isRefreshing = false
                    ImgManagerAdapter.setNewData(viewModel.files)
                }
            }).start()
        })
        swith_filter.isChecked = true
        swith_filter.setOnCheckedChangeListener { compoundButton, state ->
            //viewModel.length_filter= state
            reset()
        }
        swith_once.setOnCheckedChangeListener { compoundButton, state ->
            //viewModel.rename_once = state
            reset()
        }
    }

    private fun reset() {
        getInfo = false
        GlideApp.with(this).load(R.drawable.ic_action_search).thumbnail(0.5f).into(fab_start)
        viewModel.task?.forEach {
            it.file.checked = false
        }
        ImgManagerAdapter.notifyDataSetChanged()
    }

    private var getInfo = false
    private fun initBind() {
        fab_start.setOnClickListener {
            viewModel.length_filter = swith_filter.isChecked
            viewModel.rename_once = swith_once.isChecked
            if (viewModel.rename_once)
                viewModel.getInfo()
            else {
                if (getInfo) {
                    GlideApp.with(this).load(R.drawable.ic_action_search).into(fab_start)
                    viewModel.renameAll()
                } else {
                    GlideApp.with(this).load(R.drawable.ic_action_play).into(fab_start)
                    viewModel.getInfo()
                }
                getInfo = !getInfo
                //ImgManagerAdapter.notifyDataSetChanged()
            }
        }
        fab_folder.setOnClickListener {
            MaterialDialog(this).show {
                title(R.string.title_save_path)
                folderChooser(
                    initialDirectory = File(viewModel.path.value!!),
                    allowFolderCreation = true
                ) { _, folder ->
                    folder.absolutePath.let {
                        viewModel.pre.edit().putString("ImgManagerPath", it).apply()
                        viewModel.path.value = it
                    }
                }
                cornerRadius(2.0F)
                negativeButton(android.R.string.cancel)
                positiveButton(R.string.action_select)
                lifecycleOwner(this@ImgManagerActivity)
            }
        }

        fab_settings.setOnClickListener {
            // Setup custom view content
            val binding = CustomformatviewBinding.inflate(layoutInflater)
            val descTable = binding.formatDescTable
            val sampleTable = binding.formatSampleTable
            val Input = binding.customizedformat
            val tagSeparator = binding.tagSeparator
            Input.setText(viewModel.saveformat)
            tagSeparator.setText(viewModel.TagSeparator)
            val dialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT))
            dialog.show {
                title(R.string.saveformat)
                customView(view = binding.root, scrollable = true, horizontalPadding = true)
                positiveButton(R.string.save) { dialog ->
                    viewModel.saveformat = "${Input.text}"
                    viewModel.TagSeparator = "${tagSeparator.text}"
                    viewModel.pre.edit().putString("ImgManagerSaveFormat", viewModel.saveformat)
                        .apply()
                    viewModel.pre.edit().putString("ImgManagerTagSeparator", viewModel.TagSeparator)
                        .apply()
                    //if(getInfo){
                    getInfo = false
                    //}
                    reset()
                }
                negativeButton(android.R.string.cancel)
                lifecycleOwner(this@ImgManagerActivity)
            }
            val InputEditable = Input.editableText
            for (i in 1..descTable.childCount - 1)
                descTable.getChildAt(i).setOnClickListener {
                    InputEditable.insert(Input.selectionStart, it.tag.toString())
                }
            for (i in 1 until sampleTable.childCount)
                sampleTable.getChildAt(i).setOnClickListener {
                    InputEditable.clear()
                    InputEditable.insert(0, it.tag.toString())
                }
        }

        ImgManagerAdapter.setOnItemClickListener { _, _, position ->
            val file = viewModel.files!![position]
            if (file.type == FileUtil.T_DIR) {
                viewModel.path.value = file.path
            } else {
                val pid = viewModel.files!![position].pid.toLongOrNull()
                if (pid != null) {
                    val bundle = Bundle()
                    val arrayList = LongArray(1)
                    arrayList[0] = (pid!!)
                    bundle.putLongArray("illustidlist", arrayList)
                    bundle.putLong("illustid", arrayList[0])
                    val intent2 = Intent(applicationContext, PictureActivity::class.java)
                    intent2.putExtras(bundle)
                    startActivity(intent2)
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