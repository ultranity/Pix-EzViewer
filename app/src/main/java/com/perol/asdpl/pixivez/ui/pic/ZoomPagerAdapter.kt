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

package com.perol.asdpl.pixivez.ui.pic

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.databinding.ViewPagerZoomBinding
import com.perol.asdpl.pixivez.networks.ProgressInterceptor
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import java.io.File

class ZoomPagerAdapter(
    val context: Context,
    val illust: Illust
) : PagerAdapter() {
    private var origin: List<String>? = null
    private var preview: List<String>? = null
    override fun getCount(): Int {
        return illust.meta.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        origin = illust.meta.map { it.original }
        preview = if (PxEZApp.instance.pre.getString("quality", "0")?.toInt() == 0) {
            illust.meta.map { it.medium }
        } else {
            illust.meta.map { it.large }
        }
        val layoutInflater = LayoutInflater.from(context)
        val binding = ViewPagerZoomBinding.inflate(layoutInflater, container, false)
        binding.photoviewZoom.isEnabled = true
        binding.photoviewZoom.setMinimumDpi(10)
        // val buttonOrigin = binding.MaterialButton.button
        // val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // val dm = DisplayMetrics()
        // wm.defaultDisplay.getMetrics(dm)
        var resourceFile: File? = null
        val gestureDetector =
            GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        /*                    if ((e2.rawY - e1.rawY) > dm.heightPixels / 3) {
                                                (context as Activity).finish()
                                            }*/
                        return false
                    }

                    override fun onLongPress(e: MotionEvent) {
                        super.onLongPress(e)
                        if (resourceFile != null) {
                            MaterialDialogs(context).show {
                                setTitle(R.string.saveselectpic1)
                                confirmButton { _, _ ->
                                    Works.imageDownloadWithFile(
                                        illust,
                                        resourceFile!!,
                                        position
                                    )
                                }
                                cancelButton()
                            }
                        }
                    }
                }
            )
        binding.photoviewZoom.setOnTouchListener { v, event ->
            return@setOnTouchListener gestureDetector.onTouchEvent(event)
        }

        fun loadFromResource(resource: File, origin: Boolean = false) {
            //CrashHandler.instance.d("origin","from cache")
            // buttonOrigin.visibility =View.GONE
            binding.photoviewZoom.setImage(ImageSource.uri(Uri.fromFile(resource)))
            resourceFile = resource
            if (origin)
                binding.progressbarOrigin.visibility = View.GONE
        }

        fun tryLoadCache() {
            Glide.with(context).asFile().load(origin!![position])
                .apply(RequestOptions().onlyRetrieveFromCache(true))
                .into(object : CustomTarget<File>() {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        Glide.with(context).asFile()
                            .load(preview!![position])
                            .apply(RequestOptions().onlyRetrieveFromCache(true))
                            .into(object : CustomTarget<File>() {
                                override fun onLoadCleared(placeholder: Drawable?) {
                                }

                                override fun onResourceReady(
                                    resource: File,
                                    transition: Transition<in File>?
                                ) {
                                    loadFromResource(resource, false)
                                }
                            })
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(
                        resource: File,
                        transition: Transition<in File>?
                    ) {
                        loadFromResource(resource, true)
                    }
                })
        }

        val file = File(Works.getDownloadPath(illust, Works.parseSaveFormat(illust, position)))
        if (file.exists()) {
            Glide.with(context).asFile()
                .load(file)
                .into(object : CustomTarget<File>() {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        tryLoadCache()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                        loadFromResource(resource, true)
                    }
                })
        } else {
            tryLoadCache()
        }
        // buttonOrigin.setOnClickListener {
        //    buttonOrigin.visibility =View.GONE
        binding.progressbarOrigin.visibility = View.VISIBLE
        ProgressInterceptor.addListener(
            origin!![position]
        ) { progress -> binding.progressbarOrigin.setProgressCompat(progress, true) }
        Glide.with(context).asFile().load(origin!![position])
            .into(object : CustomTarget<File>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                }

                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    binding.photoviewZoom.setImage(ImageSource.uri(Uri.fromFile(resource)))
                    resourceFile = resource
                    binding.progressbarOrigin.visibility = View.GONE
                    //CrashHandler.instance.d("origin","load from net")
                    ProgressInterceptor.removeListener(origin!![position])
                }
            })
        // }
        container.addView(binding.root)
        return binding.root
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}
