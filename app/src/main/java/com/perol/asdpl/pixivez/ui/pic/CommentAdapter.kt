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
package com.perol.asdpl.pixivez.ui.pic

import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.chad.brvah.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.LBaseQuickAdapter
import com.perol.asdpl.pixivez.data.model.CommentsBean
import com.perol.asdpl.pixivez.objects.EmojiUtil
import com.perol.asdpl.pixivez.objects.GlideAssetsImageGetter

class CommentAdapter(
    layoutResId: Int,
    data: MutableList<CommentsBean>?
) : LBaseQuickAdapter<CommentsBean, BaseViewHolder>(layoutResId, data) {
    override fun convert(holder: BaseViewHolder, item: CommentsBean) {
        holder.setText(R.id.comment_date, item.date)
        holder.setText(R.id.comment_username, item.user.name)
        val comment_detail = holder.getView<TextView>(R.id.comment_detail)
        val comment = EmojiUtil.transform(item.comment)
        comment_detail.text = if (comment.hashCode() == item.comment.hashCode()) {
            item.comment
        } else {
            Html.fromHtml(comment, GlideAssetsImageGetter(comment_detail, "Emoji"), null)
        }

        if (!item.user.profile_image_urls.medium!!
                .contentEquals("https://source.pixiv.net/common/images/no_profile.png")
        ) {
            Glide.with(context).load(item.user.profile_image_urls.medium)
                .placeholder(R.mipmap.ic_noimage_foreground).circleCrop().into(
                    (holder.getView<View>(R.id.commentuserimage) as ImageView)
                )
        } else {
            Glide.with(context).load(R.mipmap.ic_noimage_round)
                .into((holder.getView<View>(R.id.commentuserimage) as ImageView))
        }
    }
}
