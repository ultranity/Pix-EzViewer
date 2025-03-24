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

package com.perol.asdpl.pixivez.services

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.ketch.Ketch
import com.ketch.Status
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.networks.RestClient.downloadHttpClient
import com.perol.asdpl.pixivez.networks.ServiceFactory.gson
import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.FastKVLogger
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.objects.Toasty
import io.fastkv.FastKVConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class PxEZApp : Application() {
    lateinit var pre: SharedPreferences
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("PxEZApp", "Exception in DownloadManager Scope: ${throwable.message}")
    }

    val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    lateinit var ketch: Ketch
    override fun onCreate() {
        super.onCreate()
        instance = this
        // LeakCanary.install(this);
        pre = PreferenceManager.getDefaultSharedPreferences(this)
        applicationScope.launch {
            AppDataRepo.getUser()
        }
        val workManager = WorkManager.getInstance(this) //todo: config download concurrency
        ketch = Ketch.builder().setOkHttpClient(downloadHttpClient).build(this)

        applicationScope.launch {
            ketch.getAllDownloads().forEach {
                when (it.status) {
                    //remove finished task entry
                    Status.SUCCESS, Status.CANCELLED -> {
                        ketch.clearDb(it.id, false)
                    }
                    //and resume unfinished
                    Status.FAILED, Status.PAUSED -> {
                        if (pre.getBoolean("resume_unfinished_task", true)
                        ) {
                            ketch.resume(it.id)
                        }
                    }

                    else -> {
                        Log.d("PxEZApp", "Task ${it.id} is in ${it.status} status")
                    }
                }
            }
            val postprocessedSet = mutableSetOf<Int>()
            ketch.observeDownloadsByStatus(Status.SUCCESS)
                .flowOn(Dispatchers.IO)
                .collect {
                    it.filterNot { postprocessedSet.contains(it.id) }.forEach {
                        val illustD = gson.decodeFromString<IllustD>(it.metaData)
                        val file = File(it.path, it.fileName)
                        if (file.exists()) {
                            if (!illustD.restricted)
                                MediaScannerConnection.scanFile(
                                    this@PxEZApp,
                                    arrayOf(file.path),
                                    arrayOf(
                                        MimeTypeMap.getSingleton()
                                            .getMimeTypeFromExtension(file.extension)
                                    ), null
                                )
                            postprocessedSet.add(it.id)
                            FileUtil.ListLog.add(illustD.id)
                            if (ShowDownloadToast) {
                                withContext(Dispatchers.Main) {
                                    Toasty.success(
                                        this@PxEZApp,
                                        "${illustD.title} ${illustD.pString()}${getString(R.string.savesuccess)}"
                                    )
                                }
                            }
                        }
                    }
                }
        }
        FastKVConfig.setLogger(FastKVLogger())
        FastKVConfig.setExecutor(Dispatchers.Default.asExecutor())
        AppCompatDelegate.setDefaultNightMode(
            pre.getString("dark_mode", "-1")!!.toInt()
        )
        animationEnable = pre.getBoolean("animation", false)
        ShowDownloadToast = pre.getBoolean("ShowDownloadToast", true)
        CollectMode = pre.getString("CollectMode", "0")?.toInt() ?: 0
        R18Private = pre.getBoolean("R18Private", true)
        RestrictFolder = pre.getBoolean("R18Folder", false)
        RestrictFolderPath = pre.getString("R18FolderPath", "xRestrict/")!!
        restrictSanity = pre.getInt("restrictSanity", 8)
        TagSeparator = pre.getString("TagSeparator", "#")!!
        storepath = pre.getString(
            "storepath1",
            Environment.getExternalStorageDirectory().absolutePath + File.separator + "PxEz"
        )!!
        saveformat = pre.getString("filesaveformat", "{illustid}({userid})_{title}_{part}{type}")!!
        if (pre.getBoolean("crashreport", true)) {
            CrashHandler.instance.init()
        }
        locale = LanguageUtil.getLocale() // System locale
        language = pre.getString("language", "-1")?.toIntOrNull()
            ?: LanguageUtil.localeToLang(locale) // try to detect language from system locale if not configured

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                ActivityCollector.collect(activity)
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            //override fun onActivityPostDestroyed(activity: Activity) {
            //    super.onActivityPostDestroyed(activity)
            //}

            override fun onActivityDestroyed(activity: Activity) {
                ActivityCollector.discard(activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                //
            }
        })
    }

    object ActivityCollector {
        @JvmStatic
        private val activityList = mutableListOf<Activity>()

        @JvmStatic
        fun collect(activity: Activity) {
            activityList.add(activity)
        }

        @JvmStatic
        fun discard(activity: Activity) {
            activityList.remove(activity)
        }

        @JvmStatic
        fun recreate() {
            for (i in activityList.size - 1 downTo 0) {
                activityList[i].recreate()
            }
        }
    }

    companion object {
        @JvmStatic
        var storepath = ""

        @JvmStatic
        var RestrictFolder: Boolean = false

        @JvmStatic
        var RestrictFolderPath = "xrestrict"

        @JvmStatic
        var restrictSanity: Int = 8

        @JvmStatic
        var saveformat = ""

        @JvmStatic
        var locale: Locale = Locale.SIMPLIFIED_CHINESE

        @JvmStatic
        var language: Int = 0

        @JvmStatic
        var animationEnable: Boolean = false

        @JvmStatic
        var R18Private: Boolean = true

        @JvmStatic
        var ShowDownloadToast: Boolean = true

        @JvmStatic
        var CollectMode: Int = 0

        @JvmStatic
        var TagSeparator: String = "#"

        lateinit var instance: PxEZApp
    }
}
