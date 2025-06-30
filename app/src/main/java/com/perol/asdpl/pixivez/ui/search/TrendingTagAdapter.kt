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

import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.chad.brvah.BaseQuickAdapter
import com.chad.brvah.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.model.TrendTagsBean
import com.perol.asdpl.pixivez.objects.ThemeUtil

class TrendingTagAdapter(data: MutableList<TrendTagsBean>?) :
    BaseQuickAdapter<TrendTagsBean, BaseViewHolder>(R.layout.view_trendingtag_item, data) {
    override fun convert(holder: BaseViewHolder, item: TrendTagsBean) {
        holder.setText(R.id.textview_tag, item.tag)
        val imageView = holder.itemView.findViewById<ImageView>(R.id.imageview_trendingtag)
        Glide.with(imageView.context)
            .load(item.illust.meta[0].square_medium)
            .placeholder(ThemeUtil.HALF_TRANS.toDrawable())
            .into(imageView)
    }
}
