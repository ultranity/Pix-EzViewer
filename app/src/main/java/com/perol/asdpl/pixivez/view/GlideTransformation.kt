package com.perol.asdpl.pixivez.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class ResizeTransformation(
    val sampling: Int = DEFAULT_DOWN_SAMPLING
) :
    BitmapTransformation() {
    companion object {
        private const val DEFAULT_DOWN_SAMPLING = 1
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap? {
        if (sampling == 1) {
            return toTransform
        }
        val width = toTransform.getWidth()
        val height = toTransform.getHeight()
        val scaledWidth = width / sampling
        val scaledHeight = height / sampling

        var bitmap: Bitmap = pool.get(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)

        bitmap.density = toTransform.getDensity()

        val canvas = Canvas(bitmap)
        canvas.scale(1 / sampling.toFloat(), 1 / sampling.toFloat())
        val paint = Paint()
        paint.flags = Paint.FILTER_BITMAP_FLAG
        canvas.drawBitmap(toTransform, 0f, 0f, paint)
        return bitmap
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("ResizeTransformation$sampling".toByteArray())
    }
}