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

package com.perol.asdpl.pixivez.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnLoadMoreListener
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.ScreenUtil.dp2px
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.responses.UserPreviewsBean
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.NiceImageView

// TODO: fling optimize
class UserShowAdapter(layoutResId: Int) :
    BaseQuickAdapter<UserPreviewsBean, BaseViewHolder>(layoutResId), LoadMoreModule {
    companion object{
        const val itemWidth:Int = 400
        val itemWidthPx:Int = dp2px(400f)
    }
    private var mSharedPool = RecyclerView.RecycledViewPool()

    init {
        setOnItemClickListener { adapter, view, position ->
            val options = if (PxEZApp.animationEnable) {
                ActivityOptions.makeSceneTransitionAnimation(
                    context as Activity,
                    Pair(view.findViewById(R.id.imageview_usershow), "userimage")
                ).toBundle()
            } else null
            UserMActivity.start(context, this.data[position].user, options)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: UserPreviewsBean) {
        val userSearchillustAdapter: UserSearchIllustAdapter
        val recyclerView = holder.getView<RecyclerView>(R.id.recyclerview_usershow)
        if (recyclerView.adapter == null) {
            recyclerView.apply {
                userSearchillustAdapter = UserSearchIllustAdapter(R.layout.view_usersearchillust_item, item.illusts)
                adapter = userSearchillustAdapter
                layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
                setRecycledViewPool(mSharedPool)
                setHasFixedSize(true)
                isNestedScrollingEnabled = false
            }
        }
        else {
            userSearchillustAdapter = recyclerView.adapter as UserSearchIllustAdapter
            val itemCount = userSearchillustAdapter.data.size
            userSearchillustAdapter.data = item.illusts
            userSearchillustAdapter.notifyItemRangeChanged(0, itemCount)
        }
        val userImage = holder.getView<NiceImageView>(R.id.imageview_usershow)
        val username = holder.getView<TextView>(R.id.textview_usershowname)
        val colorPrimary = ThemeUtil.getColorPrimary(context)
        val badgeTextColor = ThemeUtil.getColorHighlight(context)
        if (item.user.is_followed) {
            userImage.setBorderColor(badgeTextColor) // Color.YELLOW
        }
        else {
            userImage.setBorderColor(colorPrimary)
        }
        userSearchillustAdapter.setOnItemClickListener { adapter, view, position ->
            val intent = Intent(context, PictureActivity::class.java)
            val bundle = Bundle()
            bundle.putInt("position", position)
            bundle.putLong("illustid", userSearchillustAdapter.data[position].id)
            DataHolder.setIllustsList(userSearchillustAdapter.data)
            intent.putExtras(bundle)
            val options = if (PxEZApp.animationEnable) {
                val mainimage = view.findViewById<ImageView>(R.id.imageview_usersearchillust)
                ActivityOptions.makeSceneTransitionAnimation(
                    context as Activity,
                    Pair(mainimage, "mainimage"),
                    Pair(username, "username"),
                    Pair(userImage, "userimage")
                ).toBundle()
            } else null
            ContextCompat.startActivity(context, intent, options)
        }
        username.text = "${item.user.name} : ${item.user.account}"
        Glide.with(userImage.context).load(item.user.profile_image_urls.medium).circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).transition(withCrossFade()).into(userImage)
    }


    fun loadMoreEnd() {
        this.loadMoreModule.loadMoreEnd()
    }

    fun loadMoreComplete() {
        this.loadMoreModule.loadMoreComplete()
    }

    fun loadMoreFail() {
        this.loadMoreModule.loadMoreFail()
    }

    fun setOnLoadMoreListener(onLoadMoreListener: OnLoadMoreListener) {
        this.loadMoreModule.setOnLoadMoreListener(onLoadMoreListener)
    }
}
