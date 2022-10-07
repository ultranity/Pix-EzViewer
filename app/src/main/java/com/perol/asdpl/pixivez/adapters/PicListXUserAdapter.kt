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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.databinding.ViewRankingItemSBinding
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.GlideApp
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.shehuan.niv.NiceImageView
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min

// simple Adapter for image item with user imageView and heart icon
//TODO: rename
class PicListXUserAdapter(
    layoutResId: Int,
    data: List<Illust>?,
    override var R18on: Boolean,
    override var blockTags: List<String>,
    override var hideBookmarked: Int = 0,
    override var hideDownloaded: Boolean = false,
    override var sortCoM: Int = 0
) :
    PicItemXUserAdapter(layoutResId, data?.toMutableList()), LoadMoreModule {

    init {
        if (PxEZApp.CollectMode == 2) {
            setOnItemClickListener { adapter, view, position ->
                (adapter.data as ArrayList<Illust?>)[position]?.let {item ->
                    val like = view.findViewById<NiceImageView>(R.id.imageview_like)
                    like.setBorderColor(colorPrimaryDark)
                    Works.imageDownloadAll(item)
                    if (!item.is_bookmarked){
                        retrofitRepository.postLikeIllustWithTags(item.id, x_restrict(item), null).subscribe({
                            //helper.bd.imageviewLike
                            like.setImageResource(R.drawable.heart_red)
                            //GlideApp.with(context).load(R.drawable.heart_red).into(like)
                            like.alpha = 0.9F
                            item.is_bookmarked = true
                        }, {}, {})
                    }
                    if (!item.user.is_followed) {
                        retrofitRepository.postfollowUser(item.user.id, x_restrict(item)).subscribe({
                            item.user.is_followed = true
                            view.findViewById<NiceImageView>(R.id.imageview_user)
                                .apply{
                                    //alpha = 0.9F
                                    setBorderColor(badgeTextColor) // Color.YELLOW
                                }
                        }, {}, {})
                    }
                }
            }
            setOnItemLongClickListener { adapter, view, position ->
                val bundle = Bundle()
                DataHolder.setIllustsList(this.data.subList(max(position-30,0), min(this.data.size,max(position-30,0)+60)))
                bundle.putInt("position",position - max(position-30,0))
                bundle.putLong("illustid", this.data[position].id)
                val intent = Intent(context, PictureActivity::class.java)
                intent.putExtras(bundle)
                if (PxEZApp.animationEnable) {
                    val mainimage = view.findViewById<View>(R.id.item_img)
                    val userImage = view.findViewById<View>(R.id.imageview_user)

                    val options =
                        if (this.data[position].meta_pages.size>1)
                            ActivityOptions.makeSceneTransitionAnimation(
                                context as Activity,
                                Pair.create(
                                    mainimage,
                                    "mainimage"
                                )
                            )
                        else
                            ActivityOptions.makeSceneTransitionAnimation(
                                context as Activity,
                                Pair.create(
                                    mainimage,
                                    "mainimage"
                                ),
                                Pair.create(userImage, "userimage")
                            )
                    ContextCompat.startActivity(context, intent, options.toBundle())
                } else
                    ContextCompat.startActivity(context, intent, null)
                true
            }
        }
        else {
            setOnItemClickListener { adapter, view, position ->
                val bundle = Bundle()
                //bundle.putLong("illustid", this.data[position].id)
                //val illustlist = LongArray(this.data.count())
                //for (i in this.data.indices) {
                //    illustlist[i] = this.data[i].id
                //}
                //bundle.putParcelable("illust", this.data[position])
                DataHolder.setIllustsList(this.data.subList(max(position-30,0), min(this.data.size,max(position-30,0)+60)))
                bundle.putInt("position",position - max(position-30,0))
                //  bundle.putParcelable(this.data[position].id.toString(), this.data[position])
                bundle.putLong("illustid", this.data[position].id)
                val intent = Intent(context, PictureActivity::class.java)
                intent.putExtras(bundle)
                if (PxEZApp.animationEnable) {
                    val mainimage = view.findViewById<View>(R.id.item_img)
                    val userImage = view.findViewById<View>(R.id.imageview_user)

                    val options = ActivityOptions.makeSceneTransitionAnimation(
                        context as Activity,
                        Pair.create(
                            mainimage,
                            "mainimage"
                        ),
                        Pair.create(userImage, "userimage")
                    )
                    ContextCompat.startActivity(context, intent, options.toBundle())
                } else
                    ContextCompat.startActivity(context, intent, null)
            }
            setOnItemLongClickListener { adapter, view, position ->
                //show detail of illust
                (adapter.data as ArrayList<Illust?>)[position]?.let {
                    val detailstring =
                        "id: " + it.id.toString() +
                                "caption: " + it.caption + "create_date: " + it.create_date +
                                "width: " + it.width.toString() + "height: " + it.height.toString() +
                                //+ "image_urls: " + illust.image_urls.toString() + "is_bookmarked: " + illust.is_bookmarked.toString() +
                                "user: " + it.user.name +
                                "tags: " + it.tags.toString() +// "title: " + illust.title.toString() +
                                "total_bookmarks: " + it.total_bookmarks.toString() +
                                "total_view: " + it.total_view.toString() +
                                "user account: " + it.user.account + "\n" +
                                "tools: " + it.tools.toString() + "\n" +
                                "type: " + it.type + "\n" +
                                "page_count: " + it.page_count.toString() + "\n" +
                                "visible: " + it.visible.toString() + "\n" +
                                "is_muted: " + it.is_muted.toString() + "\n" +
                                "sanity_level: " + it.sanity_level.toString() + "\n" +
                                "restrict: " + it.restrict.toString() + "\n" +
                                "x_restrict: " + it.x_restrict.toString()
                    MaterialAlertDialogBuilder(context as Activity)
                        .setMessage(detailstring)
                        .setTitle("Detail")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                        }
                        .create().show()
                }
                true
            }
        }
    }

    override fun convert(helper: BaseVBViewHolder<ViewRankingItemSBinding>, item: Illust) {
        super.convert(helper, item)
        /* 将导致无法正常刷新状态
        if (PxEZApp.CollectMode == 2) {
            setOnItemClickListener { adapter, view, position ->
                (adapter.data as ArrayList<Illust?>)[position]?.let {item ->
                    helper.bd.imageviewLike.setBorderColor(colorPrimaryDark)
                    Works.imageDownloadAll(item)
                    if (!item.is_bookmarked){
                        retrofitRepository.postLikeIllustWithTags(item.id, x_restrict(item), null).subscribe({
                            //helper.bd.imageviewLike.setImageResource(R.drawable.heart_red)
                            GlideApp.with(context).load(R.drawable.heart_red).into(helper.bd.imageviewLike)
                            item.is_bookmarked = true
                        }, {}, {})
                    }
                    if (!item.user.is_followed) {
                        retrofitRepository.postfollowUser(item.user.id, x_restrict(item)).subscribe({
                            item.user.is_followed = true
                            helper.bd.imageviewUser
                                .setBorderColor(badgeTextColor) // Color.YELLOW
                        }, {}, {})
                    }
                }
            }
            setOnItemLongClickListener { adapter, view, position ->
                val bundle = Bundle()
                DataHolder.setIllustsList(this.data.subList(max(position-30,0), min(this.data.size,max(position-30,0)+60)))
                bundle.putInt("position",position - max(position-30,0))
                bundle.putLong("illustid", this.data[position].id)
                val intent = Intent(context, PictureActivity::class.java)
                intent.putExtras(bundle)
                if (PxEZApp.animationEnable) {
                    val mainimage = view.findViewById<View>(R.id.item_img)
                    val userImage = view.findViewById<View>(R.id.imageview_user)

                    val options =
                        if (this.data[position].meta_pages.size>1)
                            ActivityOptions.makeSceneTransitionAnimation(
                                context as Activity,
                                Pair.create(
                                    mainimage,
                                    "mainimage"
                                )
                            )
                        else
                            ActivityOptions.makeSceneTransitionAnimation(
                            context as Activity,
                            Pair.create(
                                mainimage,
                                "mainimage"
                            ),
                            Pair.create(userImage, "userimage")
                            )
                    ContextCompat.startActivity(context, intent, options.toBundle())
                } else
                    ContextCompat.startActivity(context, intent, null)
                true
            }
        }
        else {
            setOnItemClickListener { adapter, view, position ->
                val bundle = Bundle()
                //bundle.putLong("illustid", this.data[position].id)
                //val illustlist = LongArray(this.data.count())
                //for (i in this.data.indices) {
                //    illustlist[i] = this.data[i].id
                //}
                //bundle.putParcelable("illust", this.data[position])
                DataHolder.setIllustsList(this.data.subList(max(position-30,0), min(this.data.size,max(position-30,0)+60)))
                bundle.putInt("position",position - max(position-30,0))
                //  bundle.putParcelable(this.data[position].id.toString(), this.data[position])
                bundle.putLong("illustid", this.data[position].id)
                val intent = Intent(context, PictureActivity::class.java)
                intent.putExtras(bundle)
                if (PxEZApp.animationEnable) {
                    val mainimage = view.findViewById<View>(R.id.item_img)
                    val userImage = view.findViewById<View>(R.id.imageview_user)

                    val options = ActivityOptions.makeSceneTransitionAnimation(
                        context as Activity,
                        Pair.create(
                            mainimage,
                            "mainimage"
                        ),
                        Pair.create(userImage, "userimage")
                    )
                    ContextCompat.startActivity(context, intent, options.toBundle())
                } else
                    ContextCompat.startActivity(context, intent, null)
            }
            setOnItemLongClickListener { adapter, view, position ->
                //show detail of illust
                (adapter.data as ArrayList<Illust?>)[position]?.let {
                    val detailstring =
                        "id: " + it.id.toString() +
                                "caption: " + it.caption + "create_date: " + it.create_date +
                                "width: " + it.width.toString() + "height: " + it.height.toString() +
                                //+ "image_urls: " + illust.image_urls.toString() + "is_bookmarked: " + illust.is_bookmarked.toString() +
                                "user: " + it.user.name +
                                "tags: " + it.tags.toString() +// "title: " + illust.title.toString() +
                                "total_bookmarks: " + it.total_bookmarks.toString() +
                                "total_view: " + it.total_view.toString() +
                                "user account: " + it.user.account + "\n" +
                                "tools: " + it.tools.toString() + "\n" +
                                "type: " + it.type + "\n" +
                                "page_count: " + it.page_count.toString() + "\n" +
                                "visible: " + it.visible.toString() + "\n" +
                                "is_muted: " + it.is_muted.toString() + "\n" +
                                "sanity_level: " + it.sanity_level.toString() + "\n" +
                                "restrict: " + it.restrict.toString() + "\n" +
                                "x_restrict: " + it.x_restrict.toString()
                    MaterialAlertDialogBuilder(context as Activity)
                        .setMessage(detailstring)
                        .setTitle("Detail")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                        }
                        .create().show()
                }
                true
            }
        }*/

        val imageViewUser = helper.bd.imageviewUser
        if (item.user.is_followed) {
            imageViewUser.setBorderColor(badgeTextColor) // Color.YELLOW
            //imageViewUser.alpha = 0.9F
        }
        else {
            imageViewUser.setBorderColor(colorTransparent)//setBorderColor(colorPrimary)
            //imageViewUser.alpha = 0.5F
        }
        imageViewUser.setOnClickListener {
            val intent = Intent(context, UserMActivity::class.java)
            intent.putExtra("data", item.user.id)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (PxEZApp.animationEnable) {
                val options = ActivityOptions.makeSceneTransitionAnimation(
                    context as Activity,
                    Pair.create(imageViewUser, "UserImage")
                )
                context.startActivity(intent, options.toBundle())
            } else
                context.startActivity(intent)
        }
        imageViewUser.setOnLongClickListener {
            val id = item.user.id
            if (!item.user.is_followed) {
                retrofitRepository.postfollowUser(id, "public").subscribe({
                    item.user.is_followed = true
                    imageViewUser.setBorderColor(badgeTextColor) // Color.YELLOW
                    //imageViewUser.alpha = 0.9F
                }, {}, {})
            } else {
                retrofitRepository.postunfollowUser(id).subscribe({
                    item.user.is_followed = false
                    imageViewUser.setBorderColor(colorPrimary)
                    //imageViewUser.alpha = 0.5F
                }, {}, {}
                )
            }
            true
        }
        imageViewUser.setTag(R.id.tag_first, item.user.profile_image_urls.medium)

        GlideApp.with(imageViewUser.context).load(item.user.profile_image_urls.medium).circleCrop().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
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
