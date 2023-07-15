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

import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.webkit.MimeTypeMap
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.common.HttpOption
import com.google.gson.Gson
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.networks.ImageHttpDns
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.Illust
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun String.toLegal(): String {
    return this.replace(Regex("[\\x00-\\x1f]"), "").replace("\t", "    ")
        .replace("/", "／").replace("\\", "＼").replace(":", "꞉")
        .replace("*", "∗").replace("?", "？").replace("|", "ǀ")
        .replace("\"", "\'\'").replace("<", "＜").replace(">", "＞")
}

data class IllustD(
    var id: Long = 0,
    var part: Int = 0,
    var preview: String? = null,
    var userId: Long = 0,
    var userName: String? = null,
    var userAvatar: String? = null,
    var title: String? = null,
    var url: String
)
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

    val pre: SharedPreferences = PxEZApp.instance.pre
    fun parseSaveFormat(illust: Illust, part: Int? = null): String {
        return parseSaveFormat(illust, part, PxEZApp.saveformat, PxEZApp.TagSeparator, PxEZApp.R18Folder)
    }

    fun parseSaveFormat(illust: Illust, part: Int?, saveformat: String, TagSeparator: String, R18Folder: Boolean): String {
        val url: String
        val tag = if (saveformat.contains("{tag")) {
            illust.tags
        } else null
        var filename = saveformat.replace("{illustid}", illust.id.toString())
            .replace("{userid}", illust.user.id.toString())
            .replace("{name}", illust.user.name.let { if (it.length > 8) it.substringBeforeLast("@") else it }.toLegal())
            .replace("{account}", illust.user.account.toLegal())
            .replace("{R18}", if (illust.x_restrict == 1) "R18" else "")
            .replace("{title}", illust.title.toLegal())
        // !illust.title.contains(it.name)
        if (part != null && illust.meta_pages.isNotEmpty()) {
            url = illust.meta_pages[
                if (part < illust.meta_pages.size - 1) {
                    part
                }
                else {
                    illust.meta_pages.size - 1
                }
            ].image_urls.original
            filename = filename.replace("{part}", part.toString())
        }
        else if (illust.meta_single_page.original_image_url != null) {
            url = illust.meta_single_page.original_image_url
            filename = filename.replace("_p{part}", "")
                .replace("_{part}", "")
                .replace("{part}", "")
        }
        else {
            url = ""
        }
        if (R18Folder && illust.x_restrict == 1) {
            filename = "？$filename"
        }

        val type = when {
            url.contains("png") -> {
                ".png"
            }
            url.contains("jpeg") -> {
                ".jpeg"
            }
            else -> ".jpg"
        }
        filename = filename.replace("{type}", type)
        if (saveformat.contains("{tagsm")) {
            filename = filename.replace(
                "{tagsm}",
                tag!!.map { it.translated_name + "_" + it.name }
                    .distinct().sortedBy { it.length }
                    .joinToString(TagSeparator, limit = 8).take(110 - filename.length).toLegal()
            )
        }
        else if (saveformat.contains("{tagso")) {
            filename = filename.replace(
                "{tagso}",
                tag!!.map{ it.name }
                    .distinct().sortedBy { it.length }
                    .joinToString(TagSeparator).take(110 - filename.length).toLegal()
            )
        }
        else if (saveformat.contains("{tags")) {
            val tags = tag!!.map {
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
        val needCreateFold = pre.getBoolean("needcreatefold", false)
        val targetFile = File(
            "${PxEZApp.storepath}/" +
                (if (PxEZApp.R18Folder && filename.startsWith("？")) PxEZApp.R18FolderPath else "") +
                if (needCreateFold) "${name}_$userid" else "",
            filename.removePrefix("？")
        )
        try {
            file.copyTo(targetFile, overwrite = true)
            file.delete()
            if (PxEZApp.ShowDownloadToast) {
                Toasty.shortToast(R.string.savesuccess)
            }
            MediaScannerConnection.scanFile(
                PxEZApp.instance,
                arrayOf(targetFile.path),
                arrayOf(
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(targetFile.extension)
                )
            ) { _, _ ->
                FileUtil.ListLog.add(illust.id.toInt())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun downloadAll(data: List<Illust>, hideDownloaded: Boolean = true) {
        for (item in data) {
            if (hideDownloaded && FileUtil.isDownloaded(item)) {
                imageDownloadAll(item)
            }
        }
    }
    fun imageDownloadAll(illust: Illust) {
        if (PxEZApp.ShowDownloadToast) {
            Toasty.shortToast(R.string.join_download_queue)
        }
        CoroutineScope(Dispatchers.IO).launch {
            if (illust.meta_pages.isEmpty()) {
                imgD(illust, null)
            }
            else {
                for (i in illust.meta_pages.indices) {
                    imgD(illust, i)
                }
            }
        }
    }

    val option by lazy {
        HttpOption()
            .apply {
                addHeader(
                    "User-Agent",
                    "PixivAndroidApp/5.0.234 (Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MODEL})"
                ).addHeader("referer", "https://app-api.pixiv.net/")
            }
    }
    var mirrorLinkView = pre.getBoolean("mirrorLinkView", false)
    var mirrorLinkDownload = pre.getBoolean("mirrorLinkDownload", false)
    const val opximg = "i.pximg.net"
    var mirrorURL = pre.getString("mirrorURL", opximg)!!
    var mirrorFormat = pre.getString("mirrorFormat", "{host}/{params}")!!
    var spximg = lookup(opximg)
    var smirrorURL = lookup(mirrorURL)
    fun lookup(url: String): String {
        return if (pre.getBoolean("disableproxy", false)) {
            url
        }
        else {
            ImageHttpDns.lookup(url)[0].hostAddress!!
        }
    }
    fun mirrorLinkView(url: String) = mirror(url, mirrorLinkView)
    private fun mirrorLinkDownload(url: String) = mirror(url, mirrorLinkDownload)
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
    fun mirror(illust: Illust, part: Int?, url: String): String {
        var params = url.substringAfterLast(opximg).trimStart('/')
        val pname = params.substringAfterLast('/')
        params = params.substringBeforeLast('/')
        // val illustid = pname.substringBeforeLast("_p").toIntOrNull()
        // val part = pname.substringAfterLast("_p").substringBeforeLast(".").toIntOrNull()
        val type = "." + pname.substringAfterLast(".")
        return "https://" + mirrorFormat.replace("{host}", spximg)
            .replace("{params}", params)
            .replace("{illustid}", illust.id.toString())
            .replace("{part}", part?.toString() ?: "0")
            .replace("{type}", type)
    }
    fun imgD(pid: Long, part: Int?) {
        RetrofitRepository.getInstance().getIllust(pid)
            .subscribeOn(Schedulers.io()).map {
                imgD(it.illust, part)
            }
            .observeOn(AndroidSchedulers.mainThread()).subscribe()
    }
    fun imgD(illust: Illust, part: Int?) {
        var url = (
            if (part != null && illust.meta_pages.isNotEmpty()) {
                illust.meta_pages[part].image_urls.original
            }
            else {
                illust.meta_single_page.original_image_url!!
            }
            )
        // url = mirror(illust, part, url)
        url = mirrorLinkDownload(url)
        val name = illust.user.name.toLegal()
        val title = illust.title.toLegal()
        val filename = parseSaveFormat(illust, part)
        val needCreateFold = pre.getBoolean("needcreatefold", false)
        val path = if (needCreateFold) {
            "${PxEZApp.storepath}/${name}_${illust.user.id}"
        }
        else {
            PxEZApp.storepath
        }
        val targetFile = File(path, filename)
        if (targetFile.exists()) {
            Toasty.shortToast(R.string.alreadysaved)
            return
        }
        val illustD = IllustD(
            id = illust.id,
            part = part ?: 0,
            preview = illust.image_urls.square_medium,
            userName = name,
            userId = illust.user.id,
            userAvatar = illust.user.profile_image_urls.medium,
            title = title,
            url = url
        )
        val targetPath = "${PxEZApp.instance.cacheDir}${File.separator}$filename"
        Aria.download(PxEZApp.instance)
            .load(url) // 读取下载地址
            .setFilePath(targetPath) // 设置文件保存的完整路径
            .ignoreFilePathOccupy()
            .setExtendField(Gson().toJson(illustD))
            .option(option)
            .create()
    }

/*
    @Deprecated("imgD")
    fun imageDownloadOne(illust: Illust, part: Int?) {
        var url = ""
        val title = illust.title.toLegal()
        val userName = illust.user.name.toLegal()
        val user = illust.user.id
        val name = illust.id
        val format = pre.getString(
            "saveformat",
            "0"
        )?.toInt()
            ?: 0
        val needCreateFold = pre.getBoolean("needcreatefold", false)
        var type = ".png"
        var filename = "${name}_p$part$type"
        if (part != null && illust.meta_pages.isNotEmpty()) {
            url = illust.meta_pages[part].image_urls.original
            type = if (url.contains("png")) {
                ".png"
            } else ".jpg"
            when (format) {
                0 -> {
                    filename = "${name}_$part$type"
                }
                1 -> {
                    filename = "${name}_p$part$type"
                }
                2 -> {
                    filename = "${name}_${name}_$part$type"
                }
                3 -> {
                    filename = "${name}_${title}_$part$type"
                }
            }
        }
        else {
            url = illust.meta_single_page.original_image_url!!
            type = if (url.contains("png")) {
                ".png"
            } else ".jpg"
            when (format) {
                0 -> {
                    filename = "$name$type"
                }
                1 -> {
                    filename = "$name$type"
                }
                2 -> {
                    filename = "${name}_$name$type"
                }
                3 -> {
                    filename = "${name}_${title}$type"
                }
            }
        }


    }
*/
}
