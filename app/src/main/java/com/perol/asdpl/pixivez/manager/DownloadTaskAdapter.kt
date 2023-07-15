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

package com.perol.asdpl.pixivez.manager

import android.annotation.SuppressLint
import android.view.View
import androidx.viewbinding.ViewBinding
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadEntity
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.gson.Gson
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.databinding.ItemDownloadTaskBinding
import com.perol.asdpl.pixivez.services.IllustD
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
@Suppress("UNCHECKED_CAST")
fun <VB : ViewBinding> BaseViewHolder.getBinding(bind: (View) -> VB): VB =
    itemView.getTag(Int.MIN_VALUE) as? VB ?: bind(itemView).also { itemView.setTag(Int.MIN_VALUE, it) }

@SuppressLint("CheckResult")
class DownloadTaskAdapter :
    BaseQuickAdapter<DownloadEntity, BaseViewHolder>(R.layout.item_download_task) {
    init {
        this.setOnItemClickListener { adapter, view, position ->
            val item = data[position]
            val illust = Gson().fromJson(item.str, IllustD::class.java)
            PictureActivity.start(context, id = illust.id)
        }
        this.setOnItemLongClickListener { adapter, view, position ->
            val item = data[position]
            MaterialDialog(context).show {
                title(text = item.fileName)
                listItems(R.array.download_task_choice) { _, index, string ->
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
                    val taskList = Aria.download(this).taskList
                    if (taskList?.isNotEmpty() == true) {
                        this@DownloadTaskAdapter.setNewInstance(taskList.asReversed())
                    }
                }
            }
            true
        }
    }

    /*override fun onItemViewHolderCreated(
        viewHolder: BaseViewHolder,
        viewType: Int
    ) { // 绑定 view
        val binding = ItemDownloadTaskBinding.bind(viewHolder.itemView)
    }*/

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
            val illustD = Gson().fromJson(item.str, IllustD::class.java)
            binding.title.text = illustD.title
            binding.status.text =  item.state.toIEntityString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
