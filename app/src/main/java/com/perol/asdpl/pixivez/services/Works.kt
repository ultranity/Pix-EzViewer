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

import android.media.MediaScannerConnection
import android.webkit.MimeTypeMap
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.RetrofitRepository
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.data.model.ImageUrlsX
import com.perol.asdpl.pixivez.data.model.UgoiraMetadataBean
import com.perol.asdpl.pixivez.networks.ImageHttpDns
import com.perol.asdpl.pixivez.networks.ServiceFactory.gson
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.ToastQ
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.pathString

fun String.toLegal(): String {
    return this.replace(Regex("[\\x00-\\x1f]"), "").replace("\t", "    ")
        .replace("/", "／").replace("\\", "＼").replace(":", "꞉")
        .replace("*", "∗").replace("?", "？").replace("|", "ǀ")
        .replace("\"", "\'\'").replace("<", "＜").replace(">", "＞")
}

@Serializable
class IllustD(
    val id: Int = 0,
    val part: Int = -1,
//    val userId: Int = 0,
//    val userName: String? = null,
    val restricted: Boolean = false,
    val title: String? = null
) {
    fun pString() = if (part == -1) "" else "p$part"
}

fun byteLimit(tags: List<String>, title: String, TagSeparator: String, blimit: Int): String {
    var result = tags.mapNotNull {
        if (!title.contains(it)
            and
            !tags.minusElement(it).any { ot -> ot.contains(it) }
        ) {
            it
        } else null
    }
        .joinToString(TagSeparator)
        .toLegal()
    var size = result.toByteArray().size
    while (size > blimit) {
        result = result.dropLast((size - blimit + 2) / 3)
        size = result.toByteArray().size
    }

    return result
}

object Works {

    private val pre by lazy { PxEZApp.instance.pre }
    private val ketch by lazy { PxEZApp.instance.ketch }

    fun parseSaveFormat(
        illust: Illust,
        part: Int? = null,
        saveformat: String = PxEZApp.saveformat,
        TagSeparator: String = PxEZApp.TagSeparator
    ): String {
        val url: String
        var filename = saveformat.replace("{illustid}", illust.id.toString())
            .replace("{userid}", illust.user.id.toString())
            .replace(
                "{name}",
                illust.user.name.let { if (it.length > 8) it.substringBeforeLast("@") else it }
                    .toLegal()
            )
            .replace("{account}", illust.user.account.toLegal())
            .replace("{R18}", if (illust.isR18) "R18" else "")
            .replace("{title}", illust.title.toLegal())
        // !illust.title.contains(it.name)
        if (part != null && part < illust.meta.size) {
            url = getQualityUrl(illust, part)
            filename = filename.replace(
                "{part}",
                if (illust.meta.size > 10) part.toString().padStart(2, '0')
                else part.toString()
            )
        } else { //if (illust.meta.size == 1) {
            url = getQualityUrl(illust)
            filename = filename.replace("_p{part}", "")
                .replace("_{part}", "")
                .replace("{part}", "")
        }//throw Error("part $part while illust.meta.size ${illust.meta.size}")
        val type = when {
            url.contains("png") -> ".png"
            url.contains("jpeg") -> ".jpeg"
            else -> ".jpg"
        }
        filename = filename.replace("{type}", type)
        if (saveformat.contains("{tagsm")) {
            filename = filename.replace(
                "{tagsm}",
                byteLimit(
                    illust.tags.map { it.translated_name + "_" + it.name }
                    .distinct().sortedBy { it.length },
                    illust.title,
                    TagSeparator,
                    253 - filename.toByteArray().size
                )
            )
        } else if (saveformat.contains("{tagso")) {
            filename = filename.replace(
                "{tagso}",
                byteLimit(
                    illust.tags.map { it.name }.distinct().sortedBy { it.length },
                    illust.title,
                    TagSeparator,
                    253 - filename.toByteArray().size
                )
            )
        } else if (saveformat.contains("{tags")) {
            val tags = illust.tags.map {
                it.translated_name?.let { ot ->
                    if (ot.length < it.name.length * 2.5) ot else it.name
                } ?: it.name
            }.distinct().sortedBy { it.length }
            filename = filename.replace(
                "{tags}",
                byteLimit(tags, illust.title, TagSeparator, 253 - filename.toByteArray().size)
            )
        }
        return filename
    }

