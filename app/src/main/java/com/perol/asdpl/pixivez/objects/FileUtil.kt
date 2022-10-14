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

import android.os.Environment
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.PxEZApp
import org.roaringbitmap.RoaringBitmap
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class FileInfo(file: File) {
    var icon = "0"
    var name: String = file.name
    var size: String = ""
    var pixel: String = ""
    var time: String = ""
    var path: String = file.path
    var type: Int = 0
    var lastModify: Long = 0
    var bytesize: Long = 0
    var illust:Illust?=null
    var target:String?=null
    var checked:Boolean=false
    override fun toString(): String {
        return "FileInfo{" +
                "icon=" + icon +
                ", name='" + name + '\'' +
                ", size='" + size + '\'' +
                ", time='" + time + '\'' +
                ", path='" + path + '\'' +
                ", type=" + type +
                ", lastModify=" + lastModify +
                ", bytesize=" + bytesize +
                '}'
    }

    fun isPic():Boolean{
        return listOf("jpg", "jpeg", "png", "gif").contains(ext)
    }

    val ext:String
        get() {
            return if (name.contains(".")) {
                val dot = name.lastIndexOf(".")// 123.abc.txt
                name.substring(dot + 1)
            } else {
                ""
            }
        }
    val pid: Long?
        get() {
        return (Regex("(?<=(pid)?_?)(\\d{7,9})")
            .find(name)?.value?:"").toLongOrNull()
    }

    val part: String
    get(){
        val dot = name.lastIndexOf(".")
        return Regex("""(?<=_p?)([0-9]{1,2})(?=\.)""")
            .find(name, if (dot - 4 > 0) dot - 4 else 0)?.value?:""
    }
}

interface OnclickInterfaceFile {
    fun itemClick(position: Int)
}

object FileUtil{
    val sdCardPath: String
        get() = Environment.getExternalStorageDirectory().absolutePath

        const val T_DIR = 0
        const val T_FILE = 1

        const val SORT_NAME = 0
        const val SORT_DATE = 1
        const val SORT_SIZE = 2

