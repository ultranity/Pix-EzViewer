package com.perol.asdpl.pixivez.objects

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html.ImageGetter
import android.util.Log
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference

/**
 * Assets Image Getter using AssetManager
 * Load image from assets folder
 * @author [Daniel Passos](mailto:daniel@passos.me)
 */
class HtmlAssetsImageGetter(context: Context, private val folder: String = "") : ImageGetter {
    private val mContext: Context = context

    override fun getDrawable(source: String): Drawable? {
        return try {
            /*Glide.with(mContext).load(Uri.parse("file:///android_asset/$folder/$source"))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .submit().get().also{
                    it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight) //54? 56?
                }*/
            val inputStream: InputStream = mContext.assets.open("$folder/$source")
            val d = Drawable.createFromStream(inputStream, null)
            d!!.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight) // 54? 56?
            d
        } catch (e: IOException) {
            // prevent a crash if the resource still can't be found
            Log.e("HtmlTextView", "source could not be found: $source")
            null
        }
    }
}

/**
 * Assets Image Getter using Glide for bitmap reuse
 * Load image from assets folder by Uri parsing
 */
class GlideAssetsImageGetter(textView: TextView, private val folder: String = "") : GlideImageGetter(textView) {
    override fun getDrawable(source: String): Drawable {
        val drawable = BitmapDrawablePlaceholder()
        container.get()?.apply {
            post {
                Glide.with(context)
                    .asBitmap()
                    .load(Uri.parse("file:///android_asset/" + (if (folder.isBlank()) "" else "$folder/") + source))
                    .into(drawable)
            }
        }
        return drawable
    }
}

/* https://gist.github.com/Jessenw/044c81a3c72f02868c8826094007b269
 * Based on code by https://github.com/ddekanski and https://gist.github.com/yrajabi
 * See: https://gist.github.com/yrajabi/5776f4ade5695009f87ce7fcbc08078f
 */
open class GlideImageGetter(textView: TextView) : ImageGetter {

    val container: WeakReference<TextView>

    init {
        container = WeakReference(textView)
    }

    override fun getDrawable(source: String): Drawable {
        val drawable = BitmapDrawablePlaceholder()
        container.get()?.apply {
            post {
                Glide.with(container.get()!!.context)
                    .asBitmap()
                    .load(source)
                    .into(drawable)
            }
        }
        return drawable
    }

    inner class BitmapDrawablePlaceholder : BitmapDrawable(
            container.get()?.resources,
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        ), Target<Bitmap> {

        var drawable: Drawable? = null
            set(value) {
                field = value
                value?.let { drawable ->
                    val drawableWidth = drawable.intrinsicWidth
                    val drawableHeight = drawable.intrinsicHeight

                    val maxWidth = container.get()!!.measuredWidth
                    if (drawableWidth > maxWidth) {
                        val calculatedHeight = maxWidth * drawableHeight / drawableWidth
                        drawable.setBounds(0, 0, maxWidth, calculatedHeight)
                        setBounds(0, 0, maxWidth, calculatedHeight)
                    }
                    else {
                        drawable.setBounds(0, 0, drawableWidth, drawableHeight)
                        setBounds(0, 0, drawableWidth, drawableHeight)
                    }

                    container.get()?.let { it.text = it.text }
                }
            }

        override fun draw(canvas: Canvas) {
            drawable?.draw(canvas)
        }

        override fun onLoadStarted(placeholderDrawable: Drawable?) {
            placeholderDrawable?.let { drawable = it }
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            errorDrawable?.let { drawable = it }
        }

        override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
            drawable = BitmapDrawable(container.get()!!.resources, bitmap)
        }

        override fun onLoadCleared(placeholderDrawable: Drawable?) {
            placeholderDrawable?.let { drawable = it }
        }

        override fun getSize(cb: SizeReadyCallback) {
            cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
        }

        override fun removeCallback(cb: SizeReadyCallback) {}
        override fun setRequest(request: Request?) {}
        override fun getRequest(): Request? = null

        override fun onStart() {}
        override fun onStop() {}
        override fun onDestroy() {}
    }
}
