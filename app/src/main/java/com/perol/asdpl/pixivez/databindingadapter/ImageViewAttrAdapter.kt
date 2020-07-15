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

package com.perol.asdpl.pixivez.databindingadapter

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.services.GlideApp

@BindingAdapter("userUrl")
fun loadImage(imageView: ImageView, url: String?) {
    if (url != null)

            GlideApp.with(imageView.context)
                .load(if (url.contentEquals("https://source.pixiv.net/common/images/no_profile.png"))
                            GlideApp.with(imageView.context).load(R.mipmap.ic_noimage_round).circleCrop().transition(withCrossFade()).into(imageView)
                      else
                            url)
                .circleCrop()
                .placeholder(R.drawable.black_background)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        (imageView.context as FragmentActivity).supportStartPostponedEnterTransition()
                        return false
                    }
                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        (imageView.context as FragmentActivity).supportStartPostponedEnterTransition()
                        return false
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.mipmap.ic_noimage_round)
                .transition(withCrossFade()).into(imageView)
}

@BindingAdapter("url")
fun GlideLoadImage(imageView: ImageView, url: String?) {
    if (url != null)
        GlideApp.with(imageView.context).load(url).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).transition(withCrossFade()).error(R.drawable.chobi01).placeholder(R.drawable.chobi01).into(imageView)
    else {
        GlideApp.with(imageView).load(R.drawable.chobi01).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).transition(withCrossFade()).into(imageView)
    }
}