    fun imageDownloadWithFile(illust: Illust, file: File, part: Int?) {
        val name = illust.user.name.toLegal()
        val userid = illust.user.id
        val filename = parseSaveFormat(illust, part)
        val targetFile = File(
            Path(
                PxEZApp.storepath,
                if (PxEZApp.RestrictFolder && (illust.restricted || illust.isR18)) PxEZApp.RestrictFolderPath else "",
                if (pre.getBoolean("needcreatefold", false)) "${name}_$userid" else "",
                filename
            ).pathString
        )
        try {
            val compatCheck = FileUtil.move(file, targetFile)
            if (PxEZApp.ShowDownloadToast) {
                ToastQ.post(R.string.savesuccess)
            }
            if (!(illust.restricted || illust.isR18))
                MediaScannerConnection.scanFile(
                    PxEZApp.instance,
                    arrayOf(targetFile.path),
                    arrayOf(
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(targetFile.extension)
                    )
                ) { _, _ ->
                    FileUtil.ListLog.add(illust.id)
                    compatCheck()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun downloadAll(data: List<Illust>, retryDownloaded: Boolean = true) {
        for (item in data) {
            if (retryDownloaded || !FileUtil.isDownloaded(item)) {
                imageDownloadAll(item)
            }
        }
    }

    fun imageDownloadAll(illust: Illust) {
        if (PxEZApp.ShowDownloadToast) {
            ToastQ.post(R.string.join_download_queue)
        }
        CoroutineScope(Dispatchers.IO).launch {
            if (illust.meta.size == 1) {
                imgD(illust, null) //hide _0 if is single image
            } else {
                for (i in illust.meta.indices) {
                    imgD(illust, i)
                }
            }
        }
    }

    fun ugoiraDownloadAll(ugoira: Illust, ugoiraMetadata: UgoiraMetadataBean) {
        val path = Path(
            PxEZApp.storepath,
            if (PxEZApp.RestrictFolder && (ugoira.restricted || ugoira.isR18)) PxEZApp.RestrictFolderPath else "",
            if (pre.getBoolean("needcreatefold", false))
                "${ugoira.user.name.toLegal()}_${ugoira.user.id}" else "",
            parseSaveFormat(ugoira).substringBeforeLast(".")
        ).pathString
        ugoira.meta.map { mirrorLinkDownload(qualityUrl(it)) }.forEach {
            ketch.download(
                it,
                path,
                it.substringAfterLast('/'),
                tag = ugoira.id.toString(),
                metaData = gson.encodeToString(ugoira)
            )
        }
    }

    var mirrorForView = pre.getBoolean("mirrorLinkView", false)
    var mirrorForDownload = pre.getBoolean("mirrorLinkDownload", false)
    const val opximg = "i.pximg.net"
    var mirrorURL = pre.getString("mirrorURL", opximg)!!
    var mirrorFormat = pre.getString("mirrorFormat", "{host}/{params}")!!
    var forceIP = pre.getBoolean("forceIP", true)
    val spximg
        get() = lookup(opximg)
    var smirrorURL = lookup(mirrorURL)
    fun lookup(url: String): String {
        return if (pre.getBoolean("dnsProxy", false) and forceIP) {
            ImageHttpDns.lookup(url)[0].hostAddress!!
        } else {
            url
        }
    }

    fun mirrorLinkView(url: String) = mirror(url, mirrorForView)
    fun mirrorLinkDownload(url: String) = mirror(url, mirrorForDownload)
    private fun mirror(url: String, mirror: Boolean = true): String {
        if (!mirror) {
            return url.replace(opximg, spximg)
        }
        if (mirrorFormat == "{host}/{params}") {
            return url.replace(opximg, smirrorURL)
        }

        var params = url.substringAfterLast(opximg)
        val pname = params.substringAfterLast('/')
        params = params.trimStart('/').substringBeforeLast('/')
        val illustid = pname.substringBeforeLast("_p").toIntOrNull()
        val part = pname.substringAfterLast("_p").substringBeforeLast(".").toIntOrNull()
        val type = "." + pname.substringAfterLast(".")
        return "https://" + mirrorFormat.replace("{host}", smirrorURL)
            .replace("{params}", params)
            .replace("{illustid}", illustid.toString())
            .replace("{part}", part.toString())
            .replace("{type}", type)
    }

    fun imgD(pid: Int, part: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            RetrofitRepository.getInstance().api.getIllust(pid).let {
                imgD(it.illust, part)
            }
        }
    }

    fun imgD(illust: Illust, part: Int?) {
        var url = (
                if (part != null && illust.meta.size > 1) {
                    getQualityUrl(illust, part)
                } else {
                    getQualityUrl(illust)
                }
                )
        // url = mirror(illust, part, url)
        url = mirrorLinkDownload(url)
        val name = illust.user.name.toLegal()
        val title = illust.title.toLegal()
        val filename = parseSaveFormat(illust, part)
        val path = Path(
            PxEZApp.storepath,
            if (PxEZApp.RestrictFolder && (illust.restricted || illust.isR18)) PxEZApp.RestrictFolderPath else "",
            if (pre.getBoolean("needcreatefold", false)) "${name}_${illust.user.id}" else ""
        ).pathString
        val targetFile = File(path, filename)
        if (targetFile.exists()) {
            ToastQ.post(R.string.alreadysaved)
            return
        }
        val illustD = IllustD(
            id = illust.id,
            part = part ?: -1,
//            userName = name,
//            userId = illust.user.id,
            restricted = illust.restricted || illust.isR18,
            title = title
        )
        ketch.download(
            url,
            path,
            filename,
            tag = illust.id.toString(),
            metaData = gson.encodeToString(illustD)
        )
    }

    var qualityDownload: Int = pre.getString("qualityDownload", "2")!!.toInt()
    private fun getQualityUrl(illust: Illust, part: Int = 0): String {
        //TODO if need: val part = part.coerceAtMost(illust.meta_pages.size - 1)
        val urls = illust.meta[part]
        return qualityUrl(urls)
    }

    private fun qualityUrl(urls: ImageUrlsX): String = when (qualityDownload) {
        0 -> urls.medium
        1 -> urls.large
        else -> urls.original // 2
    }
}
