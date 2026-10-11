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
import com.perol.asdpl.pixivez.objects.IllustCacheRepo
import com.perol.asdpl.pixivez.objects.Storage
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
    }

    private val saveStem by lazy { Works.parseSaveFormat(illust).substringBeforeLast(".") }
    private val zipTarget by lazy { Works.downloadTarget(illust, "$saveStem.zip") }
    private val gifTarget by lazy { Works.downloadTarget(illust, "$saveStem.gif") }
    val fileCachedZIP by lazy {
        File(PxEZApp.instance.cacheDir.toString() + File.separatorChar + illust.id + ".zip")
    }
    private val fileCachedZIPTemp by lazy {
        File(PxEZApp.instance.cacheDir.toString() + File.separatorChar + illust.id + ".zip.tmp")
    }
    private val fileCachedGIF by lazy {
        File(PxEZApp.instance.cacheDir.toString() + File.separatorChar + illust.id + ".gif")
    }

    /** Fills [fileCachedZIP] from a previously saved zip; playback and encoding read the cache. */
    private fun restoreCachedZip(): Boolean {
        if (fileCachedZIP.exists()) return true
        val saved = Storage.find(zipTarget) ?: return false
        Storage.openInput(saved.uri).use { input ->
            fileCachedZIPTemp.outputStream().use { input.copyTo(it) }
        }
        return fileCachedZIPTemp.renameTo(fileCachedZIP)
    }

    private fun alreadySaved(target: List<String>): Boolean {
        val saved = Storage.find(target) ?: return false
        ToastQ.post(
            "${PxEZApp.instance.getString(R.string.alreadysaved)}:${Storage.filePath(saved.uri) ?: saved.name}"
        )
        return true
    }

    fun saveZIP() {
        PxEZApp.instance.applicationScope.launch {
            if (alreadySaved(zipTarget)) return@launch
            if ((downloadUgoiraZipSuccess.value == true) && fileCachedZIP.exists()) {
                try {
                    Works.save(illust, zipTarget, fileCachedZIP)
                    ToastQ.post(R.string.save_zip_success)
                } catch (e: Exception) {
                    CrashHandler.instance.e("Ugoira", "save zip failed", e, toast = true)
                }
            } else {
                ToastQ.post(R.string.not_downloaded)
            }
        }
    }

    fun saveGIF() {
        PxEZApp.instance.applicationScope.launchCatching(
            {
                if (!alreadySaved(gifTarget)) {
                    isEncoding = true
                    encodeGIFfromZIP()
                    Works.save(illust, gifTarget, fileCachedGIF)
                    fileCachedGIF.delete()
                    ToastQ.post(R.string.savegifsuccess)
                }
                isEncoding = false
            }, {}, {
                isEncoding = false
                Toasty.error(PxEZApp.instance, R.string.savegifsuccesserr)
                Toasty.error(PxEZApp.instance, it.message ?: "unknown error")
            })
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
        if (!restoreCachedZip()) {
            throw RuntimeException("zip file not found")
        }
        val file = fileCachedZIP
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
        progress.value = 0
        CoroutineScope(Dispatchers.IO).launch {
            val success = try {
                if (!(restoreCachedZip() && ZipFile(fileCachedZIP).isValidZipFile)) {
                    downloadUgoiraZip(mediaURL)
                }
                true
            } catch (e: Exception) {
                CrashHandler.instance.e("GIF", "load ugoira zip failed", e, toast = true)
                false
            }
            launchUI {
                downloadUgoiraZipSuccess.value = success
            }
        }
    }

    private suspend fun downloadUgoiraZip(mediaURL: String) {
        CrashHandler.instance.d("GIF", mediaURL)
        val response = retrofit.gif.getGIFFile(mediaURL)
        val totalLen = response.contentLength()
        var bytesCopied: Long = 0
        fileCachedZIPTemp.delete()
        response.byteStream().use { input ->
            fileCachedZIPTemp.outputStream().use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytes = input.read(buffer)
                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    bytes = input.read(buffer)
                    launchUI {
                        progress.value = (100 * bytesCopied / totalLen).toInt()
                    }
                }
            }
        }
        fileCachedZIPTemp.renameTo(fileCachedZIP)
    }

    fun firstGet(illust: Illust) {
        val canonical = IllustCacheRepo.update(illust.id, illust)
        illustDetail.value = canonical
        likeIllust.value = canonical.is_bookmarked
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
