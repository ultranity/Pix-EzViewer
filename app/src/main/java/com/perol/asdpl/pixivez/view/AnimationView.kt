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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.math.max

// Pixiv动图的帧动画解决方案，如果需要拿去用把Copyright带上或者提一下我的id吧，研究了挺久的
open class AnimationView : SurfaceView, SurfaceHolder.Callback, Runnable {

    private val drawPool: MutableList<String> = ArrayList()
    private val handlerThread = HandlerThread("UgoiraView")
    private var needToDraw = false
    private var isSurfaceCreated = false

    private var painterHandler: Handler? = null
    var delayTime: Long = 50
    private var loadingZIP = false

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    protected fun init() {
        holder?.addCallback(this)
        holder?.setFormat(PixelFormat.TRANSLUCENT)
        handlerThread.start()
    }

    fun pausePlay() {
        needToDraw = false
        painterHandler?.removeCallbacks(this)
        painterHandler = null
    }

    fun startPlay() {
        if (drawPool.isEmpty()) return
        needToDraw = true
        painterHandler = Handler(handlerThread.looper)
        painterHandler?.post(this)
    }

    fun startAnimation(
        pathListRGB: List<String>
    ) {
        drawPool.clear()
        loadingZIP = false
        drawPool.addAll(pathListRGB)
        painterHandler = Handler(handlerThread.looper)
        painterHandler?.post(this)
    }

    fun startAnimation(pathZipRGB: String) {
        drawPool.clear()
        loadingZIP = true
        drawPool.add(pathZipRGB)
        painterHandler = Handler(handlerThread.looper)
        painterHandler?.post(this)
    }

    lateinit var onStart: () -> Unit
    lateinit var onEnd: () -> Unit
    fun onStartListener(listener: () -> Unit) {
        this.onStart = listener
    }

    fun onEndListener(listener: () -> Unit) {
        this.onEnd = listener
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceCreated = true
        needToDraw = true
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        needToDraw = false
    }

    fun setPreviewImage(bitmap: Bitmap) {
        val canvas = holder.lockCanvas()
        if (canvas != null) {
            bitmap.let {
                //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                val react = RectF(
                    this@AnimationView.left.toFloat(),
                    this@AnimationView.top.toFloat(),
                    this@AnimationView.width.toFloat(),
                    this@AnimationView.height.toFloat()
                )
                canvas.drawBitmap(it, null, react, null)
            }
            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun run() {
        post { onStart() }

        val iter = loadBitSequence().iterator()
        while (true) {
            if (needToDraw) {
                try {
                    val preTime = System.currentTimeMillis()
                    draw(iter)
                    val duration = System.currentTimeMillis() - preTime
                    Thread.sleep(
                        max(
                            0,
                            delayTime - duration
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            } else {
                break
            }
        }

        post {
            onEnd()
        }
    }
    var targetBitmap: Bitmap? = null
    fun loadZipAsSeq(zipFile: File, cyclic: Boolean = false): Sequence<Bitmap> {
        val zipInputStream = ZipInputStream(zipFile.inputStream())
        val entries = mutableListOf<ByteArray>()
        var entry: ZipEntry?
        var bitmap: Bitmap? = null
        return sequence {
            while (zipInputStream.nextEntry.also { entry = it } != null) {
                if (!entry!!.isDirectory) {
                    entries.add(zipInputStream.readBytes())
                }
                zipInputStream.closeEntry()
            }
            zipInputStream.close()
            while (true) {
                entries.forEach {
                    bitmap = BitmapFactory.decodeByteArray(
                        it,
                        0,
                        it.size,
                        BitmapFactory.Options().apply {
                            inMutable = true
                            inBitmap = bitmap
                        })
                    yield(bitmap!!)
                }
                if (!cyclic) break
            }
        }
    }

    private fun loadBitSequence(): Sequence<Bitmap?> {
        if (loadingZIP)
            return loadZipAsSeq(File(drawPool[0]), true)
        return sequence {
            while (true) {
                for (path in drawPool) {
                    yield(
                        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
                            inMutable = true
                            inBitmap = targetBitmap
                        })
                    )
                }
            }
        }
    }

    private fun draw(bitmapIter: Iterator<Bitmap?>) {
        val canvas = holder.lockCanvas()
        if (canvas != null) {
            targetBitmap = bitmapIter.next()
            targetBitmap?.let {
                //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                val react = RectF(
                    this@AnimationView.left.toFloat(),
                    this@AnimationView.top.toFloat(),
                    this@AnimationView.width.toFloat(),
                    this@AnimationView.height.toFloat()
                )
                canvas.drawBitmap(it, null, react, null)
            }
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun draw(path: String) {
        val canvas = holder.lockCanvas()
        if (canvas != null) {
            targetBitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
                inMutable = true
                inBitmap = targetBitmap
            })
            targetBitmap?.let {
                //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                val react = RectF(
                    this@AnimationView.left.toFloat(),
                    this@AnimationView.top.toFloat(),
                    this@AnimationView.width.toFloat(),
                    this@AnimationView.height.toFloat()
                )
                canvas.drawBitmap(it, null, react, null)
            }
            holder.unlockCanvasAndPost(canvas)
        }
    }
}