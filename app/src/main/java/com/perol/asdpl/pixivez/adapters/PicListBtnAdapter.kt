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

package com.perol.asdpl.pixivez.adapters

import android.view.View
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.button.MaterialButton
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works

/**
 *  simple Adapter for image item with save/like button
 */
open class PicListBtnAdapter(
    layoutResId: Int,
    data: List<Illust>?,
    filter: IllustFilter
) :
    PicListAdapter(layoutResId, data?.toMutableList(), filter) {

    override fun convert(holder: BaseViewHolder, item: Illust) {
        super.convert(holder, item)

        holder.setText(R.id.title, item.title)

        holder.setTextColor(
            R.id.save,
            if (FileUtil.isDownloaded(item)) {
                badgeTextColor
            }
            else {
                colorPrimary
            }
        )

        if (PxEZApp.CollectMode == 1) {
            holder.getView<MaterialButton>(R.id.save).setOnClickListener {
                holder.setTextColor(R.id.save, colorPrimaryDark)
                Works.imageDownloadAll(item)
                if (!item.is_bookmarked) {
                    InteractionUtil.like(item, null) {
                        holder.getView<MaterialButton>(R.id.like).setTextColor(badgeTextColor)
                    }
                }
            }
        }
        else {
            holder.getView<MaterialButton>(R.id.save).setOnClickListener {
                holder.setTextColor(R.id.save, colorPrimaryDark)
                Works.imageDownloadAll(item)
            }
        }

        holder.getView<MaterialButton>(R.id.like).apply {
            setOnClickListener { v ->
                if (!item.is_bookmarked) {
                    InteractionUtil.like(item, null) {
                        setUILike(true, v)
                    }
                }
                else {
                    InteractionUtil.unlike(item) {
                        setUILike(false, v)
                    }
                }
            }
            setUILike(item.is_bookmarked, this)
        }
    }

    override fun setUILike(status: Boolean, position: Int) {
        (
            getViewByAdapterPosition(
                position,
                R.id.like
            ) as MaterialButton?
            )?.let { setUILike(status, it) }
    }

    override fun setUILike(status: Boolean, view: View) {
        val like = view as MaterialButton
        like.setTextColor(if (status) badgeTextColor else colorPrimary)
    }

    override fun setUIFollow(status: Boolean, position: Int) {
        return
    }

    override fun setUIFollow(status: Boolean, view: View) {
        return
    }

    override fun setUIDownload(status: Int, position: Int) {
        (
            getViewByAdapterPosition(
                position,
                R.id.save
            ) as MaterialButton?
            )?.let { setUIDownload(status, it) }
    }

    override fun setUIDownload(status: Int, view: View) {
        val save = view as MaterialButton
        when (status) {
            0 -> { // not downloaded
                save.setTextColor(colorPrimary)
            }
            1 -> { // Downloading
                save.setTextColor(colorPrimaryDark)
            }
            2 -> { // Downloaded
                save.setTextColor(badgeTextColor)
            }
        }
    }
}
