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

package com.perol.asdpl.pixivez.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.view.LayoutInflater
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.objects.dp
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun resourceIdToUri(context: Context, resourceId: Int): String =
    "android.resource://${context.packageName}/$resourceId"

//@BindingAdapter("userUrl")
fun loadUserImage(imageView: ImageView, url: String?) {
    if ((url == null) or url.contentEquals("https://source.pixiv.net/common/images/no_profile.png")) {
        Glide.with(imageView.context).load(R.mipmap.ic_noimage_foreground).circleCrop()
            .transition(withCrossFade()).into(imageView)
        (imageView.context as FragmentActivity).supportStartPostponedEnterTransition()
    } else {
        Glide.with(imageView.context)
            .load(url)
            .placeholder(R.mipmap.ic_noimage_round)
            .circleCrop()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
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
            .transition(withCrossFade()).into(imageView)
    }
}

//@BindingAdapter("url")
fun loadBGImage(imageView: ImageView, url: String?) {
    if (url != null) {
        imageView.setOnClickListener {
            MaterialDialogs(imageView.context).show {
                setTitle(url)
                val view = LayoutInflater.from(context).inflate(R.layout.view_history_item, null)
                val mainImage = view.findViewById<ImageView>(R.id.item_img)!!
                mainImage.minimumHeight = 200.dp
                Glide.with(mainImage).load(url)
                    .into(mainImage)
                setView(view)
                setPositiveButton(R.string.download) { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val f = Glide.with(imageView).asFile()
                            .load(url)
                            .submit()
                        val file = f.get()
                        val target = File(
                            PxEZApp.storepath,
                            "user_${url.substringAfterLast("/")}"
                        )
                        file.copyTo(target, overwrite = true)
                        MediaScannerConnection.scanFile(
                            PxEZApp.instance,
                            arrayOf(target.path),
                            arrayOf(
                                MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                    target.extension
                                )
                            )
                        ) { _, _ -> }

                        withContext(Dispatchers.Main) {
                            Toasty.info(imageView.context, "Saved")
                        }
                    }
                }
            }
        }
        Glide.with(imageView.context).load(url)
            .transition(withCrossFade())
            .placeholder(ColorDrawable(ThemeUtil.getColorPrimary(imageView.context)))
            .error(ColorDrawable(ThemeUtil.getColorPrimary(imageView.context)))
            .into(object : ImageViewTarget<Drawable>(imageView) {
                override fun setResource(resource: Drawable?) {
                    imageView.setImageDrawable(resource)
                }
            })
    } else {
        Glide.with(imageView.context)
            .load(ColorDrawable(ThemeUtil.getColorPrimary(imageView.context)))
            .into(imageView)
    }
}
