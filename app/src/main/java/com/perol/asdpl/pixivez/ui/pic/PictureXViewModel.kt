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
        val cachedZipPath = "${PxEZApp.instance.cacheDir.path}/${illust.id}_original.zip"
        val fileCachedZIP = File(cachedZipPath)
        val zipPath = "$pathUgoira.zip"
        if (File(zipPath).exists()) {
            ToastQ.post(R.string.alreadysaved)
            return
        }
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ZipFile(cachedZipPath).addFolder(File(pathUgoira))
            } catch (e: Exception) {
                e.printStackTrace()
                ToastQ.post("zip Failed")
                fileCachedZIP.delete()
            }
            fileCachedZIP.copyTo(File(zipPath), true)
        }
    }

    fun convertGIFUgoira() {
        //TODO: convert original Ugoira images to GIF
        ToastQ.post("convertGIFUgoira not implemented yet...")
    }

    val pathUgoira by lazy {
        fileGIF.path.substringBeforeLast(".")
    }
    val fileGIF by lazy {
        val filePath =
            PxEZApp.storepath + File.separatorChar +
                    (if (PxEZApp.R18Folder && illust.restricted) PxEZApp.R18FolderPath else "") +
                    Works.parseSaveFormat(illust).substringBeforeLast(".").removePrefix("？")
        val fileGIF = File("$filePath.gif")
        fileGIF.parentFile?.mkdirs()// try make dir
        fileGIF
    }
    val pathCachedGIF by lazy {
        File(PxEZApp.instance.cacheDir.toString() + File.separatorChar + illust.id + ".gif")
    }

    fun saveGIF() {
        if (fileGIF.exists()) {
            ToastQ.post(R.string.alreadysaved)
            return
        }
        // TODO: Works.imageDownloadWithFile(illust, resourceFile!!, position)
        PxEZApp.instance.applicationScope.launchCatching(
            { encodingGif() }, {
                CoroutineScope(Dispatchers.IO).launch {
                    pathCachedGIF.copyTo(fileGIF, true)
                    MediaScannerConnection.scanFile(
                        PxEZApp.instance,
                        arrayOf(fileGIF.path),
                        arrayOf(
                            MimeTypeMap.getSingleton()
                                .getMimeTypeFromExtension(
                                    fileGIF.extension
                                )
                        )
                    ) { _, _ -> }
                    isEncoding = false
                    pathCachedGIF.delete()
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

    suspend fun encodingGif() {
        if (pathCachedGIF.exists()) {
            return
        }
        val parentPath = PxEZApp.instance.cacheDir.path + File.separatorChar + illust.id
        val listFiles = File(parentPath).listFiles()
        if (listFiles.isNullOrEmpty()) {
            throw RuntimeException("unzipped files not found")
        }
        // if (listFiles.size < size) {
        //     throw RuntimeException("something wrong in ugoira files")
        // }
        // TODO: 合成进度条
        // Toasty.info(PxEZApp.instance, "约有${listFiles.size}张图片正在合成").showInMain()
        withContext(Dispatchers.Default) {
            listFiles.sortWith { o1, o2 -> o1.name.compareTo(o2.name) }
            val gifEncoder = GifEncoder()
            //gifEncoder.encode(PxEZApp.instance, pathCachedGIF.path, listFiles.map { it.path })
            val bitmap = BitmapFactory.decodeFile(listFiles[0].absolutePath)
            val option = BitmapFactory.Options().apply {
                inBitmap = bitmap
            }
            gifEncoder.init(
                bitmap.width,
                bitmap.height,
                pathCachedGIF.path,
                GifEncoder.EncodingType.ENCODING_TYPE_STABLE_HIGH_MEMORY
            )
            CrashHandler.instance.d("gif progress", "0/${listFiles.size}")
            for (i in 1..listFiles.size) {
                val bitmap = BitmapFactory.decodeFile(listFiles[i].absolutePath, option)
                gifEncoder.encodeFrame(bitmap, duration)
                CrashHandler.instance.d("gif progress", "$i/${listFiles.size}")
            }
        }
    }

    fun downloadZip(mediaURL: String) {
        val zipPath =
            "${PxEZApp.instance.cacheDir.path}/${illust.id}.zip"
        val file = File(zipPath)
        if (file.exists()) {
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    ZipFile(file).extractAll(
                        PxEZApp.instance.cacheDir.path + File.separatorChar + illust.id
                    )
                    withContext(Dispatchers.Main) {
                        downloadUgoiraZipSuccess.value = true
                    }
                } catch (e: Exception) {
                    ToastQ.post("Unzip Failed")
                    File(PxEZApp.instance.cacheDir.path + File.separatorChar + illust.id).deleteRecursively()
                    file.delete()
                    reDownLoadUgoiraZip(mediaURL)
                }
            }
        } else {
            reDownLoadUgoiraZip(mediaURL)
        }
    }

    suspend fun loadGif(id: Int) = withContext(Dispatchers.Main) {
        val resp = retrofit.api.getUgoiraMetadata(id)
        ugoiraDetail.value = resp.ugoira_metadata
        resp.ugoira_metadata
    }

    private fun reDownLoadUgoiraZip(mediaURL: String) {
        val zipPath = "${PxEZApp.instance.cacheDir}/${illust.id}.zip"
        val file = File(zipPath)
        progress.value = 0
        CoroutineScope(Dispatchers.IO).launchCatching({
            CrashHandler.instance.d("GIF", mediaURL)
            retrofit.gif.getGIFFile(mediaURL)
        }, { response ->
            val inputStream = response.byteStream()
            val output = file.outputStream()
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
            ZipFile(file).extractAll(
                PxEZApp.instance.cacheDir.path + File.separatorChar + illust.id
            )
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

    fun firstGet(illust_id: Int) {
        CoroutineScope(Dispatchers.IO).launchCatching({
            retrofit.api.getIllust(illust_id)
        }, {
            firstGet(it.illust)
        }, {
            Toasty.warning(PxEZApp.instance, "Failed pid:$illust_id ${it.message}")
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
            InteractionUtil.like(illust, null) {
                likeIllust.value = true
            }
        } else {
            InteractionUtil.unlike(illust) {
                likeIllust.value = false
            }
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
            InteractionUtil.like(illust, tagList, private) {
                likeIllust.value = true
            }
        } else {
            InteractionUtil.unlike(illust) {
                likeIllust.value = false
            }
        }
    }

    fun likeUser() {
        val user = illust.user
        val need_follow = !user.is_followed
        if (need_follow)
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
