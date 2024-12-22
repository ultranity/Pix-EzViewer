package com.perol.asdpl.pixivez.ui.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.list.listItems
import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadEntity
import com.arialyy.aria.core.download.DownloadReceiver
import com.arialyy.aria.core.task.DownloadTask
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivityDownloadManagerBinding
import com.perol.asdpl.pixivez.databinding.DialogDownloadConfigBinding
import com.perol.asdpl.pixivez.networks.ServiceFactory.gson
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.services.Works.option
import kotlinx.serialization.encodeToString
import java.io.File

class DownloadManagerActivity : RinkActivity() {

    private lateinit var binding: ActivityDownloadManagerBinding
    private lateinit var downloadTaskAdapter: DownloadTaskAdapter
    private lateinit var aria: DownloadReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aria = Aria.download(this)
        aria.register()
        binding = ActivityDownloadManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.progress.text = getString(
            R.string.fractional,
            aria.allNotCompleteTask?.size ?: 0, aria.taskList?.size ?: 0
        )
        downloadTaskAdapter = DownloadTaskAdapter()
        binding.progressList.apply {
            adapter = downloadTaskAdapter
            layoutManager = LinearLayoutManager(context)
        }
        binding.downloadlistrefreshlayout.setOnRefreshListener {
            val taskList = aria.taskList
            if (taskList?.isNotEmpty() == true) {
                downloadTaskAdapter.setNewInstance(taskList.asReversed())
            }
            binding.downloadlistrefreshlayout.isRefreshing = false
        }
    }

    private fun refreshSingle(task: DownloadTask?) {
        task?.let {
            val index = downloadTaskAdapter.data.indexOfFirst { ot ->
                ot.id == it.downloadEntity.id
            }

            if (index != -1) {
                downloadTaskAdapter.data[index] = it.entity
                downloadTaskAdapter.notifyItemChanged(index, it.entity)
            }
        }
        binding.progress.text = getString(
            R.string.fractional,
            aria.allNotCompleteTask?.size ?: 0, aria.taskList?.size ?: 0
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_download_manager, menu)
        return true
    }

    @SuppressLint("CheckResult")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val configDialog = DialogDownloadConfigBinding.inflate(layoutInflater)
                configDialog.spinnerMaxTaskNum.setSelection(
                    PxEZApp.instance.pre.getString(
                        "max_task_num",
                        "2"
                    )!!.toInt() - 1
                )
                configDialog.spinnerThreadNum.setSelection(
                    PxEZApp.instance.pre.getString(
                        "thread_num",
                        "2"
                    )!!.toInt() - 1
                )
                MaterialDialogs(this).show {
                    setTitle(R.string.task_setting)
                    setView(configDialog.root)
                    cancelButton()
                    confirmButton { _, _ ->
                        Aria.get(this@DownloadManagerActivity).downloadConfig.apply {
                            maxTaskNum =
                                (configDialog.spinnerMaxTaskNum.selectedItem as String).toInt()
                            threadNum =
                                (configDialog.spinnerThreadNum.selectedItem as String).toInt()
                            PxEZApp.instance.pre.edit {
                                putString(
                                    "max_task_num",
                                    configDialog.spinnerMaxTaskNum.selectedItem as String
                                )
                                putString(
                                    "thread_num",
                                    configDialog.spinnerThreadNum.selectedItem as String
                                )
                            }
                        }
                    }
                }
            }

            R.id.action_resume -> {
                Thread {
                    // wait for resumeAllTask
                    Thread.sleep(2000)
                    // restart other failed task
                    aria.allNotCompleteTask?.asReversed()?.forEach {
                        if (it.state == 0) {
                            aria.load(it.id).cancel(false)
                            Thread.sleep(500)
                            // val illustD = gson.decodeFromString<IllustD>(item.str)
                            aria.load(it.url)
                                .setFilePath(it.filePath) // 设置文件保存的完整路径
                                .ignoreFilePathOccupy()
                                .setExtendField(it.str)
                                .option(option)
                                .create()
                            Thread.sleep(300)
                        }
                    }
                    runOnUiThread {
                        val taskList = aria.taskList
                        if (taskList?.isNotEmpty() == true) {
                            downloadTaskAdapter.setNewInstance(taskList.asReversed())
                        }
                    }
                }.start()
                aria.resumeAllTask()
            }

            R.id.action_cancel -> {
                MaterialDialogs(this).show {
                    setMessage(R.string.all_cancel)
                    confirmButton { _, _ ->
                        aria.removeAllTask(false)
                    }
                }
            }

            R.id.action_finished_cancel -> {
                Thread {
                    aria.allCompleteTask?.forEach {
                        aria.load(it.id).cancel(true)
                    }
                }.start()
            }

            R.id.action_stop -> {
                aria.stopAllTask()
            }

            R.id.action_restart -> {
                PxEZApp.ActivityCollector.recreate()
            }

            R.id.action_export -> {
                MaterialDialog(this).show {
                    folderChooser(
                        initialDirectory = File(PxEZApp.storepath),
                        allowFolderCreation = true,
                        context = context
                    ) { _, folder ->
                        MaterialDialog(context).show {
                            listItems(items = listOf("URL", "FILE", "INFO")) { _, index, _ ->
                                val writer =
                                    File(folder.absolutePath + File.separatorChar + "download.log")
                                        .writer()
                                when (index) {
                                    0 -> aria.taskList.forEach {
                                        writer.appendLine(it.fileName)
                                        writer.appendLine(it.url)
                                        writer.appendLine(it.str)
                                    }

                                    1 -> aria.taskList.forEach {
                                        writer.appendLine(it.url.substringAfterLast("/"))
                                    }

                                    2 -> aria.taskList.forEach {
                                        writer.appendLine(gson.encodeToString(it))
                                    }

                                }
                                writer.flush()
                                writer.close()
                            }
                        }
                    }
                }
            }

            R.id.action_import -> {
                MaterialDialog(this).show {
                    fileChooser(
                        initialDirectory = File(PxEZApp.storepath),
                        filter = { it.isDirectory || it.extension == "log" },
                        context = context
                    ) { _, file ->
                        MaterialDialog(context).show {
                            listItems(items = listOf("URL", "FILE", "INFO")) { _, index, _ ->
                                Thread {
                                    when (index) {
                                        0 -> file.readLines().chunked(3).forEach {
                                            val targetPath =
                                                "${PxEZApp.instance.cacheDir}${File.separator}${it[0]}"
                                            aria.load(it[1]) // 读取下载地址
                                                .setFilePath(targetPath) // 设置文件保存的完整路径
                                                .ignoreFilePathOccupy()
                                                .setExtendField(it[2])
                                                .option(option)
                                                .create()
                                        }

                                        1 -> file.readLines().forEach {
                                            val pid = (
                                                    Regex("(?<=(pid)?_?)(\\d{7,9})")
                                                        .find(it)?.value ?: ""
                                                    ).toIntOrNull()
                                            val dot = it.lastIndexOf(".")
                                            val part = (
                                                    Regex("""(?<=_p?)([0-9]{1,2})(?=\.)""")
                                                        .find(
                                                            it, //TODO: if necessary?
                                                            if (dot - 4 > 0) dot - 4 else 0
                                                        )?.value ?: "0"
                                                    ).toIntOrNull()
                                            if (pid != null && part != null) {
                                                Works.imgD(pid, part)
                                                Thread.sleep(300)
                                            }
                                        }

                                        2 -> file.readLines().forEach {
                                            val task = gson.decodeFromString<DownloadEntity>(it)
                                            aria.load(task.url) // 读取下载地址
                                                .setFilePath(task.filePath) // 设置文件保存的完整路径
                                                .ignoreFilePathOccupy()
                                                .setExtendField(task.str)
                                                .option(option)
                                                .create()
                                        }

                                    }
                                }.start()
                            }
                        }
                    }
                }
            }
        }
        val taskList = aria.taskList
        if (taskList?.isNotEmpty() == true) {
            downloadTaskAdapter.setNewInstance(taskList.asReversed())
        }
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
            val index = downloadTaskAdapter.data.indexOfFirst { it.id == task.downloadEntity.id }
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
        aria.unRegister()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val taskList = aria.taskList
        if (taskList?.isNotEmpty() == true) {
            downloadTaskAdapter.setNewInstance(taskList.asReversed())
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(
                Intent(
                    context,
                    DownloadManagerActivity::class.java
                ).setAction("DownMgr.start")
            )
        }
    }
}
