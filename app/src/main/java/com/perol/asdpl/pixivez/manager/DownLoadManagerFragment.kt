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

package com.perol.asdpl.pixivez.manager

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.list.listItems
import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadEntity
import com.arialyy.aria.core.task.DownloadTask
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.gson.Gson
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.databinding.FragmentDownloadManagerBinding
import com.perol.asdpl.pixivez.databinding.ItemDownloadTaskBinding
import com.perol.asdpl.pixivez.services.IllustD
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import org.jetbrains.annotations.NotNull
import java.io.File


class DownloadTaskAdapter :
    BaseQuickAdapter<DownloadEntity, BaseViewHolder>(R.layout.item_download_task) {
    init {
        this.setOnItemClickListener { adapter, view, position ->
            val item = data[position]
            val illust = Gson().fromJson(item.str, IllustD::class.java)
            PictureActivity.startSingle(context, id = illust.id)
        }
        this.setOnItemLongClickListener { adapter, view, position ->
            val item = data[position]
            MaterialDialog(context).show {
                title(text = item.fileName)
                listItems(R.array.download_task_choice) { _, index, string ->
                    when (index) {
                        0 -> {
                            Aria.download(PxEZApp.instance)
                                .load(item.url) //读取下载地址
                                .setFilePath(item.filePath) //设置文件保存的完整路径
                                .ignoreFilePathOccupy()
                                .setExtendField(item.str)
                                .option(Works.option)
                                .create()
                        }
                        1 -> {
                            Aria.download(context).load(data[position].id).stop()
                        }
                        2 -> {
                            Aria.download(context).load(data[position].id).resume()
                        }
                        3 -> {
                            Aria.download(context).load(data[position].id).cancel()
                        }
                    }
                    val taskList = Aria.download(this).taskList
                    if (taskList?.isNotEmpty() == true)
                        this@DownloadTaskAdapter.setNewData(taskList.asReversed())
                }
            }
            true
        }

    }


    override fun onItemViewHolderCreated(
        @NotNull viewHolder: BaseViewHolder,
        viewType: Int
    ) { // 绑定 view
        DataBindingUtil.bind<ItemDownloadTaskBinding>(viewHolder.itemView)
    }

    private fun Int.toIEntityString(): String {
        return when (this) {
            0 -> {
                "FAIL"
            }
            1 -> "COMPLETE"
            2 -> "STOP"
            3 -> "WAIT"
            4 -> "RUNNING"
            5 -> {
                "PRE"
            }
            6 -> {
                "POST_PRE"
            }
            7 -> {
                "CANCEL"
            }
            else -> {
                "OTHER"
            }
        }
    }

    override fun convert(helper: BaseViewHolder, item: DownloadEntity, payloads: List<Any>) {
        val binding = helper.getBinding<ItemDownloadTaskBinding>()!!
        if (payloads.isNotEmpty()) {
            val thatItem = payloads[0] as DownloadEntity
            val progress = helper.getView<ProgressBar>(R.id.progress)
            progress.max = thatItem.fileSize.toInt()
            progress.progress = thatItem.currentProgress.toInt()
            binding.progressFont.text =
                "${thatItem.currentProgress.toInt()}/${thatItem.fileSize.toInt()}"
        }
    }
    override fun convert(helper: BaseViewHolder, item: DownloadEntity) {
        val binding = helper.getBinding<ItemDownloadTaskBinding>()!!
        val progress = helper.getView<ProgressBar>(R.id.progress)
        progress.max = item.fileSize.toInt()
        progress.progress = item.currentProgress.toInt()
        binding.progressFont.text = "${item.currentProgress.toInt()}/${item.fileSize.toInt()}"
        try {
            val illustD = Gson().fromJson(item.str, IllustD::class.java)
            helper.setText(R.id.title, illustD.title)
            helper.setText(R.id.status, item.state.toIEntityString())
        } catch (e: Exception) {

        }
    }
}

class DownLoadManagerFragment : Fragment() {


