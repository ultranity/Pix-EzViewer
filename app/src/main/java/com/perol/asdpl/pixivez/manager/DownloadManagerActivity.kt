package com.perol.asdpl.pixivez.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.files.folderChooser
import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadReceiver
import com.arialyy.aria.core.task.DownloadTask
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivityDownloadManagerBinding
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import java.io.File

class DownloadManagerActivity : RinkActivity() {



    private lateinit var binding: ActivityDownloadManagerBinding
    private lateinit var viewModel: DownLoadManagerViewModel
    private lateinit var downloadTaskAdapter: DownloadTaskAdapter
    private lateinit var  aria: DownloadReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        viewModel = ViewModelProvider(this)[DownLoadManagerViewModel::class.java]
        viewModel.progress.observe(this) {
            binding.progress.text = it
        }
        aria = Aria.download(this)
        aria.register()
        downloadTaskAdapter = DownloadTaskAdapter()
        binding.progressList.apply {
            adapter = downloadTaskAdapter
            layoutManager = LinearLayoutManager(context)
        }
        binding.downloadlistrefreshlayout.setOnRefreshListener {
            val taskList = aria.taskList
            if (taskList?.isNotEmpty() == true)
                downloadTaskAdapter.setNewInstance(taskList.asReversed())
            binding.downloadlistrefreshlayout.isRefreshing = false
        }
    }
    private fun refreshSingle(task: DownloadTask?) {
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
        viewModel.progress.value = "${aria.allNotCompleteTask?.size?:0}/${aria.taskList?.size?:0}"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_download_manager, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, ManagerSettingsActivity::class.java))
            }
            R.id.action_resume -> {
                Thread {
                    // wait for resumeAllTask
                    Thread.sleep(2000)
                    // restart other failed task
                    Aria.download(this).allNotCompleteTask?.forEach {
                        if (it.state == 0) {
                            Aria.download(this).load(it.id).cancel(true)
                            Thread.sleep(500)
                            //val illustD = Gson().fromJson(it.str, IllustD::class.java)
                            Aria.download(this).load(it.url)
                                .setFilePath(it.filePath) //设置文件保存的完整路径
                                .ignoreFilePathOccupy()
                                .setExtendField(it.str)
                                .option(Works.option)
                                .create()
                            Thread.sleep(300)
                        }
                    }
                    runOnUiThread {
                        val taskList = Aria.download(this).taskList
                        if (taskList?.isNotEmpty() == true)
                            downloadTaskAdapter.setNewInstance(taskList.asReversed())
                    }
                }.start()
                Aria.download(this).resumeAllTask()
            }
            R.id.action_cancel -> {
                MaterialAlertDialogBuilder(this).apply{
                    setMessage(R.string.all_cancel)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            Aria.download(this).removeAllTask(false)
                        }
                }.show()
            }
            R.id.action_finished_cancel -> {
                Thread {
                    Aria.download(this).allCompleteTask?.forEach {
                        Aria.download(this).load(it.id).cancel(true)
                    }
                }.start()
            }
            R.id.action_stop -> {
                Aria.download(this).stopAllTask()
            }

            R.id.action_restart -> {
                PxEZApp.ActivityCollector.recreate()
            }
            R.id.action_export -> {
                MaterialDialog(this).show {
                    folderChooser(
                        allowFolderCreation = true
                        , context = context) { _, folder ->
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
                MaterialDialog(this).show {
                    fileChooser(filter = { it.isDirectory || it.extension=="log"}, context = context)
                    { _, file ->
                        Thread {
                            file.readLines().forEach {
                                val pid = (Regex("(?<=(pid)?_?)(\\d{7,9})")
                                    .find(it)?.value ?: "").toLongOrNull()
                                val dot = it.lastIndexOf(".")
                                val part = (Regex("""(?<=_p?)([0-9]{1,2})(?=\.)""")
                                    .find(it, if (dot - 4 > 0) dot - 4 else 0)?.value
                                    ?: "").toIntOrNull()
                                if (pid != null && part != null && !FileUtil.isDownloaded(pid)){
                                    Works.imgD(pid, part)
                                    Thread.sleep(300)
                                }
                                /*it.split("_p",".").let {
                                val pid = it[0].toLongOrNull()
                                val part = it[1].toIntOrNull()
                                if (pid!=null && part!=null)
                                    Works.imgD(pid,part)
                            }*/
                            }
                        }.start()
                    }
                }
            }
        }
        val taskList = Aria.download(this).taskList
        if (taskList?.isNotEmpty() == true)
            downloadTaskAdapter.setNewInstance(taskList.asReversed())
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
                downloadTaskAdapter.removeAt(index)
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

    override fun onDestroy() {
        Aria.download(this).unRegister()
        super.onDestroy()

    }

    override fun onResume() {
        super.onResume()
        val taskList = aria.taskList
        if (taskList?.isNotEmpty() == true){
            downloadTaskAdapter.setNewInstance(taskList.asReversed())
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DownloadManagerActivity::class.java))
        }
    }
}