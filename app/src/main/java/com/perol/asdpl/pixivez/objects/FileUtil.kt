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
package com.perol.asdpl.pixivez.objects

import android.annotation.SuppressLint
import android.net.Uri
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.model.Illust
import org.roaringbitmap.RoaringBitmap
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)
class FileInfo(entry: Entry) {
    var uri: Uri = entry.uri
    var name: String = entry.name
    val isDirectory: Boolean = entry.isDirectory
    val size: String = if (entry.isDirectory) "" else FileUtil.getSize(entry.size.toFloat())
    val time: String = dateFormat.format(Date(entry.lastModified))
    var pixel: String = ""
    /** drawable res for folders, image uri for files */
    val icon: Any get() = if (isDirectory) R.drawable.ic_action_folder else uri
    var illust: Illust? = null
    var target: String? = null
    var checked: Boolean = false
    override fun toString(): String {
        return "FileInfo{" +
            "name='" + name + '\'' +
            ", size='" + size + '\'' +
            ", time='" + time + '\'' +
            ", uri='" + uri + '\'' +
            ", isDirectory=" + isDirectory +
            '}'
    }

    fun isPic(): Boolean {
        return listOf("jpg", "jpeg", "png", "gif").contains(ext)
    }

    val ext: String
        get() {
            return if (name.contains(".")) {
                val dot = name.lastIndexOf(".") // 123.abc.txt
                name.substring(dot + 1)
            } else {
                ""
            }
        }
    val pid: Int?
        get() {
            return (
                    Regex("(?<=(pid)?_?)(\\d{7,9})")
                        .find(name)?.value ?: ""
                    ).toIntOrNull()
        }

    val part: String
        get() {
            val dot = name.lastIndexOf(".")
            return Regex("""(?<=_p?)([0-9]{1,2})(?=\.)""")
                .find(name, if (dot - 4 > 0) dot - 4 else 0)?.value ?: ""
        }
}

object FileUtil {
    const val SORT_NAME = 0
    const val SORT_DATE = 1
    const val SORT_SIZE = 2

    /** Visible folders, then pictures, of [dir]. */
    fun listDir(dir: Uri): List<FileInfo> {
        val (dirs, files) = Storage.list(dir)
            .filterNot { it.name.startsWith(".") }
            .map(::FileInfo)
            .filter { it.isDirectory || it.isPic() }
            .partition { it.isDirectory }
        return dirs + files
    }

    /**
     * 格式转换应用大小 单位"B,KB,MB,GB"
     */
    @SuppressLint("DefaultLocale")
    fun getSize(length: Float): String {
        val kb: Long = 1024
        val mb = 1024 * kb
        val gb = 1024 * mb
        return if (length < kb) {
            String.format("%dB", length.toInt())
        }
        else if (length < mb) {
            String.format("%.2fKB", length / kb)
        }
        else if (length < gb) {
            String.format("%.2fMB", length / mb)
        }
        else {
            String.format("%.2fGB", length / gb)
        }
    }

    fun searchFile(list: List<FileInfo>, keyword: String): List<FileInfo> {
        val searchResultList = ArrayList<FileInfo>()
        for (i in list.indices) {
            val app = list[i]
            if (app.name.lowercase(Locale.ROOT).contains(keyword.lowercase(Locale.ROOT))) {
                searchResultList.add(app)
            }
        }
        return searchResultList
    }

    private fun collectPids(dir: Uri, into: RoaringBitmap) {
        for (entry in Storage.list(dir)) {
            if (entry.name.startsWith(".")) continue
            if (entry.isDirectory) {
                collectPids(entry.uri, into)
            } else {
                FileInfo(entry).takeIf { it.isPic() }?.pid?.let { into.add(it) }
            }
        }
    }

    /** `path.txt` in the storage root: extra folders (file paths) to index, one per line. */
    private fun extraDirs(): List<Uri> {
        val entry = Storage.find(listOf(EXTRA_PATH_FILE)) ?: return emptyList()
        return Storage.openInput(entry.uri).bufferedReader().use { it.readLines() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { Uri.fromFile(File(it)) }
    }

    private fun readDownloadLog(): RoaringBitmap? {
        val entry = Storage.find(listOf(DOWNLOAD_LOG_FILE)) ?: return null
        return RoaringBitmap().apply {
            DataInputStream(Storage.openInput(entry.uri)).use { deserialize(it) }
        }
    }

    private fun writeDownloadLog(rr: RoaringBitmap) {
        Storage.write(listOf(DOWNLOAD_LOG_FILE)) { rr.serialize(DataOutputStream(it)) }
    }

    private const val DOWNLOAD_LOG_FILE = "roaringbit.data"
    private const val EXTRA_PATH_FILE = "path.txt"
    var ListLog = RoaringBitmap()

    /** Rebuilds [ListLog] from the persisted log plus a scan of the storage root. */
    fun getFileList() {
        try {
            val log = readDownloadLog() ?: RoaringBitmap()
            val before = log.cardinality
            log.or(ListLog)
            (listOf(Storage.root) + extraDirs()).forEach { collectPids(it, log) }
            ListLog = log
            if (log.cardinality > before) {
                writeDownloadLog(log)
            }
        } catch (e: Exception) {
            CrashHandler.instance.e("FileUtil", "index downloads failed", e)
        }
    }

    fun isDownloaded(illust: Illust): Boolean {
        return ListLog.contains(illust.id)
    }

    fun isDownloaded(pid: Int): Boolean {
        return ListLog.contains(pid)
    }
}
