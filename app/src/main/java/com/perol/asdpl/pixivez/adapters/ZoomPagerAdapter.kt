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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.DisplayMetrics
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.dinuscxj.progressbar.CircleProgressBar
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.networks.ProgressInterceptor
import com.perol.asdpl.pixivez.networks.ProgressListener
import com.perol.asdpl.pixivez.responses.Illust
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
        return if (illust.meta_pages.isEmpty()) {
            1
        }
        else {
            illust.meta_pages.size
        }
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val large = PxEZApp.instance.pre.getString(
            "quality",
            "0"
        )!!.toInt() != 0
        if (illust.meta_pages.isEmpty()) {
            origin = listOf(illust.meta_single_page.original_image_url!!)
            preview = if (large) {
                listOf(illust.image_urls.large)
            } 
            else {
                listOf(illust.image_urls.medium)
            }
        } 
        else {
            origin = illust.meta_pages.map { it.image_urls.original }
            preview = if (large) {
                illust.meta_pages.map { it.image_urls.large }
            } 
            else {
                illust.meta_pages.map { it.image_urls.medium }
            }
        }
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.view_pager_zoom, container, false)
        val photoView = view.findViewById<SubsamplingScaleImageView>(R.id.photoview_zoom)
        photoView.isEnabled = true
        val progressBar = view.findViewById<CircleProgressBar>(R.id.progressbar_origin)
        // val buttonOrigin = view.findViewById<MaterialButton>(R.id.button_origin)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getMetrics(dm)
        var resourceFile: File? = null
        val gestureDetector =
            GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(
                        e1: MotionEvent,
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
                            MaterialDialog(context).show {
                                title(R.string.saveselectpic1)
                                positiveButton(android.R.string.ok) {
                                    if (illust.meta_pages.isNotEmpty()) {
                                        Works.imageDownloadWithFile(illust, resourceFile!!, position)
                                    }
                                    else {
                                        Works.imageDownloadWithFile(illust, resourceFile!!, null)
                                    }
                                }
                                negativeButton(android.R.string.cancel)
                                lifecycleOwner((this@ZoomPagerAdapter.context as AppCompatActivity))
                            }
                        }
                    }
                }
            )
        photoView.setOnTouchListener { v, event ->
            return@setOnTouchListener gestureDetector.onTouchEvent(event)
        }

        Glide.with(context).asFile().load(origin!![position]).apply(RequestOptions().onlyRetrieveFromCache(true))
            .into(object : CustomTarget<File>() {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    // super.onLoadFailed(errorDrawable)
                    Glide.with(context).asFile()
                        .load(preview!![position])
                        .apply(RequestOptions().onlyRetrieveFromCache(true))
                        .into(object : CustomTarget<File>() {
                            override fun onLoadCleared(placeholder: Drawable?) {
                            }
                            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                                // Log.d("origin","load preview")
                                photoView.setImage(ImageSource.uri(Uri.fromFile(resource)))
                                resourceFile = resource
                            }
                        })
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                }
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    // Log.d("origin","from cache")
                    // buttonOrigin.visibility =View.GONE
                    photoView.setImage(ImageSource.uri(Uri.fromFile(resource)))
                    resourceFile = resource
                    progressBar.visibility = View.GONE
                }
            })
        // buttonOrigin.setOnClickListener {
        //    buttonOrigin.visibility =View.GONE
        progressBar.visibility = View.VISIBLE
        ProgressInterceptor.addListener(
            origin!![position],
            object : ProgressListener {
                override fun onProgress(progress: Int) {
                    progressBar.progress = progress
                }
            }
        )
        Glide.with(context).asFile().load(origin!![position])
            .into(object : CustomTarget<File>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                }
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    photoView.setImage(ImageSource.uri(Uri.fromFile(resource)))
                    resourceFile = resource
                    progressBar.visibility = View.GONE
                    // Log.d("origin","load from net")
                    ProgressInterceptor.removeListener(origin!![position])
                }
            })
        // }
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}
