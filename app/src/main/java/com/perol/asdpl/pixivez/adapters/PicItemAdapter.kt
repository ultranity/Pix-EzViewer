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

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnLoadMoreListener
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.button.MaterialButton
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.GlideApp
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works

// basic Adapter for image item
//TODO: reuse more code
abstract class PicItemAdapter(
    layoutResId: Int,
    data: List<Illust>?
) :
    BaseQuickAdapter<Illust, BaseViewHolder>(layoutResId, data?.toMutableList()), LoadMoreModule {

    abstract var R18on: Boolean
    abstract var hideBookmarked: Int
    abstract var sortCoM: Int
    abstract var hideDownloaded: Boolean
    var colorPrimary: Int = R.color.colorPrimary
    var colorPrimaryDark: Int = R.color.colorPrimaryDark
    var badgeTextColor: Int = R.color.yellow
    abstract var blockTags: List<String>
    val retrofitRepository: RetrofitRepository = RetrofitRepository.getInstance()
    fun loadMoreEnd() {
        this.loadMoreModule?.loadMoreEnd()
    }

    fun loadMoreComplete() {
        this.loadMoreModule?.loadMoreComplete()
    }

    fun loadMoreFail() {
        this.loadMoreModule?.loadMoreFail()
    }

    fun x_restrict(item: Illust): String{
        return if (PxEZApp.R18Private && item.x_restrict == 1) {
            "private"
        } else {
            "public"
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        addFooterView(LayoutInflater.from(context).inflate(R.layout.foot_list, null))
        animationEnable = true
        setAnimationWithDefault(AnimationType.ScaleIn)
        this.loadMoreModule?.preLoadNumber = 12
        colorPrimary = ThemeUtil.getColor(context, R.attr.colorPrimary)
        colorPrimaryDark= ThemeUtil.getColor(context,R.attr.colorPrimaryDark)
        badgeTextColor= ThemeUtil.getColor(context,R.attr.badgeTextColor)
    }
    override fun convert(helper: BaseViewHolder, item: Illust) {
        if (((hideBookmarked == 1 && item.is_bookmarked) || (hideBookmarked == 3 && !item.is_bookmarked)) ||
                (sortCoM == 1 && item.type !="manga") || (sortCoM == 2 && item.type =="manga") ||
                (hideDownloaded && FileUtil.isDownloaded(item))
        ){
            helper.itemView.visibility = View.GONE
            helper.itemView.layoutParams.apply {
                height = 0
                width = 0
            }
            return
        }
        val tags = item.tags.map {
            it.name
        }
        var needBlock = false
        //if (blockTags.intersect(tags).isNotEmpty())
        for (i in blockTags) {
            if (tags.contains(i)) {
                needBlock = true
                break
            }
        }
        if (blockTags.isNotEmpty() && tags.isNotEmpty() && needBlock) {
            helper.itemView.visibility = View.GONE
            helper.itemView.layoutParams.apply {
                height = 0
                width = 0
            }
            return
        } else {
            helper.itemView.visibility = View.VISIBLE
            helper.itemView.layoutParams.apply {
                height = LinearLayout.LayoutParams.WRAP_CONTENT
                width = LinearLayout.LayoutParams.MATCH_PARENT
            }
        }
        if (PxEZApp.CollectMode == 1) {
            helper.getView<MaterialButton>(R.id.save).setOnClickListener {
                helper.setTextColor(R.id.save, colorPrimaryDark)
                Works.imageDownloadAll(item)
                if (!item.is_bookmarked) {
                    retrofitRepository.postLikeIllustWithTags(item.id, x_restrict(item), null)
                        .subscribe({
                            helper.getView<MaterialButton>(R.id.like).setTextColor(badgeTextColor)
                            item.is_bookmarked = true
                        }, {}, {})
                }
            }
        } else {
            helper.getView<MaterialButton>(R.id.save).setOnClickListener {
                helper.setTextColor(R.id.save, colorPrimaryDark)
                Works.imageDownloadAll(item)
            }
        }

        helper.setText(R.id.title, item.title)
        helper.setTextColor(
            R.id.like, if (item.is_bookmarked) {
                badgeTextColor
            } else {
                colorPrimary
            }
        )
        helper.setTextColor(
            R.id.save, if (FileUtil.isDownloaded(item)) {
                badgeTextColor
            } else {
                colorPrimary
            }
        )

        helper.getView<MaterialButton>(R.id.like).setOnClickListener { v ->
            val textView = v as Button
            if (item.is_bookmarked) {
                retrofitRepository.postUnlikeIllust(item.id).subscribe({
                    textView.setTextColor(colorPrimary)
                    item.is_bookmarked = false
                }, {}, {})
            } else {
                retrofitRepository.postLikeIllustWithTags(item.id, x_restrict(item), null).subscribe({
                    textView.setTextColor(badgeTextColor)
                    item.is_bookmarked = true
                }, {}, {})
            }
        }

        val constraintLayout =
            helper.itemView.findViewById<ConstraintLayout>(R.id.constraintLayout_num)
        when (item.type) {
            "illust" -> if (item.meta_pages.isEmpty()) {
                constraintLayout.visibility = View.INVISIBLE
            } else if (item.meta_pages.isNotEmpty()) {
                constraintLayout.visibility = View.VISIBLE
                helper.setText(R.id.textview_num, item.meta_pages.size.toString())
            }
            "ugoira" -> {
                constraintLayout.visibility = View.VISIBLE
                helper.setText(R.id.textview_num, "Gif")
            }
            else -> {
                constraintLayout.visibility = View.VISIBLE
                helper.setText(R.id.textview_num, "CoM")
            }
        }
        val mainImage = helper.getView<ImageView>(R.id.item_img)
        mainImage.setTag(R.id.tag_first, item.image_urls.medium)
        val quality =
            PreferenceManager.getDefaultSharedPreferences(context).getString("quality","0")?.toInt()?: 0
        val needSmall = if(quality == 1)
                            (item.height/item.width > 3) ||(item.width/item.height > 3)
                        else
                            item.height > 1800
        val loadUrl = if (needSmall) {
            item.image_urls.square_medium
        }
        else {
            item.image_urls.medium
        }

        if (!R18on) {
            val isr18 = tags.contains("R-18") || tags.contains("R-18G")
            if (isr18) {
                GlideApp.with(mainImage.context)
                    .load(R.drawable.h)
                    .into(mainImage)
            } else {
                GlideApp.with(mainImage.context).load(loadUrl)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .transition(withCrossFade()).placeholder(R.color.halftrans)
                    .into(object : ImageViewTarget<Drawable>(mainImage) {
                        override fun setResource(resource: Drawable?) {
                            mainImage.setImageDrawable(resource)
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            if (mainImage.getTag(R.id.tag_first) === item.image_urls.medium) {
                                super.onResourceReady(resource, transition)
                            }
                        }
                    })
            }
        }
        else {
            GlideApp.with(mainImage.context).load(loadUrl).transition(withCrossFade())
                .placeholder(R.color.halftrans)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .error(ContextCompat.getDrawable(mainImage.context, R.drawable.ai))
                .into(object : ImageViewTarget<Drawable>(mainImage) {
                    override fun setResource(resource: Drawable?) {
                        mainImage.setImageDrawable(resource)
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        if (mainImage.getTag(R.id.tag_first) === item.image_urls.medium) {
                            super.onResourceReady(resource, transition)
                        }

                    }
                })
        }
    }
    fun setOnLoadMoreListener(onLoadMoreListener: OnLoadMoreListener, recyclerView: RecyclerView?) {
        this.loadMoreModule?.setOnLoadMoreListener(onLoadMoreListener)
    }

    /*override fun addData(newData: Collection<Illust>) {
        /*super.addData(newData.mapNotNull {
            if(hideBookmarked) {
                if(it.is_bookmarked) null else it
            } else if (blockTags.isNotEmpty()) {
                if (blockTags.intersect(it.tags.map { it.name }).isNotEmpty())
                    null else it
            }
            else
                it
        })*/
        super.addData(
            newData.apply {
                if (hideBookmarked) {
                    filter {
                        !it.is_bookmarked
                    }
                }
            }.apply {
                if (blockTags.isNotEmpty()) {
                    filter{
                        !blockTags.intersect(it.tags.map { it.name }).isNotEmpty()
                    }
                }
            }
        )
    }

    override fun setNewData(newData: MutableList<Illust>?) {
        super.setNewData(
            newData?.apply {
                if (hideBookmarked) {
                    mapNotNull {
                        if(it.is_bookmarked)
                            null
                        else
                            it
                    }
                }
            }?.apply {
                if (blockTags.isNotEmpty()) {
                    mapNotNull{
                        if (blockTags.intersect(it.tags.map { it.name }).isNotEmpty())
                            null
                        else
                            it
                    }
                }
            }
        )
    }*/
    override fun addData(newData: Collection<Illust>) {
        super.addData(newData)
        DataHolder.pictureAdapter?.notifyDataSetChanged().also{
            DataHolder.pictureAdapter=null
        }
    }
}
