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

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.chad.library.adapter.base.listener.OnLoadMoreListener
import com.chad.library.adapter.base.module.LoadMoreModule
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.GlideApp
import com.perol.asdpl.pixivez.services.Works
import kotlin.reflect.KClass

// basic Adapter for image item
//TODO: reuse more code
abstract class PicItemAdapterBaseBinding<VB : ViewBinding>(
    layoutResId: Int,
    VBClass: KClass<*>,
    data: List<Illust>?
) :
    BaseBindingAdapter<Illust, VB>(layoutResId,VBClass, data?.toMutableList()), LoadMoreModule,
    IPicItemAdapter {

    var colorPrimary: Int = R.color.colorPrimary
    var colorTransparent: Int = R.color.transparent
    var colorPrimaryDark: Int = R.color.colorPrimaryDark
    var badgeTextColor: Int = R.color.yellow
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

    fun downloadAll(){
        // TODO
        /*this.data.toList().asFlow().flowOn(Dispatchers.IO)
            .filter {item->
                ((hideBookmarked == 1 && item.is_bookmarked) || (hideBookmarked == 3 && !item.is_bookmarked)) ||
                        (sortCoM == 1 && item.type != "manga") || (sortCoM == 2 && item.type == "manga") ||
                        (hideDownloaded && FileUtil.isDownloaded(item))
            }.onEach {
                Works.imageDownloadAll(it)
            }.collect()*/
        for (item in this.data.toList()) {
            if (!item.visible ||
                ((hideBookmarked == 1 && item.is_bookmarked) || (hideBookmarked == 3 && !item.is_bookmarked)) ||
                (sortCoM == 1 && item.type != "manga") || (sortCoM == 2 && item.type == "manga") ||
                (hideDownloaded && FileUtil.isDownloaded(item))
            ) { continue }
            Works.imageDownloadAll(item)
        }
    }
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        addFooterView(LayoutInflater.from(context).inflate(R.layout.foot_list, null))
        animationEnable = true
        setAnimationWithDefault(AnimationType.ScaleIn)
        this.loadMoreModule?.preLoadNumber = 12
        colorPrimary = ThemeUtil.getColor(context, androidx.appcompat.R.attr.colorPrimary)
        colorPrimaryDark= ThemeUtil.getColor(context, androidx.appcompat.R.attr.colorPrimaryDark)
        badgeTextColor= ThemeUtil.getColor(context, com.google.android.material.R.attr.badgeTextColor)
    }

    override fun convert(helper: BaseVBViewHolder<VB>, item: Illust) {
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
        if(blockTags.isNotEmpty() && tags.isNotEmpty()){
            //if (blockTags.intersect(tags).isNotEmpty())
            for (i in blockTags) {
                if (tags.contains(i)) {
                    needBlock = true
                    break
                }
            }
        }
        if (needBlock) {
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
        
        val numLayout =
            helper.itemView.findViewById<View>(R.id.layout_num)
        when (item.type) {
            "illust" -> if (item.meta_pages.isEmpty()) {
                numLayout.visibility = View.INVISIBLE
            } else if (item.meta_pages.isNotEmpty()) {
                numLayout.visibility = View.VISIBLE
                helper.setText(R.id.textview_num, item.meta_pages.size.toString())
            }
            "ugoira" -> {
                numLayout.visibility = View.VISIBLE
                helper.setText(R.id.textview_num, "Gif")
            }
            else -> {
                numLayout.visibility = View.VISIBLE
                helper.setText(R.id.textview_num, "C"+item.meta_pages.size.toString())
            }
        }
        val mainImage = helper.getView<ImageView>(R.id.item_img)
        mainImage.setTag(R.id.tag_first, item.image_urls.medium)
        val quality =
            PreferenceManager.getDefaultSharedPreferences(context).getString("quality","0")?.toInt()?: 0

        setFullSpan(helper,(1.0*item.width/item.height > 2.1))

        val needSmall = if(quality == 1)
                            (1.0*item.height/item.width > 3) //||(item.width/item.height > 3)
                        else
                            item.height > 1800
        val loadUrl = if (needSmall) {
            item.image_urls.square_medium
        }
        else {
            item.image_urls.medium
        }

        //val isr18 = tags.contains("R-18") || tags.contains("R-18G")
        if (!R18on && item.x_restrict == 1) {
                GlideApp.with(mainImage.context)
                    .load(R.drawable.h).transition(withCrossFade())
                    .placeholder(R.drawable.h)
                    .into(mainImage)
            }
        else {
            GlideApp.with(mainImage.context).load(loadUrl).transition(withCrossFade())
                .placeholder(ColorDrawable(ThemeUtil.halftrans))
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
    fun setDataVisible(){
        data.forEach{ item->
            item.visible =
                    ((hideBookmarked == 1 && item.is_bookmarked)
                    || (hideBookmarked == 3 && !item.is_bookmarked))
                        // TODO
        }
    }
    override fun addData(newData: Collection<Illust>) {
        super.addData(newData)
        DataHolder.pictureAdapter?.notifyDataSetChanged().also{
            DataHolder.pictureAdapter=null
        }
    }
}
