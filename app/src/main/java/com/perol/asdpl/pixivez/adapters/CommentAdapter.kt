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

import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.objects.EmojiUtil
import com.perol.asdpl.pixivez.objects.GlideAssetsImageGetter
import com.perol.asdpl.pixivez.responses.IllustCommentsResponse.CommentsBean

class CommentAdapter(
    layoutResId: Int,
    data: MutableList<CommentsBean>?
) : BaseQuickAdapter<CommentsBean, BaseViewHolder>(layoutResId, data), LoadMoreModule {
    override fun convert(holder: BaseViewHolder, item: CommentsBean) {
        holder.setText(R.id.commentdate, item.date)
        if (item.parent_comment.user != null) {
            holder.setText(
                R.id.commentusername,
                item.user.name + " to " + item.parent_comment.user.name
            )
        }
        else {
            holder.setText(
                R.id.commentusername,
                item.user.name
            )
        }
        val commentdetail = holder.getView<TextView>(R.id.commentdetail)
        val comment = EmojiUtil.transform(item.comment)
        commentdetail.text = if (comment.hashCode() == item.comment.hashCode()) {
            item.comment
        }
        else {
            Html.fromHtml(comment, GlideAssetsImageGetter(commentdetail, "Emoji"), null)
        }

        if (!item.user.profile_image_urls.medium!!
            .contentEquals("https://source.pixiv.net/common/images/no_profile.png")
        ) {
            Glide.with(context).load(item.user.profile_image_urls.medium)
                .placeholder(R.mipmap.ic_noimage_foreground).circleCrop().into(
                    (holder.getView<View>(R.id.commentuserimage) as ImageView)
                )
        }
        else {
            Glide.with(context).load(R.mipmap.ic_noimage_round)
                .into((holder.getView<View>(R.id.commentuserimage) as ImageView))
        }
    }
}
