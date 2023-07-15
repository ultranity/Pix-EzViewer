/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
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
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnLoadMoreListener
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.responses.User
import com.perol.asdpl.pixivez.services.PxEZApp

class UserListAdapter(layoutResId: Int) :
    BaseQuickAdapter<User, BaseViewHolder>(layoutResId), LoadMoreModule {

    init {
        setOnItemClickListener { adapter, view, position ->
            val options = if (PxEZApp.animationEnable) {
                ActivityOptions.makeSceneTransitionAnimation(
                    context as Activity,
                    Pair(view.findViewById(R.id.imageview_usershow), "userimage")
                ).toBundle()
            } else null
            UserMActivity.start(context, this.data[position], options)
        }
    }

    override fun convert(holder: BaseViewHolder, item: User) {
        // val linearLayoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        // val recyclerView = holder.getView<RecyclerView>(R.id.recyclerview_usershow)
        val userImage = holder.getView<View>(R.id.imageview_usershow) as ImageView
        val username = holder.getView<View>(R.id.textview_usershowname) as TextView
        // recyclerView.layoutManager = linearLayoutManager
        username.text = item.name
        Glide.with(userImage.context).load(item.profile_image_urls.medium).circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).transition(withCrossFade())
            .into(userImage)
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
