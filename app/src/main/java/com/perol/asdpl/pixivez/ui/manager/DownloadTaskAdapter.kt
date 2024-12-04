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

package com.perol.asdpl.pixivez.ui.manager

import android.annotation.SuppressLint
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadEntity
import com.chad.brvah.BaseQuickAdapter
import com.chad.brvah.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.databinding.ItemDownloadTaskBinding
import com.perol.asdpl.pixivez.networks.ServiceFactory.gson
import com.perol.asdpl.pixivez.objects.ViewBindingUtil.getBinding
import com.perol.asdpl.pixivez.services.IllustD
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.ui.pic.PictureActivity

@SuppressLint("CheckResult")
class DownloadTaskAdapter :
    BaseQuickAdapter<DownloadEntity, BaseViewHolder>(R.layout.item_download_task) {
    init {
        this.setOnItemClickListener { adapter, view, position ->
            val item = data[position]
            val illust = gson.decodeFromString<IllustD>(item.str)
            PictureActivity.start(context, id = illust.id)
        }
        this.setOnItemLongClickListener { adapter, view, position ->
            val item = data[position]
            MaterialDialogs(context).show {
                setTitle(item.fileName)
                setItems(R.array.download_task_choice) { _, index ->
                    when (index) {
                        0 -> {
                            Aria.download(PxEZApp.instance)
                                .load(item.url) // 读取下载地址
                                .setFilePath(item.filePath) // 设置文件保存的完整路径
                                .ignoreFilePathOccupy()
                                .setExtendField(item.str)
                                .option(Works.option)
                                .create()
                        }

                        1 -> {
                            Aria.download(context).load(data[position].id).stop()
                        }

                        2 -> {
                            Aria.download(context).load(data[position].id).resume()
                        }

                        3 -> {
                            Aria.download(context).load(data[position].id).cancel()
                        }
                    }
//                    val taskList = Aria.download(this).taskList
//                    if (taskList?.isNotEmpty() == true) {
//                        this@DownloadTaskAdapter.setNewInstance(taskList.asReversed())
//                    }
                }
            }
            true
        }
    }

    private fun Int.toIEntityString(): String {
        return when (this) {
            0 -> {
                "FAIL"
            }

            1 -> "COMPLETE"
            2 -> "STOP"
            3 -> "WAIT"
            4 -> "RUNNING"
            5 -> {
                "PRE"
            }

            6 -> {
                "POST_PRE"
            }

            7 -> {
                "CANCEL"
            }

            else -> {
                "OTHER"
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: DownloadEntity, payloads: List<Any>) {
        val binding = holder.getBinding(ItemDownloadTaskBinding::bind)
        if (payloads.isNotEmpty()) {
            val thatItem = payloads[0] as DownloadEntity
            binding.progress.max = thatItem.fileSize.toInt()
            binding.progress.progress = thatItem.currentProgress.toInt()
            binding.progressFont.text =
                context.getString(R.string.fractional, thatItem.currentProgress, thatItem.fileSize)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: DownloadEntity) {
        val binding = holder.getBinding(ItemDownloadTaskBinding::bind)
        binding.progress.max = item.fileSize.toInt()
        binding.progress.progress = item.currentProgress.toInt()
        binding.progressFont.text =
            context.getString(R.string.fractional, item.currentProgress, item.fileSize)
        try {
            val illustD = gson.decodeFromString<IllustD>(item.str)
            binding.title.text =
                if (illustD.part != -1) "${illustD.title}_${illustD.part}" else illustD.title
            binding.status.text = item.state.toIEntityString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
