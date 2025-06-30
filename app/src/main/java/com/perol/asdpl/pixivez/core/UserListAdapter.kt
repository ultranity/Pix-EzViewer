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

package com.perol.asdpl.pixivez.core

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.util.Pair
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.chad.brvah.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.LBaseQuickAdapter
import com.perol.asdpl.pixivez.base.SMutableLiveData
import com.perol.asdpl.pixivez.data.model.User
import com.perol.asdpl.pixivez.data.model.UserPreviewsBean
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.dp
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import com.perol.asdpl.pixivez.ui.pic.SquareMediumAdapter
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import com.perol.asdpl.pixivez.view.NiceImageView
import kotlin.properties.Delegates

// TODO: fling optimize
class UserListAdapter(layoutResId: Int) :
    LBaseQuickAdapter<UserPreviewsBean, BaseViewHolder>(layoutResId) {
    companion object {
        const val ITEM_WIDTH: Int = 400
        val itemWidthPx: Int = 400.dp
    }

    private var colorPrimary by Delegates.notNull<Int>()
    private var badgeTextColor by Delegates.notNull<Int>()

    private var mSharedPool = RecyclerView.RecycledViewPool()
    private val tempLiveData = SMutableLiveData(false)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        colorPrimary = ThemeUtil.getColorPrimary(context)
        badgeTextColor = ThemeUtil.getColorHighlight(context)
        setOnItemClickListener { adapter, view, position ->
            val user = this.data[position].user
            addUserBinder(view, user)
            val options = if (PxEZApp.animationEnable) {
                ActivityOptions.makeSceneTransitionAnimation(
                    context as Activity,
                    Pair(view, "shared_element_container")
                    //Pair(view.findViewById(R.id.imageview_usershow), "userimage")
                ).toBundle()
            } else null
            UserMActivity.start(context, user, options)
        }
    }
    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: UserPreviewsBean) {
        if (item.user.is_blocked == true) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams.apply { width = 0; height = 0 }
            return
        }
        val recyclerview = holder.getView<RecyclerView>(R.id.recyclerview_usershow)
        if (item.illusts.isNotEmpty()) {
            val userShowIllustAdapter: SquareMediumAdapter
            if (recyclerview.adapter == null) {
                userShowIllustAdapter =
                    SquareMediumAdapter(R.layout.view_user_illust_item, item.illusts)
                recyclerview.apply {
                    adapter = userShowIllustAdapter
                    layoutManager = LinearLayoutManager(
                        holder.itemView.context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    setRecycledViewPool(mSharedPool)
                    setHasFixedSize(true)
                    isNestedScrollingEnabled = false
                }
            } else {
                userShowIllustAdapter = recyclerview.adapter as SquareMediumAdapter
                val itemCount = userShowIllustAdapter.data.size
                userShowIllustAdapter.setNewInstance(item.illusts)
                userShowIllustAdapter.notifyItemRangeChanged(0, itemCount)
            }

            userShowIllustAdapter.setOnItemClickListener { adapter, view, position ->
                val user = item.user
                addUserBinder(holder.itemView, user)
                DataHolder.setIllustList(userShowIllustAdapter.data)
                val options = if (PxEZApp.animationEnable) {
                    val mainimage = view.findViewById<ImageView>(R.id.imageview)
                    ActivityOptions.makeSceneTransitionAnimation(
                        context as Activity,
                        Pair(mainimage, "mainimage"),
                        //Pair(username, "username"),
                        //Pair(userImage, "userimage")
                    ).toBundle()
                } else null
                PictureActivity.start(
                    context,
                    userShowIllustAdapter.data[position].id,
                    position,
                    squareThumbHint = true,
                    options = options
                )
            }
        }
        val userImage = holder.getView<NiceImageView>(R.id.imageview_usershow)
        userImage.setBorderColor(if (item.user.is_followed) badgeTextColor else colorPrimary)
        val username = holder.getView<TextView>(R.id.textview_usershowname)
        username.text = "${item.user.name} : ${item.user.account}"
        Glide.with(userImage.context).load(item.user.profile_image_urls.medium)
            .circleCrop().transition(withCrossFade())
            .into(userImage)
    }

    private fun addUserBinder(rootView: View, user: User) {
        tempLiveData.overrideValue(user.is_followed)
        tempLiveData.observeAfterSet(recyclerView.context as LifecycleOwner) {
            //if (it==tempLiveData.lastValue)
            //    return@observeAfterSet
            rootView.findViewById<NiceImageView>(R.id.imageview_usershow)
                .setBorderColor(if (user.is_followed) badgeTextColor else colorPrimary)
        }
        user.addBinder(user.account, tempLiveData)
    }
}
