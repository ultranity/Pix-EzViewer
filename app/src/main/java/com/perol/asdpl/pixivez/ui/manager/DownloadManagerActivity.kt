package com.perol.asdpl.pixivez.ui.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.list.listItems
import com.ketch.DownloadModel
import com.ketch.Status
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivityDownloadManagerBinding
import com.perol.asdpl.pixivez.databinding.DialogDownloadConfigBinding
import com.perol.asdpl.pixivez.networks.ServiceFactory.gson
import com.perol.asdpl.pixivez.services.IllustD
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.io.File

class DownloadManagerActivity : RinkActivity() {

    private lateinit var binding: ActivityDownloadManagerBinding
    private lateinit var downloadTaskAdapter: DownloadTaskAdapter
    private val ketch by lazy { PxEZApp.instance.ketch }

    internal class KetchDiffCallback : DiffUtil.ItemCallback<DownloadModel>() {
        override fun areItemsTheSame(oldItem: DownloadModel, newItem: DownloadModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DownloadModel, newItem: DownloadModel): Boolean {
            return (oldItem == newItem)
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        downloadTaskAdapter = DownloadTaskAdapter()
        downloadTaskAdapter.setDiffCallback(KetchDiffCallback())
        binding.progressList.apply {
            adapter = downloadTaskAdapter
            layoutManager = LinearLayoutManager(context)
        }
        binding.downloadlistrefreshlayout.setOnRefreshListener {
            // refreshList() //TODO: fix list refresh
            lifecycleScope.launch {
                ketch.getDownloadModelByStatus(Status.FAILED).forEach {
                    ketch.resume(it.id)
                }
                binding.downloadlistrefreshlayout.isRefreshing = false
            }
        }
        refreshList()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ketch.observeDownloads().flowOn(Dispatchers.IO).collect {
                    downloadTaskAdapter.setDiffNewData(it.asReversed().toMutableList())
                    binding.progress.text = getString(
                        R.string.fractional,
                        it.count { it.status != Status.SUCCESS }, it.size
                    )
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_download_manager, menu)
        return true
    }

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
                        //TODO: set ketch download thread config
                    }
                }
            }

            R.id.action_resume -> {
                ketch.resumeAll()
            }

            R.id.action_cancel -> {
                MaterialDialogs(this).show {
                    setMessage(R.string.all_cancel)
                    confirmButton { _, _ ->
                        ketch.clearAllDb() //TODO: check if cache is cleared
                    }
                }
            }

            R.id.action_finished_cancel -> {
                PxEZApp.instance.applicationScope.launch {
                    ketch.getDownloadModelByStatus(Status.SUCCESS).forEach {
                        ketch.clearDb(it.id, false)
                    }
                }
            }

            R.id.action_stop -> {
                ketch.pauseAll()
            }

            R.id.action_restart -> {
                PxEZApp.ActivityCollector.recreate()
            }

            R.id.action_export -> {
                showExportDialog()
            }

            R.id.action_import -> {
                showImportDialog()
            }
        }
        return true
    }

    @SuppressLint("CheckResult")
    private fun showExportDialog() = MaterialDialog(this).show {
        folderChooser(
            initialDirectory = File(PxEZApp.storepath),
            allowFolderCreation = true,
            context = context
        ) { _, folder ->
            MaterialDialog(context).show {
                listItems(items = listOf("URL", "FILE", "INFO")) { _, index, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        File(folder.absolutePath + File.separatorChar + "download.log")
                            .writer().let { writer ->
                                ketch.getAllDownloads().forEach {
                                    when (index) {
                                        0 -> {
                                            writer.appendLine(it.fileName)
                                            writer.appendLine(it.url)
                                            writer.appendLine(it.metaData)
                                        }

                                        1 -> {
                                            writer.appendLine(it.url.substringAfterLast("/"))
                                        }

                                        2 -> {
                                            writer.appendLine(gson.encodeToString(it))
                                        }
                                    }
                                }
                                writer.flush()
                                writer.close()
                            }
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun showImportDialog() = MaterialDialog(this).show {
        fileChooser(
            initialDirectory = File(PxEZApp.storepath),
            filter = { it.isDirectory || it.extension == "log" },
            context = context
        ) { _, file ->
            MaterialDialog(context).show {
                listItems(items = listOf("URL", "FILE", "INFO")) { _, index, _ ->
                    if (index == 0) {
                        file.readLines().chunked(3).forEach {
                            val illust = gson.decodeFromString<IllustD>(it[2])
                            //  val filename = it[0]
                            //  val url = it[1]
                            //  val path = Works.getDownloadPath(illust)
                            //  ketch.download(
                            //      url,
                            //      path,
                            //      filename,
                            //      tag = illust.id.toString(),
                            //      metaData = it[2]
                            //  )
                            Works.imgD(illust.id, illust.part)
                        }
                    } else file.readLines().forEach {
                        when (index) {
                            1 -> {
                                val pid = (
                                        Regex("(?<=(pid)?_?)(\\d{7,9})").find(it)?.value ?: ""
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
                                    // Thread.sleep(300)
                                }
                            }

                            2 -> gson.decodeFromString<DownloadModel>(it).let { it ->
                                ketch.download(
                                    it.url,
                                    it.path,
                                    it.fileName,
                                    it.tag,
                                    it.metaData,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun refreshList() {
        lifecycleScope.launch(Dispatchers.IO) {
            downloadTaskAdapter.setList(ketch.getAllDownloads().asReversed())
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
