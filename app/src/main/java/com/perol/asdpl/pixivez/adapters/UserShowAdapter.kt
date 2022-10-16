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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.responses.SearchUserResponse
import com.perol.asdpl.pixivez.services.GlideApp
import com.perol.asdpl.pixivez.services.PxEZApp
import com.shehuan.niv.NiceImageView


class UserShowAdapter(layoutResId: Int) :
    BaseQuickAdapter<SearchUserResponse.UserPreviewsBean, BaseViewHolder>(layoutResId),
    LoadMoreModule {


    init {
        setOnItemClickListener { adapter, view, position ->
            val intent = Intent(context, UserMActivity::class.java)
            intent.putExtra("data", this.data[position].user.id)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

            if (PxEZApp.animationEnable) {
                val options = ActivityOptions.makeSceneTransitionAnimation(
                    context as Activity,
                    Pair.create(view.findViewById(R.id.imageview_usershow), "UserImage")
                )
                context.startActivity(intent, options.toBundle())
            } else
                context.startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: SearchUserResponse.UserPreviewsBean) {
        val linearLayoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        val userSearchillustAdapter = UserSearchIllustAdapter(R.layout.view_usersearchillust_item, item.illusts)
        //        helper.addOnClickListener(R.id.cardview).addOnClickListener(R.id.imageview_usershow).addOnClickListener(R.id.textview_usershowname);
        val recyclerView = holder.getView<RecyclerView>(R.id.recyclerview_usershow)
        val userImage = holder.getView<NiceImageView>(R.id.imageview_usershow)
        val username = holder.getView<TextView>(R.id.textview_usershowname)
        val colorPrimary = ThemeUtil.getColor(context, androidx.appcompat.R.attr.colorPrimary)
        val badgeTextColor= ThemeUtil.getColor(context, com.google.android.material.R.attr.badgeTextColor)
        if (item.user.is_followed)
            userImage.setBorderColor(badgeTextColor) // Color.YELLOW
        else
            userImage.setBorderColor(colorPrimary)
        recyclerView.layoutManager = linearLayoutManager
        userSearchillustAdapter.setOnItemClickListener { adapter, view, position ->
            val bundle = Bundle()
            bundle.putInt("position", position)
            bundle.putLong("illustid", item.illusts[position].id)
            DataHolder.setIllustsList(item.illusts as ArrayList<Illust>)
            val intent = Intent(context, PictureActivity::class.java)
            intent.putExtras(bundle)
            if (PxEZApp.animationEnable) {
                val mainimage = view.findViewById<ImageView>(R.id.imageview_usersearchillust)

                val options = ActivityOptions.makeSceneTransitionAnimation(
                    context as Activity,
                    Pair.create(
                        mainimage,
                        "mainimage"
                    ),
                    Pair.create(username, "username"),
                    Pair.create(userImage, "userimage")
                )
                ContextCompat.startActivity(context, intent, options.toBundle())
            } else
                ContextCompat.startActivity(context, intent, null)
        }
        recyclerView.adapter = userSearchillustAdapter
        username.text = "${item.user.name} : ${item.user.account}"
        GlideApp.with(userImage.context).load(item.user.profile_image_urls.medium).circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).transition(withCrossFade()).into(userImage)

    }
}
