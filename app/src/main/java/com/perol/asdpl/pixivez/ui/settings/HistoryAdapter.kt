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

package com.perol.asdpl.pixivez.ui.settings

import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.chad.brvah.BaseQuickAdapter
import com.chad.brvah.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.entity.HistoryEntity
import com.perol.asdpl.pixivez.objects.ThemeUtil

// TODO: reuse
class HistoryAdapter :
    BaseQuickAdapter<HistoryEntity, BaseViewHolder>(R.layout.view_history_item) {
    override fun convert(holder: BaseViewHolder, item: HistoryEntity) {
        val imageView = holder.getView<ImageView>(R.id.item_img)
        Glide.with(imageView.context).load(item.thumb)
            .placeholder(ThemeUtil.HALF_TRANS.toDrawable()).into(imageView)
        holder.setText(R.id.title, item.title)
        holder.setTextColor(
            R.id.title,
            if (item.isUser) ThemeUtil.getColorHighlight(context)
            else ThemeUtil.getTextColorPrimary(context)
        )
    }
}
