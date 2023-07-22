/*
 * MIT License
 *
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

package com.perol.asdpl.pixivez.ui.search

import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.model.TrendingtagResponse.TrendTagsBean
import com.perol.asdpl.pixivez.objects.ThemeUtil

class TrendingTagAdapter(layoutResId: Int, data: List<TrendTagsBean>?) :
    BaseQuickAdapter<TrendTagsBean, BaseViewHolder>(
        layoutResId,
        data?.toMutableList()
    ) {
    override fun convert(holder: BaseViewHolder, item: TrendTagsBean) {
        holder.setText(R.id.textview_tag, item.tag)
        val imageView = holder.itemView.findViewById<ImageView>(R.id.imageview_trendingtag)
        Glide.with(imageView.context)
            .load(item.illust.image_urls.square_medium)
            .placeholder(ColorDrawable(ThemeUtil.halftrans))
            .into(imageView)
    }
}