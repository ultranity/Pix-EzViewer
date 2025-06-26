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

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.chad.brvah.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.view.NiceImageView

fun ImageView.setLike(context: Context, status: Boolean) {
    if (status) {
        // setImageResource(R.drawable.heart_red)
        Glide.with(context).load(R.drawable.ic_love).into(this)
        // alpha = 0.9F
    } else {
        Glide.with(context).load(R.drawable.ic_love_outline).into(this)
        // alpha = 0.8F
    }
}

class Payload(
    val type: String,
    val value: Any? = null
)

/**
 *  simple Adapter for image item with heart icon
 */
// TODO: rename
open class PicListXAdapter(
    filter: PicsFilter,
    layoutResId: Int = R.layout.view_recommand_item_s,
) :
    PicListAdapter(filter, layoutResId) {

    override fun convert(holder: BaseViewHolder, item: Illust) {
        super.convert(holder, item)
        val likeView = holder.getView<NiceImageView>(R.id.imageview_like)
        if (PxEZApp.CollectMode == 1) {
            likeView.setOnClickListener {
                // download
                likeView.setBorderColor(colorPrimaryDark)
                Works.imageDownloadAll(item)
                // set like
                if (!item.is_bookmarked) {
                    InteractionUtil.like(item, null)
                }
            }
        }
        else {
            likeView.setOnClickListener {
                if (item.is_bookmarked) {
                    InteractionUtil.unlike(item)
                } else {
                    InteractionUtil.like(item, null)
                }
            }
            likeView.setOnLongClickListener {
                setUIDownload(1, likeView)
                Works.imageDownloadAll(item)
                true
            }
        }
        setUIDownload(if (FileUtil.isDownloaded(item)) 2 else 0, likeView)
    }

    override fun setUILike(status: Boolean, holder: BaseViewHolder) {
        setUILike(status, holder.getView(R.id.imageview_like))
    }

    override fun setUILike(status: Boolean, position: Int) {
        (getViewByAdapterPosition(
            position,
            R.id.imageview_like
        ) as NiceImageView?)?.setLike(context, status)
    }

    override fun setUILike(status: Boolean, view: View) {
        (view as NiceImageView).setLike(view.context, status)
    }

    override fun setUIDownload(status: Int, position: Int) {
        (
            getViewByAdapterPosition(
                position,
                R.id.imageview_like
            ) as NiceImageView?
            )?.let { setUIDownload(status, it) }
    }

    override fun setUIDownload(status: Int, view: View) {
        val like = view as NiceImageView
        when (status) {
            0 -> { // not downloaded
                like.setBorderColor(colorTransparent)
            }
            1 -> { // Downloading
                like.setBorderColor(colorPrimaryDark)
            }
            2 -> { // Downloaded
                like.setBorderColor(badgeTextColor)
            }
        }
    }
}
