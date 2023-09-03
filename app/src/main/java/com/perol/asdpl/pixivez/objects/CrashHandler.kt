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
package com.perol.asdpl.pixivez.objects

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Looper
import android.os.Process
import android.util.Log
import android.view.Gravity
import android.view.InflateException
import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.services.PxEZApp
import java.io.File
import java.io.FilenameFilter
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Properties
import java.util.TimeZone
import java.util.TreeSet
import kotlin.system.exitProcess

/** @noinspection BlockingMethodInNonBlockingContext, BlockingMethodInNonBlockingContext, BlockingMethodInNonBlockingContext, BlockingMethodInNonBlockingContext
 */
class CrashHandler : Thread.UncaughtExceptionHandler {
    companion object {
        /**
         * Debug Log tag
         */
        const val TAG = "CrashHandler"

        /**
         * 是否开启日志输出,在Debug状态下开启,
         * 在Release状态下关闭以提示程序性能
         */
        const val DEBUG = true
        private const val STACK_TRACE = "STACK_TRACE"

        /**
         * 错误报告文件的扩展名
         */
        private const val CRASH_REPORTER_EXTENSION = ".cr"
        private val syncRoot = Any()

        /**
         * 保证只有一个CrashHandler实例
         */
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var _instance: CrashHandler? = null
        val instance: CrashHandler
            get() = _instance ?: synchronized(this) {
                _instance ?: CrashHandler().also { _instance = it }
            }
    }

    var logs: MutableList<LogItem> = mutableListOf()

    class LogItem(val tag: String, val msg: String, val tr: Throwable?) {
        override fun toString(): String {
            return "$tag: $msg\n${tr?.stackTraceToString()}"
        }
    }

    /**
     * 系统默认的UncaughtException处理类
     */
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null

    private val mDeviceCrashInfo = Properties()

    /**
     * 初始化,注册Context对象,
     * 获取系统默认的UncaughtException处理器,
     * 设置该CrashHandler为程序的默认处理器
     *
     * @param ctx Context
     */
    fun init() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    val LOG_ID_MAIN = 0
    val LOG_ID_RADIO = 1
    val LOG_ID_EVENTS = 2
    val LOG_ID_SYSTEM = 3
    val LOG_ID_CRASH = 4
    fun i(tag: String, msg: String, tr: Throwable? = null) {
        if (BuildConfig.DEBUG) logs.add(LogItem(tag, msg, tr))
        Log.i(tag, msg, tr)
    }

    fun d(tag: String, msg: String, tr: Throwable? = null) {
        if (BuildConfig.DEBUG) logs.add(LogItem(tag, msg, tr))
        Log.d(tag, msg, tr)
    }

    fun w(tag: String, msg: String, tr: Throwable? = null) {
        if (BuildConfig.DEBUG) logs.add(LogItem(tag, msg, tr))
        Log.w(tag, msg, tr)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null, toast: Boolean = false) {
        if (BuildConfig.DEBUG) logs.add(LogItem(tag, msg, tr))
        Log.e(tag, msg, tr)
        if (toast) Toasty.error(PxEZApp.instance, msg).show()
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (!handleException(ex) && mDefaultHandler != null) {
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler!!.uncaughtException(thread, ex)
        } else {
            //Sleep一会后结束程序
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Error : ", e)
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    /**
     * 自定义错误处理,收集错误信息
     * 发送错误报告等操作均在此完成.
     * 开发者可以根据自己的情况来自定义异常处理逻辑
     *
     * @param ex Throwable
     * @return true:如果处理了该异常信息;否则返回false
     */
    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) {
            Log.w(TAG, "handleException --- ex==null")
            return true
        }
        Log.e("Crash", ex.message, ex)
        ex.localizedMessage ?: return false
        //使用Toast来显示异常信息
        Thread {
            Looper.prepare()
            if (ex is Resources.NotFoundException || ex is InflateException
                || ex.message != null && ex.message!!.contains("XML")) {
                val toast = Toasty.error(
                    PxEZApp.instance,
                    """Missing Resource: ${ex.message}""".trimIndent()
                )
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            } else if (DEBUG) {
                val toast = Toasty.error(PxEZApp.instance, """程序出错，即将退出:${ex.message}""")
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
            Looper.loop()
        }.start()
        //收集设备信息
        //collectCrashDeviceInfo(PxEZApp.instance)
        //保存错误报告文件
        saveCrashInfoToFile(ex)
        //发送错误报告到服务器
        //sendCrashReportsToServer(PxEZApp.instance);
        return true
    }

    /**
     * 在程序启动时候, 可以调用该函数来发送以前没有发送的报告
     */
    fun sendPreviousReportsToServer() {
        sendCrashReportsToServer(PxEZApp.instance)
    }

    /**
     * 把错误报告发送给服务器,包含新产生的和以前没发送的.
     *
     * @param ctx Context
     * @noinspection ResultOfMethodCallIgnored
     */
    private fun sendCrashReportsToServer(ctx: Context?) {
        val crFiles = getCrashReportFiles(ctx)
        if (crFiles.isNotEmpty()) {
            val sortedFiles = TreeSet(listOf(*crFiles))
            for (fileName in sortedFiles) {
                val cr = File(ctx!!.filesDir, fileName)
                postReport(cr)
                cr.delete() // 删除已发送的报告
            }
        }
    }

    private fun postReport(file: File) {}

    /**
     * 获取错误报告文件名
     *
     * @param ctx Context
     * @return filesDir.list(name.endsWith ( CRASH_REPORTER_EXTENSION))
     */
    private fun getCrashReportFiles(ctx: Context?): Array<String> {
        val filesDir = ctx!!.filesDir
        val filter = FilenameFilter { dir: File?, name: String ->
            name.endsWith(
                CRASH_REPORTER_EXTENSION
            )
        }
        return filesDir.list(filter)!!
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex Throwable
     */
    private fun saveCrashInfoToFile(ex: Throwable) {
        val info: Writer = StringWriter()
        val printWriter = PrintWriter(info)
        ex.printStackTrace(printWriter)
        var cause = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        val result = info.toString()
        printWriter.close()
        mDeviceCrashInfo["EXEPTION"] = ex.localizedMessage
        mDeviceCrashInfo[STACK_TRACE] = result
        try {
            //long timestamp = System.currentTimeMillis();
            val t = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).time
            val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.ROOT).format(t)

            val fileName = "crash-${time}$CRASH_REPORTER_EXTENSION"
            val trace = PxEZApp.instance.openFileOutput(
                fileName,
                Context.MODE_PRIVATE
            )
            mDeviceCrashInfo.store(trace, "")
            trace.flush()
            trace.close()
        } catch (e: Exception) {
            Log.e(TAG, "an error occured while writing report file...", e)
        }
    }

    /**
     * 收集程序崩溃的设备信息
     *
     * @param ctx Context
     */
    fun collectCrashDeviceInfo(ctx: Context?): String {
        return EasyFormatter.DEFAULT.format(Build())
//        try {
//            PackageManager pm = ctx.getPackageManager();
//            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
//                    PackageManager.GET_ACTIVITIES);
//            if (pi != null) {
//                mDeviceCrashInfo.put(VERSION_NAME,
//                        pi.versionName == null ? "not set" : pi.versionName);
//                mDeviceCrashInfo.put(VERSION_CODE, ""+pi.versionCode);
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//            Log.e(TAG, "Error while collect package info", e);
//        }
        //使用反射来收集设备信息.在Build类中包含各种设备信息,
        //例如: 系统版本号,设备生产商 等帮助调试程序的有用信息
    }
}