    fun refreshSingle(task: DownloadTask?) {
        task?.let {
            var index = -1
            for (i in downloadTaskAdapter.data.indices) {
                if (downloadTaskAdapter.data[i].id == task.downloadEntity.id) {
                    index = i
                    break
                }
            }

            if (index != -1) {
                downloadTaskAdapter.data[index] = it.entity
                downloadTaskAdapter.notifyItemChanged(index, it.entity)

            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_download_manager, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(requireActivity(), ManagerSettingsActivity::class.java))
            }
            R.id.action_resume -> {
                Aria.download(this).resumeAllTask()
                Thread(Runnable {
                    // wait for resumeAllTask
                    Thread.sleep(2000)
                    // restart other failed task
                    Aria.download(this).allNotCompleteTask?.forEach {
                        if (it.state == 0) {
                            Aria.download(this).load(it.id).cancel(true)
                            Thread.sleep(500)
                            val illustD = Gson().fromJson(it.str, IllustD::class.java)
                            Aria.download(this).load(it.url)
                                .setFilePath(it.filePath) //设置文件保存的完整路径
                                .ignoreFilePathOccupy()
                                .setExtendField(it.str)
                                .option(Works.option)
                                .create()
                            Thread.sleep(300)
                        }
                    }
                    activity?.runOnUiThread {
                        val taskList = Aria.download(this).taskList
                        if (taskList?.isNotEmpty() == true)
                            downloadTaskAdapter.setNewData(taskList.asReversed())
                    }
                }).start()
            }
            R.id.action_cancel -> {
                Aria.download(this).removeAllTask(false)
            }
            R.id.action_finished_cancel -> {
                Thread(Runnable {
                    Aria.download(this).allCompleteTask?.forEach {
                        Aria.download(this).load(it.id).cancel(true)
                    }
                }).start()
            }
            R.id.action_stop -> {
                Aria.download(context).stopAllTask()
            }

            R.id.action_restart -> {
                PxEZApp.ActivityCollector.recreate()
            }
            R.id.action_export -> {
                MaterialDialog(requireContext()).show {
                    folderChooser(
                        allowFolderCreation = true
                    ) { _, folder ->
                        val writer  = File(folder.absolutePath+ File.separatorChar+"download.log")
                            .writer()
                        Aria.download(this).taskList.map {
                            writer.appendLine(it.url.substringAfterLast("/"))
                        }
                        writer.flush()
                        writer.close()
                    }
                }
            }
            R.id.action_import -> {
                MaterialDialog(requireContext()).show {
                    fileChooser(filter = { it.isDirectory || it.extension=="log"})
                    { _, file ->
                        file.readLines().forEach {
                            it.split("_p",".").let {
                                val pid = it[0].toLongOrNull()
                                val part = it[1].toIntOrNull()
                                if (pid!=null && part!=null)
                                    Works.imgD(pid,part)
                            }
                        }
                    }
                }
            }
        }
        val taskList = Aria.download(this).taskList
        if (taskList?.isNotEmpty() == true)
            downloadTaskAdapter.setNewData(taskList.asReversed())
        return true
    }

    @Download.onPre
    fun onPre(task: DownloadTask) {
        refreshSingle(task)
    }
    @Download.onTaskPre
    fun onTaskPre(task: DownloadTask) {
        refreshSingle(task)
    }

    @Download.onTaskStop
    fun onTaskStop(task: DownloadTask) {
        refreshSingle(task)
    }

    @Download.onTaskCancel
    fun onTaskCancel(task: DownloadTask) {
        task.let {
            var index = -1
            for (i in downloadTaskAdapter.data.indices) {
                if (downloadTaskAdapter.data[i].id == task.downloadEntity.id) {
                    index = i
                    break
                }
            }

            if (index != -1) {
                downloadTaskAdapter.remove(index)
            }
        }
    }

    @Download.onTaskRunning
    fun onTaskRunning(task: DownloadTask) {
        refreshSingle(task)
    }

    @Download.onTaskComplete
    fun onTaskComplete(task: DownloadTask) {
        refreshSingle(task)
    }

    @Download.onTaskResume
    fun onTaskResume(task: DownloadTask) {
        refreshSingle(task)
    }

    @Download.onTaskStart
    fun onTaskStart(task: DownloadTask) {
        refreshSingle(task)
    }

    @Download.onWait
    fun onWait(task: DownloadTask) {
        refreshSingle(task)
    }
    @Download.onTaskFail
    fun onTaskFail(task: DownloadTask?) {
        refreshSingle(task)
    }

    companion object {
        fun newInstance() = DownLoadManagerFragment()
    }

    override fun onDestroy() {
        Aria.download(this).unRegister()
        super.onDestroy()

    }

    private lateinit var viewModel: DownLoadManagerViewModel
    lateinit var binding: FragmentDownloadManagerBinding
    lateinit var downloadTaskAdapter: DownloadTaskAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Aria.download(this).register()
        downloadTaskAdapter = DownloadTaskAdapter()
        binding = FragmentDownloadManagerBinding.inflate(inflater)
        binding.progressList.apply {
            adapter = downloadTaskAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        binding.downloadlistrefreshlayout.setOnRefreshListener {
            val taskList = Aria.download(this).taskList
            if (taskList?.isNotEmpty() == true)
                downloadTaskAdapter.setNewData(taskList.asReversed())
            binding.downloadlistrefreshlayout.isRefreshing = false
        }
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DownLoadManagerViewModel::class.java)

    }

    override fun onResume() {
        super.onResume()
        val taskList = Aria.download(this).taskList
        if (taskList?.isNotEmpty() == true)
            downloadTaskAdapter.setNewData(taskList.asReversed())
    }

}
