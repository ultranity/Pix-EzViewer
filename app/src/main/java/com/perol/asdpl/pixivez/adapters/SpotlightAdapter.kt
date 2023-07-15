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

import android.app.Activity
import android.app.ActivityOptions
import android.util.Pair
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.objects.Spotlight
import com.perol.asdpl.pixivez.services.PxEZApp

class SpotlightAdapter(layoutResId: Int, data: List<Spotlight>?) :
    BaseQuickAdapter<Spotlight, BaseViewHolder>(layoutResId, data?.toMutableList()) {

    override fun convert(holder: BaseViewHolder, item: Spotlight) {
        if (item.illust.meta_pages.isNotEmpty()) {
            holder.setText(R.id.textview_num, item.illust.meta_pages.size.toString())
        }
        holder.setGone(R.id.textview_num, item.illust.meta_pages.isNotEmpty())
        val userImage = holder.getView<ImageView>(R.id.imageview_user)
        val mainImage = holder.getView<ImageView>(R.id.item_img)
        holder.setText(R.id.textview_context, item.username)
            .setText(R.id.title, item.title)
        Glide.with(mainImage.context).load(item.illust.image_urls.medium)
            .error(R.drawable.ai).transition(withCrossFade()).into(mainImage)
        Glide.with(userImage.context).load(item.illust.user.profile_image_urls.medium)
            .placeholder(R.mipmap.ic_noimage_foreground)
            .transition(withCrossFade()).circleCrop()
            .into(userImage)
        mainImage.setOnClickListener {
            PictureActivity.start(context, item.illust)
        }
        userImage.setOnClickListener {
            val options = if (PxEZApp.animationEnable) {
                ActivityOptions.makeSceneTransitionAnimation(
                    context as Activity,
                    Pair(userImage, "userimage")
                ).toBundle()
            } else null
            UserMActivity.start(context, item.illust.user, options)
        }
    }
}