    init {
        getFileList()
    }
    /**
     * 通过传入的路径,返回该路径下的所有的文件和文件夹列表
     *
     * @param path
     * @return
     */
    fun getListData(
        path: String,
        picOnly: Boolean = true,
        withFolder: Boolean = true,
        showParent: Boolean = true//,
        //includeSubFolder: Boolean = false
    ): MutableList<FileInfo> {
        val list = ArrayList<FileInfo>()//用来存储便利之后每一个文件中的信息
        val pfile = File(path)// 兴建实例化文件对象
        if (!pfile.exists()) {// 判断路径是否存在
        //如果没有这个文件目录。那么这里去创建
            pfile.mkdirs()
                return list
        }
        if (pfile.isFile){
            //TODO: pfile.readLines().forEach {  }
        }
        if(showParent && pfile.parentFile != null){
            val parent = FileInfo(pfile.parentFile!!)
            // 获取文件夹目录结构
            parent.icon = R.drawable.ic_action_folder.toString()//图标
            parent.bytesize = pfile.length()
            parent.size = parent.name
            parent.name = ".."
            parent.type = T_DIR
            parent.time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)
                .format(Date(pfile.lastModified()))
            list.add(parent)
        }
        val files: Array<File>? = pfile.listFiles()//文件对象数组 // 该文件对象下所属的所有文件和文件夹列表
        if (files != null && files.isNotEmpty()) {// 非空验证
            list.addAll(files.mapNotNull {
                val item = FileInfo(it)
                if (it.isHidden) {
                    return@mapNotNull null
                }
                if (it.isDirectory// 文件夹
                    && it.canRead()//是否可读
                ) {
                    if (!withFolder)
                        return@mapNotNull null
                    // 获取文件夹目录结构
                    item.icon = R.drawable.ic_action_folder.toString()//图标
                    item.bytesize = it.length()
                    item.size =
                        it.listFiles()?.size.toString()//getSize(item.bytesize.toFloat())//大小
                    item.type = T_DIR
                    //if (includeSubFolder)
                    //    list.addAll(getListData(it.path,includeSubFolder=true))
                }
                if (it.isFile) {// 文件
                    if (picOnly && !item.isPic())
                        return@mapNotNull null
                    item.icon = item.path// 根据扩展名获取图标
                    item.size = getSize(it.length().toFloat())
                    item.type = T_FILE
                }
                item.time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)
                    .format(Date(it.lastModified()))
                item
            })
    }
        return list
    }

    /**
     * 格式转换应用大小 单位"B,KB,MB,GB"
     */
     fun getSize(length: Float): String {

        val kb: Long = 1024
        val mb = 1024 * kb
        val gb = 1024 * mb
        return if (length < kb) {
            String.format("%dB", length.toInt())
        } else if (length < mb) {
            String.format("%.2fKB", length / kb)
        } else if (length < gb) {
            String.format("%.2fMB", length / mb)
        } else {
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

    fun getGroupList(path: String, recursive: Boolean = false): MutableList<FileInfo> {
        val list = getListData(path)
        val dirs = ArrayList<FileInfo>()
        val files = ArrayList<FileInfo>()
        for (i in list.indices) {
            val item = list[i]
            if (item.type == 0) {
                dirs.add(item)
            } else {
                files.add(item)
            }
        }
        if (recursive){
            val subDirs = ArrayList<FileInfo>()
            for (dir in dirs) {
                subDirs.addAll(getGroupList(dir.path))
            }
            dirs.addAll(subDirs)
        }
        dirs.addAll(files)
        return dirs
    }

    fun deleteFile(path: String) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }

    fun pasteFile(targetDir: String, file: File): Int {
        val newFile = File(targetDir, file.name)
        if (newFile.exists()) {
            // Toast.makeText(this,newPath+" already exists",Toast.LENGTH_SHORT).show();
            return 1
        } else {
            try {
                file.copyTo(newFile)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return 0// OK
    }

    fun pasteDir(targetDir: String, dir: File) {
        val newDir = File(targetDir, dir.name)
        dir.copyRecursively(newDir, false)
    }

    private fun extraPath(path: String): List<String>? {
        File(path).let { file ->
            if (file.exists())
                return@extraPath file.readLines()
        }
        return null
    }
    private fun bitSetFileLog(path: String): RoaringBitmap? {
        val rr = RoaringBitmap()
        val file = File(path)
        if (!file.exists())
            return null
        val stream = DataInputStream(FileInputStream(file))
        rr.deserialize(stream)
        // number of values stored?
        // val cardinality = rr.longCardinality
        return rr
    }
    private fun writeBitSetFileLog(rr:RoaringBitmap, path: String) {
        val file = File("$path.bak")
        val stream = DataOutputStream(FileOutputStream(file))
        rr.serialize(stream)
        stream.close()
        file.copyTo(File(path),true)
    }



    lateinit var fileList:List<Int>
    lateinit var extraPath:List<String>
    lateinit var ListLog:RoaringBitmap
    //lateinit var localLog:RoaringBitmap
    fun getFileList(){
        //localLog = RoaringBitmap()
        extraPath = extraPath(PxEZApp.storepath+File.separator+"path.txt")?:
                extraPath(Environment.getExternalStorageDirectory().absolutePath + File.separator + "PxEz"+File.separator+"path.txt")
                ?: listOf()
        fileList = getGroupList(PxEZApp.storepath,true).mapNotNull{ it.pid?.toInt() }.sorted()

        if (extraPath.isNotEmpty()){
            extraPath.forEach {
                getGroupList(it,true).forEach {it.pid?.toInt()?.let{ListLog.add(it)} }
                fileList.plus(getGroupList(it,true).mapNotNull{ it.pid})
            }
        }
        ListLog =bitSetFileLog(PxEZApp.storepath+File.separator+"roaringbit.data")?:
                bitSetFileLog(Environment.getExternalStorageDirectory().absolutePath + File.separator + "PxEz"+File.separator+"roaringbit.data")
                ?:RoaringBitmap()
        val before = ListLog.cardinality
        ListLog.addN(fileList.toIntArray(),0,fileList.size)
        if (ListLog.cardinality > before)
            writeBitSetFileLog(ListLog,PxEZApp.storepath+File.separator+"roaringbit.data")
        //fileList.forEach {ListLog.add(it)}
    }
    fun haveLog(): Boolean {
        return (((bitSetFileLog(PxEZApp.storepath + File.separator + "roaringbit.data")
            ?: bitSetFileLog(Environment.getExternalStorageDirectory().absolutePath + File.separator + "PxEz" + File.separator + "roaringbit.data"))
                != null))
    }
    fun haveExtraPath(): Boolean {
        return (((extraPath(PxEZApp.storepath + File.separator + "path.txt") ?: extraPath(
            Environment.getExternalStorageDirectory().absolutePath + File.separator + "PxEz" + File.separator + "path.txt"
        ))
                != null))
    }
    fun isDownloaded(illust: Illust): Boolean {
        return isDownloaded2(illust)|| isDownloaded3(illust)
    }
    fun isDownloaded(pid: Long): Boolean {
        return isDownloaded2(pid)|| isDownloaded3(pid)
    }
    fun logDownloaded(illust: Illust): Int {
        return if(isDownloaded2(illust)) 1 else 0 + if(isDownloaded3(illust)) 1 else 0
    }
    private fun isDownloaded2(illust: Illust): Boolean {
        return fileList.contains(illust.id.toInt())
    }
    private fun isDownloaded2(pid: Long): Boolean {
        return fileList.contains(pid.toInt())
    }

    private fun isDownloaded3(illust: Illust): Boolean {
        return ListLog.contains(illust.id.toInt())
    }
    private fun isDownloaded3(pid: Long): Boolean {
        return ListLog.contains(pid.toInt())
    }
}
