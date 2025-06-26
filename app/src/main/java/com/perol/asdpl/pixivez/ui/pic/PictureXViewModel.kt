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

package com.perol.asdpl.pixivez.ui.pic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.webkit.MimeTypeMap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.base.KotlinUtil.launchCatching
import com.perol.asdpl.pixivez.data.model.BookmarkDetailBean
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.data.model.UgoiraMetadataBean
import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.objects.ToastQ
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.waynejo.androidndkgif.GifEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class PictureXViewModel : BaseViewModel() {
    val illustDetail = MutableLiveData<Illust?>()
    val ugoiraDetail = MutableLiveData<UgoiraMetadataBean>()
    val related = MutableLiveData<MutableList<Illust>?>()
    val relatedAdded = MutableLiveData<MutableList<Illust>?>()
    val nextRelated = MutableLiveData<String?>()
    val likeIllust = MutableLiveData<Boolean>()
    val followUser = MutableLiveData<Boolean>()
    var tags = MutableLiveData<BookmarkDetailBean>()
    val progress = MutableLiveData<Int>()
    val downloadUgoiraZipSuccess = MutableLiveData<Boolean>()
    var isEncoding = false
    private val illust: Illust
        get() = illustDetail.value!!
    val duration: Int
        get() = ugoiraDetail.value!!.frames[0].delay

    fun downloadUgoira() {
        if (!ugoiraDetail.isInitialized) //wait until ugoiraDetail is initialized
            runBlocking { ugoiraDetail.asFlow().take(1).collect {} }
        val size = ugoiraDetail.value!!.frames.size
        val ugoira = illust.extendUgoira(size)
        Works.ugoiraDownloadAll(ugoira, ugoiraDetail.value!!)
        //zipUgoira()
    }

    fun zipUgoira() {
        val fileCachedZIP = File("${fileCachedUgoira.path}_original.zip")
        val fileZIP = File("${filePath}_original.zip")
        if (fileZIP.exists()) {
            ToastQ.post(R.string.alreadysaved)
            return
        }
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ZipFile(fileCachedZIP).addFolder(fileCachedUgoira)
            } catch (e: Exception) {
                e.printStackTrace()
                ToastQ.post("zip Failed")
                fileCachedZIP.delete()
            }
            FileUtil.move(fileCachedZIP, fileZIP)
        }
    }

    fun convertGIFUgoira() {
        //TODO: convert original Ugoira images to GIF
        ToastQ.post("convertGIFUgoira not implemented yet...")
    }

    val filePath by lazy {
        Works.getDownloadPath(illust, Works.parseSaveFormat(illust).substringBeforeLast("."))
    }
    val fileZIP by lazy { File("$filePath.zip") }
    val fileGIF by lazy { File("$filePath.gif") }
    val fileCachedUgoira by lazy {
        File(PxEZApp.instance.cacheDir.toString() + File.separatorChar + illust.id)
    }
    val fileCachedZIP by lazy {
        File(PxEZApp.instance.cacheDir.toString() + File.separatorChar + illust.id + ".zip")
    }
    val fileCachedZIPTemp by lazy {
        File(PxEZApp.instance.cacheDir.toString() + File.separatorChar + illust.id + ".zip.tmp")
    }
    val fileCachedGIF by lazy {
        File(PxEZApp.instance.cacheDir.toString() + File.separatorChar + illust.id + ".gif")
    }

    fun saveZIP() {
        if (File("$filePath.zip").exists()) {
            ToastQ.post("${PxEZApp.instance.getString(R.string.alreadysaved)}:${fileZIP.path}")
            return
        }
        if ((downloadUgoiraZipSuccess.value == true) && fileCachedZIP.exists()) {
            FileUtil.move(fileCachedZIP, fileZIP)
            Toasty.info(PxEZApp.instance, R.string.save_zip_success)
        } else {
            Toasty.error(PxEZApp.instance, R.string.not_downloaded)
        }
    }

    fun saveGIF() {
        if (fileGIF.exists()) {
            ToastQ.post("${PxEZApp.instance.getString(R.string.alreadysaved)}:${fileGIF.path}")
            return
        }
        // TODO: Works.imageDownloadWithFile(illust, resourceFile!!, position)
        PxEZApp.instance.applicationScope.launchCatching(
            { encodeGIFfromZIP() }, {
                CoroutineScope(Dispatchers.IO).launch {
                    FileUtil.move(fileCachedGIF, fileGIF)
                    if (!(illust.restricted || illust.isR18))
                        MediaScannerConnection.scanFile(
                            PxEZApp.instance,
                            arrayOf(fileGIF.path),
                            arrayOf(
                                MimeTypeMap.getSingleton()
                                    .getMimeTypeFromExtension(
                                        fileGIF.extension
                                    )
                            ),
                            null
                        )
                    isEncoding = false
                    withContext(Dispatchers.Main) {
                        Toasty.success(PxEZApp.instance, R.string.savegifsuccess)
                    }
                }
            }, {
                isEncoding = false
                Toasty.error(PxEZApp.instance, R.string.savegifsuccesserr)
                Toasty.error(PxEZApp.instance, it.message ?: "unknown error")
            })
    }

    suspend fun encodeGIF(listFiles: ArrayList<File>?) {
        if (listFiles.isNullOrEmpty()) {
            throw RuntimeException("unzipped files not found")
        }
        if (listFiles.size < ugoiraDetail.value!!.frames.size) {
            throw RuntimeException("something wrong in ugoira files")
        }
        // TODO: 合成进度条
        // Toasty.info(PxEZApp.instance, "约有${listFiles.size}张图片正在合成").showInMain()
        withContext(Dispatchers.IO) {
            listFiles.sortWith { o1, o2 -> o1.name.compareTo(o2.name) }
            val gifEncoder =
                GifEncoder().apply { setThreadCount(Runtime.getRuntime().availableProcessors()) }
            //gifEncoder.encode(PxEZApp.instance, pathCachedGIF.path, listFiles.map { it.path })
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var bitmap = BitmapFactory.decodeFile(listFiles[0].absolutePath, options)
            gifEncoder.init(
                options.outWidth,
                options.outHeight,
                fileCachedGIF.path,
                GifEncoder.EncodingType.ENCODING_TYPE_STABLE_HIGH_MEMORY
            )
            for (i in listFiles.indices) {
                bitmap = BitmapFactory.decodeFile(
                    listFiles[i].absolutePath,
                    BitmapFactory.Options().apply {
                        inBitmap = bitmap
                        inMutable = true
                    })
                if (bitmap == null) {
                    throw RuntimeException("bitmap decode failed")
                }
                gifEncoder.encodeFrame(bitmap, duration)
                CrashHandler.instance.d("gif progress", "$i/${listFiles.size}")
            }
            gifEncoder.close()
            CrashHandler.instance.d(
                "gif done",
                "${listFiles.size}, ${FileUtil.getSize(fileCachedGIF.length().toFloat())}"
            )
        }
    }

    // Load images from a zip file
    fun loadImagesFromZip(zipFile: File): Sequence<Bitmap> {
        val zipInputStream = ZipInputStream(zipFile.inputStream())
        var entry: ZipEntry?
        var bitmap: Bitmap? = null
        return sequence {
            while (zipInputStream.nextEntry.also { entry = it } != null) {
                if (!entry!!.isDirectory) {
                    // Decode the image from the zip stream and invoke the callback
                    val opt = BitmapFactory.Options().apply {
                        inMutable = true
                        inBitmap = bitmap
                    }
                    bitmap = BitmapFactory.decodeStream(zipInputStream, null, opt)
                    yield(bitmap!!)
                }
                zipInputStream.closeEntry()
            }
            zipInputStream.close()
        }
    }

    suspend fun encodeGIFfromZIP() = withContext(Dispatchers.Default) {
        val file = if (fileZIP.exists()) {
            fileZIP
        } else if (fileCachedZIP.exists()) {
            fileCachedZIP
        } else {
            throw RuntimeException("zip file not found")
        }
        val gifEncoder =
            GifEncoder().apply { setThreadCount(Runtime.getRuntime().availableProcessors()) }
        var inited = false
        var size = 0
        loadImagesFromZip(file).forEach { bitmap ->
            if (!inited) {
                gifEncoder.init(
                    bitmap.width,
                    bitmap.height,
                    fileCachedGIF.path,
                    GifEncoder.EncodingType.ENCODING_TYPE_STABLE_HIGH_MEMORY
                )
                inited = true
            }
            gifEncoder.encodeFrame(bitmap, duration)
            size++
        }
        gifEncoder.close()
        CrashHandler.instance.d(
            "gif done",
            "${size}, ${FileUtil.getSize(fileCachedGIF.length().toFloat())}"
        )
    }

    suspend fun loadUgoiraMetadata(id: Int) = withContext(Dispatchers.Main) {
        val resp = retrofit.api.getUgoiraMetadata(id)
        ugoiraDetail.value = resp.ugoira_metadata
        resp.ugoira_metadata
    }

    fun loadUgoiraZip(mediaURL: String) {
        //if (fileCachedUgoira.exists() && fileCachedUgoira.listFiles()!!.size == ugoiraDetail.value!!.frames.size)
        val file = if (fileZIP.exists()) fileZIP else fileCachedZIP
        if (file.exists() && ZipFile(file).isValidZipFile) {
            CoroutineScope(Dispatchers.Main).launch {
                downloadUgoiraZipSuccess.value = true
            }
            return
        }
        progress.value = 0
        CoroutineScope(Dispatchers.IO).launchCatching({
            CrashHandler.instance.d("GIF", mediaURL)
            retrofit.gif.getGIFFile(mediaURL)
        }, { response ->
            val inputStream = response.byteStream()
            fileCachedZIPTemp.delete()
            val output = fileCachedZIPTemp.outputStream()
            CrashHandler.instance.d("GIF", "----------")
            val totalLen = response.contentLength()
            var bytesCopied: Long = 0
            val buffer = ByteArray(8 * 1024)
            var bytes = inputStream.read(buffer)
            CrashHandler.instance.d("GIF", Thread.currentThread().toString())
            while (bytes >= 0) {
                output.write(buffer, 0, bytes)
                bytesCopied += bytes
                bytes = inputStream.read(buffer)
                launchUI {
                    progress.value = (100 * bytesCopied / totalLen).toInt()
                }
            }
            inputStream.close()
            output.close()
            CrashHandler.instance.d("GIF", "++++${progress.value}++++")
            FileUtil.move(fileCachedZIPTemp, fileCachedZIP)
            //ZipFile(fileCachedZIPTemp).extractAll(fileCachedUgoira.path)
            launchUI {
                downloadUgoiraZipSuccess.value = true
            }
            CrashHandler.instance.d("GIF", "wwwwwwwwwwwwwwwwwwwwww")
        }, {
            it.printStackTrace()
            CrashHandler.instance.d("GIF", "xxxxxxxxxxxxxxxxxxxxxx")
            throw it
        }, contextOnSuccess = Dispatchers.IO)
    }

    fun firstGet(illust: Illust) {
        illustDetail.value = illust
        likeIllust.value = illust.is_bookmarked
    }

    fun firstGet(pid: Int) {
        CoroutineScope(Dispatchers.IO).launchCatching({
            retrofit.api.getIllust(pid)
        }, {
            firstGet(it.illust)
        }, {
            Toasty.warning(PxEZApp.instance, "Failed pid:$pid ${it.message}")
            illustDetail.value = null
        })
    }

    fun getRelated() {
        subscribeNext(
            { retrofit.api.getIllustRelated(illust.id) },
            related,
            nextRelated
        )
    }

    fun fabClick() {
        if (!illust.is_bookmarked) {
            InteractionUtil.like(illust, null) //{ likeIllust.value = true }
        } else {
            InteractionUtil.unlike(illust) //{ likeIllust.value = false }
        }
    }

    fun onLoadTags() {
        if (illustDetail.value != null) {
            viewModelScope.launch {
                retrofit.api.getIllustBookmarkDetail(illust.id)
                    .let { tags.value = it.bookmark_detail }
            }
        }
    }

    fun onDialogClick(private: Boolean) {
        val needActionLike = !illust.is_bookmarked or private
        if (needActionLike) {
            //TODO: default tag to add?
            val tagList =
                tags.value?.tags?.mapNotNull { if (it.is_registered) it.name else null }
            if (private) illust.x_restrict = 1 //TODO: add setting for mark private => x_restrict=1
            InteractionUtil.like(illust, tagList, private) //{ likeIllust.value = true }
        } else {
            InteractionUtil.unlike(illust) //{ likeIllust.value = false }
        }
    }

    fun likeUser() {
        val user = illust.user
        if (!user.is_followed)
            InteractionUtil.follow(user) {
                followUser.value = true
            }
        else
            InteractionUtil.unfollow(user) {
                followUser.value = false
            }
    }

    fun onLoadMoreRelated() {
        if (nextRelated.value != null) {
            subscribeNext(
                { retrofit.getIllustNext(nextRelated.value!!) },
                relatedAdded,
                nextRelated
            )
        }
    }
}

//data class ProgressInfo(var now: Long, var all: Long)
