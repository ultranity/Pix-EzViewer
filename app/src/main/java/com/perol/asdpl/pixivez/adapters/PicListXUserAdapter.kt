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
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Pair
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.GlideApp
import com.perol.asdpl.pixivez.services.PxEZApp
import com.shehuan.niv.NiceImageView
import kotlin.math.max
import kotlin.math.min

// simple Adapter for image item with user imageView and heart icon
// TODO: rename
class PicListXUserAdapter(
    layoutResId: Int,
    data: List<Illust>?,
    filter: IllustFilter
) :
    PicListXAdapter(layoutResId, data, filter), LoadMoreModule {

    override fun viewPics(view: View, position: Int) {
        // super.viewPics(view, position)
        val bundle = Bundle()
        DataHolder.setIllustsList(this.data.subList(max(position - 30, 0), min(this.data.size, max(position - 30, 0) + 60)))
        bundle.putInt("position", position - max(position - 30, 0))
        bundle.putLong("illustid", this.data[position].id)
        val intent = Intent(context, PictureActivity::class.java)
        intent.putExtras(bundle)
        if (PxEZApp.animationEnable) {
            val mainimage = view.findViewById<View>(R.id.item_img)
            val userImage = view.findViewById<View>(R.id.imageview_user)

            val options =
                if (this.data[position].meta_pages.size > 1) {
                    ActivityOptions.makeSceneTransitionAnimation(
                        context as Activity,
                        Pair.create(
                            mainimage,
                            "mainimage"
                        )
                    )
                }
                else {
                    ActivityOptions.makeSceneTransitionAnimation(
                        context as Activity,
                        Pair.create(mainimage, "mainimage"),
                        Pair.create(userImage, "userimage")
                    )
                }
            ContextCompat.startActivity(context, intent, options.toBundle())
        }
        else {
            ContextCompat.startActivity(context, intent, null)
        }
    }

    override fun setUIFollow(status: Boolean, position: Int) {
        (
            getViewByAdapterPosition(
                position,
                R.id.imageview_user
            ) as NiceImageView?
            )?.let { setUIFollow(status, it) }
    }

    override fun setUIFollow(status: Boolean, view: View) {
        val user = view as NiceImageView
        if (status) {
            // user.alpha = 0.9F
            user.setBorderColor(badgeTextColor) // Color.YELLOW
        }
        else {
            // user.alpha = 0.5F
            user.setBorderColor(colorPrimary)
        }
    }

    override fun convert(holder: BaseViewHolder, item: Illust) {
        super.convert(holder, item)
        convertUser(holder, item)
    }

    private fun convertUser(
        holder: BaseViewHolder,
        item: Illust
    ) {
        val imageViewUser = holder.getView<NiceImageView>(R.id.imageview_user)
        setUIFollow(item.user.is_followed, imageViewUser)
        imageViewUser.setOnClickListener {
            val intent = Intent(context, UserMActivity::class.java)
            intent.putExtra("data", item.user.id)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (PxEZApp.animationEnable) {
                val options = ActivityOptions.makeSceneTransitionAnimation(
                    context as Activity,
                    Pair.create(imageViewUser, "userimage")
                )
                context.startActivity(intent, options.toBundle())
            }
            else {
                context.startActivity(intent)
            }
        }
        imageViewUser.setOnLongClickListener {
            val id = item.user.id
            if (!item.user.is_followed) {
                InteractionUtil.follow(item) {
                    imageViewUser.setBorderColor(badgeTextColor) // Color.YELLOW
                    // imageViewUser.alpha = 0.9F
                }
            }
            else {
                InteractionUtil.unfollow(item) {
                    imageViewUser.setBorderColor(colorPrimary)
                    // imageViewUser.alpha = 0.5F
                }
            }
            true
        }
        imageViewUser.setTag(R.id.tag_first, item.user.profile_image_urls.medium)

        GlideApp.with(imageViewUser.context).load(item.user.profile_image_urls.medium).circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(object : ImageViewTarget<Drawable>(imageViewUser) {
                override fun setResource(resource: Drawable?) {
                    imageViewUser.setImageDrawable(resource)
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    if (item.user.profile_image_urls.medium === imageViewUser.getTag(R.id.tag_first)) {
                        super.onResourceReady(resource, transition)
                    }
                }
            })
    }
}
