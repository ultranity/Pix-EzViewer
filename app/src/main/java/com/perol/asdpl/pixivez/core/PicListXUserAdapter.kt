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

import android.app.Activity
import android.app.ActivityOptions
import android.graphics.drawable.Drawable
import android.util.Pair
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.chad.brvah.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.DMutableLiveData
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import com.perol.asdpl.pixivez.view.NiceImageView

/**
 *  simple Adapter for image item with user imageView and heart icon
 */
class PicListXUserAdapter(
    layoutResId: Int, filter: PicsFilter
) : PicListXAdapter(layoutResId, filter) {

    /*override fun viewPicsOptions(view: View, illust: Illust): Bundle {
        return viewOptions(this, view, illust)
    }*/

    override fun setUIFollow(status: Boolean, position: Int) {
        (getViewByAdapterPosition(
            position, R.id.imageview_user
        ) as NiceImageView?)?.let { badgeUIFollow(this, status, it) }
    }

    override fun setUIFollow(status: Boolean, view: View) {
        val user = view as NiceImageView
        badgeUIFollow(this, status, user)
    }

    val followLiveDatas = ArrayList<DMutableLiveData<Boolean>>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        val followLiveData = DMutableLiveData(false, true)
        followLiveData.observeAfterSet(parent.context as LifecycleOwner) {
            holder.getView<NiceImageView>(R.id.imageview_user).apply {
                setUIFollow(it, this)
            }
        }
        likeLiveDatas.add(followLiveData)
        holder.itemView.setTag(followDataID, followLiveData)
        return holder
    }

    override fun convert(holder: BaseViewHolder, item: Illust) {
        super.convert(holder, item)
        convertUser(this, holder, item)
    }

    companion object {
        private val followDataID = "followLiveData".hashCode()
        fun convertUser(picListAdapter: PicListAdapter, holder: BaseViewHolder, illust: Illust) {
            val user = illust.user
            val imageViewUser = holder.getView<NiceImageView>(R.id.imageview_user)
            imageViewUser.setOnClickListener {
                val options = if (PxEZApp.animationEnable) {
                    ActivityOptions.makeSceneTransitionAnimation(
                        picListAdapter.context as Activity,
                        Pair(imageViewUser, "shared_element_container")//"userimage")
                    ).toBundle()
                } else null
                UserMActivity.start(picListAdapter.context, user, options)
            }
            imageViewUser.setOnLongClickListener {
                //val id = user.id
                if (!user.is_followed) {
                    InteractionUtil.follow(illust) {
                        imageViewUser.setBorderColor(picListAdapter.badgeTextColor) // Color.YELLOW
                        // imageViewUser.alpha = 0.9F
                    }
                } else {
                    InteractionUtil.unfollow(user) {
                        imageViewUser.setBorderColor(picListAdapter.colorPrimary)
                        // imageViewUser.alpha = 0.5F
                    }
                }
                true
            }
            imageViewUser.setTag(R.id.tag_first, user.profile_image_urls.medium)

            Glide.with(imageViewUser.context).load(user.profile_image_urls.medium)
                .circleCrop()
                .into(object : ImageViewTarget<Drawable>(imageViewUser) {
                    override fun setResource(resource: Drawable?) {
                        imageViewUser.setImageDrawable(resource)
                    }

                    override fun onResourceReady(
                        resource: Drawable, transition: Transition<in Drawable>?
                    ) {
                        if (user.profile_image_urls.medium === imageViewUser.getTag(R.id.tag_first)) {
                            super.onResourceReady(resource, transition)
                        }
                    }
                })

            //picListAdapter.setUIFollow(user.is_followed, imageViewUser)
            (holder.itemView.getTag(followDataID) as DMutableLiveData<Boolean>?)?.let {
                it.triggerValue(user.is_followed)
                user.addBinder("${illust.id}-${this.hashCode()}", it)
            }
        }

        fun badgeUIFollow(picListAdapter: PicListAdapter, status: Boolean, user: NiceImageView) {
            if (status) {
                // user.alpha = 0.9F
                user.setBorderColor(picListAdapter.badgeTextColor) // Color.YELLOW
            } else {
                // user.alpha = 0.5F
                user.setBorderColor(picListAdapter.colorPrimary)
            }
        }

        /*fun viewOptions(picListAdapter: PicListAdapter, view: View, illust: Illust): Bundle {
            val mainimage = view.findViewById<View>(R.id.item_img)
            val userImage = view.findViewById<View>(R.id.imageview_user)

            val options = if (illust.meta.size > 1) {
                ActivityOptions.makeSceneTransitionAnimation(
                    picListAdapter.context as Activity, Pair(mainimage, "mainimage")
                ).toBundle()
            } else {
                ActivityOptions.makeSceneTransitionAnimation(
                    picListAdapter.context as Activity,
                    Pair(mainimage, "mainimage"),
                    Pair(userImage, "userimage")
                ).toBundle()
            }
            return options
        }*/
    }
}
