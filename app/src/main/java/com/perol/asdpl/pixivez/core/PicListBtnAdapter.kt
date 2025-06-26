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

package com.perol.asdpl.pixivez.core

import android.view.View
import com.chad.brvah.viewholder.BaseViewHolder
import com.google.android.material.button.MaterialButton
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.KotlinUtil.Int
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works

/**
 *  simple Adapter for image item with save/like button
 */
open class PicListBtnAdapter(
    filter: PicsFilter,
    layoutResId: Int = R.layout.view_recommand_item,
) :
    PicListAdapter(filter, layoutResId) {

    override fun convert(holder: BaseViewHolder, item: Illust) {
        super.convert(holder, item)

        holder.setText(R.id.title, item.title)
        val likeBtn = holder.getView<MaterialButton>(R.id.like)
        val saveBtn = holder.getView<MaterialButton>(R.id.save)
        setUIDownload(FileUtil.isDownloaded(item).Int(), saveBtn)

        if (PxEZApp.CollectMode == 1) {
            saveBtn.setOnClickListener {
                holder.setTextColor(R.id.save, colorPrimaryDark)
                Works.imageDownloadAll(item)
                if (!item.is_bookmarked) {
                    InteractionUtil.like(item, null)
                }
            }
        } else {
            saveBtn.setOnClickListener {
                setUIDownload(1, it)
                Works.imageDownloadAll(item)
            }
        }

        likeBtn.setOnClickListener { v ->
            if (!item.is_bookmarked) {
                InteractionUtil.like(item, null)
            } else {
                InteractionUtil.unlike(item)
            }
        }
        likeBtn.setOnLongClickListener { v ->
            InteractionUtil.like(item, forcePrivate = true) {
                //setUILike(true, v)
            }
            true
        }
        //setUILike(item.is_bookmarked, likeBtn)
    }

    override fun setUILike(status: Boolean, holder: BaseViewHolder) {
        setUILike(status, holder.getView(R.id.like))
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
