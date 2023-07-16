package com.perol.asdpl.pixivez.ui

/*
 * Modified to Use AndroidX and Kotlin  by ultranity
 *  Copyright (C) 2018 shehuan https://github.com/shehuan/NiceImageView
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Xfermode
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.objects.ScreenUtil

//src: https://github.com/shehuan/NiceImageView/blob/master/niceimageview/src/main/java/com/shehuan/niv/NiceImageView.java

class NiceImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    AppCompatImageView(context, attrs, defStyleAttr) {
    //val mContext = context
    private var isCircle // 是否显示为圆形，如果为圆形则设置的corner无效
            = false
    private var isCoverSrc // border、inner_border是否覆盖图片
            = false
    private var borderWidth // 边框宽度
            = 0
    private var borderColor = Color.WHITE // 边框颜色
    private var innerBorderWidth // 内层边框宽度
            = 0
    private var innerBorderColor = Color.WHITE // 内层边框充色
    private var cornerRadius // 统一设置圆角半径，优先级高于单独设置每个角的半径
            = 0
    private var cornerTopLeftRadius // 左上角圆角半径
            = 0
    private var cornerTopRightRadius // 右上角圆角半径
            = 0
    private var cornerBottomLeftRadius // 左下角圆角半径
            = 0
    private var cornerBottomRightRadius // 右下角圆角半径
            = 0
    private var maskColor // 遮罩颜色
            = 0
    private var xfermode: Xfermode? = null
    private var width = 0
    private var height = 0
    private var radius = 0f
    private val borderRadii: FloatArray
    private val srcRadii: FloatArray
    private var srcRectF // 图片占的矩形区域
            : RectF

    // 边框的矩形区域
    private val borderRectF: RectF
    private val paint: Paint
    private val path // 用来裁剪图片的ptah
            : Path
    private var srcPath // 图片区域大小的path
            : Path? = null

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.NiceImageView, 0, 0)
        for (i in 0 until ta.indexCount) {
            when (val attr = ta.getIndex(i)) {
                R.styleable.NiceImageView_is_cover_src -> {
                    isCoverSrc = ta.getBoolean(attr, isCoverSrc)
                }
                R.styleable.NiceImageView_is_circle -> {
                    isCircle = ta.getBoolean(attr, isCircle)
                }
                R.styleable.NiceImageView_border_width -> {
                    borderWidth = ta.getDimensionPixelSize(attr, borderWidth)
                }
                R.styleable.NiceImageView_border_color -> {
                    borderColor = ta.getColor(attr, borderColor)
                }
                R.styleable.NiceImageView_inner_border_width -> {
                    innerBorderWidth = ta.getDimensionPixelSize(attr, innerBorderWidth)
                }
                R.styleable.NiceImageView_inner_border_color -> {
                    innerBorderColor = ta.getColor(attr, innerBorderColor)
                }
                R.styleable.NiceImageView_corner_radius -> {
                    cornerRadius = ta.getDimensionPixelSize(attr, cornerRadius)
                }
                R.styleable.NiceImageView_corner_top_left_radius -> {
                    cornerTopLeftRadius = ta.getDimensionPixelSize(attr, cornerTopLeftRadius)
                }
                R.styleable.NiceImageView_corner_top_right_radius -> {
                    cornerTopRightRadius = ta.getDimensionPixelSize(attr, cornerTopRightRadius)
                }
                R.styleable.NiceImageView_corner_bottom_left_radius -> {
                    cornerBottomLeftRadius = ta.getDimensionPixelSize(attr, cornerBottomLeftRadius)
                }
                R.styleable.NiceImageView_corner_bottom_right_radius -> {
                    cornerBottomRightRadius = ta.getDimensionPixelSize(attr, cornerBottomRightRadius)
                }
                R.styleable.NiceImageView_mask_color -> {
                    maskColor = ta.getColor(attr, maskColor)
                }
            }
        }
        ta.recycle()
        borderRadii = FloatArray(8)
        srcRadii = FloatArray(8)
        borderRectF = RectF()
        srcRectF = RectF()
        paint = Paint()
        path = Path()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        } else {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            srcPath = Path()
        }
        calculateRadii()
        clearInnerBorderWidth()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        width = w
        height = h
        initBorderRectF()
        initSrcRectF()
    }

    override fun onDraw(canvas: Canvas) {
        // 使用图形混合模式来显示指定区域的图片
        canvas.saveLayer(srcRectF, null, Canvas.ALL_SAVE_FLAG)
        if (!isCoverSrc) {
            val sx = 1.0f * (width - 2 * borderWidth - 2 * innerBorderWidth) / width
            val sy = 1.0f * (height - 2 * borderWidth - 2 * innerBorderWidth) / height
            // 缩小画布，使图片内容不被borders覆盖
            canvas.scale(sx, sy, width / 2.0f, height / 2.0f)
        }
        super.onDraw(canvas)
        paint.reset()
        path.reset()
        if (isCircle) {
            path.addCircle(width / 2.0f, height / 2.0f, radius, Path.Direction.CCW)
        } else {
            path.addRoundRect(srcRectF, srcRadii, Path.Direction.CCW)
        }
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.xfermode = xfermode
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            canvas.drawPath(path, paint)
        } else {
            srcPath!!.addRect(srcRectF, Path.Direction.CCW)
            // 计算tempPath和path的差集
            srcPath!!.op(path, Path.Op.DIFFERENCE)
            canvas.drawPath(srcPath!!, paint)
        }
        paint.xfermode = null

        // 绘制遮罩
        if (maskColor != 0) {
            paint.color = maskColor
            canvas.drawPath(path, paint)
        }
        // 恢复画布
        canvas.restore()
        // 绘制边框
        drawBorders(canvas)
    }

    private fun drawBorders(canvas: Canvas) {
        if (isCircle) {
            if (borderWidth > 0) {
                drawCircleBorder(canvas, borderWidth, borderColor, radius - borderWidth / 2.0f)
            }
            if (innerBorderWidth > 0) {
                drawCircleBorder(
                    canvas, innerBorderWidth, innerBorderColor,
                    radius - borderWidth - innerBorderWidth / 2.0f
                )
            }
        } else {
            if (borderWidth > 0) {
                drawRectFBorder(canvas, borderWidth, borderColor, borderRectF, borderRadii)
            }
        }
    }

    private fun drawCircleBorder(
        canvas: Canvas,
        borderWidth: Int,
        borderColor: Int,
        radius: Float
    ) {
        initBorderPaint(borderWidth, borderColor)
        path.addCircle(width / 2.0f, height / 2.0f, radius, Path.Direction.CCW)
        canvas.drawPath(path, paint)
    }

    private fun drawRectFBorder(
        canvas: Canvas,
        borderWidth: Int,
        borderColor: Int,
        rectF: RectF,
        radii: FloatArray
    ) {
        initBorderPaint(borderWidth, borderColor)
        path.addRoundRect(rectF, radii, Path.Direction.CCW)
        canvas.drawPath(path, paint)
    }

    private fun initBorderPaint(borderWidth: Int, borderColor: Int) {
        path.reset()
        paint.strokeWidth = borderWidth.toFloat()
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
    }

    /**
     * 计算外边框的RectF
     */
    private fun initBorderRectF() {
        if (!isCircle) {
            borderRectF[borderWidth / 2.0f, borderWidth / 2.0f, width - borderWidth / 2.0f] =
                height - borderWidth / 2.0f
        }
    }

    /**
     * 计算图片原始区域的RectF
     */
    private fun initSrcRectF() {
        if (isCircle) {
            radius = width.coerceAtMost(height) / 2.0f
            srcRectF[width / 2.0f - radius, height / 2.0f - radius, width / 2.0f + radius] =
                height / 2.0f + radius
        } else {
            srcRectF[0f, 0f, width.toFloat()] = height.toFloat()
            if (isCoverSrc) {
                srcRectF = borderRectF
            }
        }
    }

    /**
     * 计算RectF的圆角半径
     */
    private fun calculateRadii() {
        if (isCircle) {
            return
        }
        if (cornerRadius > 0) {
            for (i in borderRadii.indices) {
                borderRadii[i] = cornerRadius.toFloat()
                srcRadii[i] = cornerRadius - borderWidth / 2.0f
            }
        } else {
            borderRadii[1] = cornerTopLeftRadius.toFloat()
            borderRadii[0] = borderRadii[1]
            borderRadii[3] = cornerTopRightRadius.toFloat()
            borderRadii[2] = borderRadii[3]
            borderRadii[5] = cornerBottomRightRadius.toFloat()
            borderRadii[4] = borderRadii[5]
            borderRadii[7] = cornerBottomLeftRadius.toFloat()
            borderRadii[6] = borderRadii[7]
            srcRadii[1] = cornerTopLeftRadius - borderWidth / 2.0f
            srcRadii[0] = srcRadii[1]
            srcRadii[3] = cornerTopRightRadius - borderWidth / 2.0f
            srcRadii[2] = srcRadii[3]
            srcRadii[5] = cornerBottomRightRadius - borderWidth / 2.0f
            srcRadii[4] = srcRadii[5]
            srcRadii[7] = cornerBottomLeftRadius - borderWidth / 2.0f
            srcRadii[6] = srcRadii[7]
        }
    }

    private fun calculateRadiiAndRectF(reset: Boolean) {
        if (reset) {
            cornerRadius = 0
        }
        calculateRadii()
        initBorderRectF()
        invalidate()
    }

    /**
     * 目前圆角矩形情况下不支持inner_border，需要将其置0
     */
    private fun clearInnerBorderWidth() {
        if (!isCircle) {
            innerBorderWidth = 0
        }
    }

    fun isCoverSrc(isCoverSrc: Boolean) {
        this.isCoverSrc = isCoverSrc
        initSrcRectF()
        invalidate()
    }

    fun isCircle(isCircle: Boolean) {
        this.isCircle = isCircle
        clearInnerBorderWidth()
        initSrcRectF()
        invalidate()
    }

    fun setBorderWidth(borderWidth: Int) {
        this.borderWidth = ScreenUtil.dp2px(borderWidth)
        calculateRadiiAndRectF(false)
    }

    fun setBorderColor(@ColorInt borderColor: Int) {
        this.borderColor = borderColor
        invalidate()
    }

    fun setInnerBorderWidth(innerBorderWidth: Int) {
        this.innerBorderWidth = ScreenUtil.dp2px(innerBorderWidth)
        clearInnerBorderWidth()
        invalidate()
    }

    fun setInnerBorderColor(@ColorInt innerBorderColor: Int) {
        this.innerBorderColor = innerBorderColor
        invalidate()
    }

    fun setCornerRadius(cornerRadius: Int) {
        this.cornerRadius = ScreenUtil.dp2px(cornerRadius)
        calculateRadiiAndRectF(false)
    }

    fun setCornerTopLeftRadius(cornerTopLeftRadius: Int) {
        this.cornerTopLeftRadius = ScreenUtil.dp2px(cornerTopLeftRadius)
        calculateRadiiAndRectF(true)
    }

    fun setCornerTopRightRadius(cornerTopRightRadius: Int) {
        this.cornerTopRightRadius = ScreenUtil.dp2px(cornerTopRightRadius)
        calculateRadiiAndRectF(true)
    }

    fun setCornerBottomLeftRadius(cornerBottomLeftRadius: Int) {
        this.cornerBottomLeftRadius = ScreenUtil.dp2px(cornerBottomLeftRadius)
        calculateRadiiAndRectF(true)
    }

    fun setCornerBottomRightRadius(cornerBottomRightRadius: Int) {
        this.cornerBottomRightRadius = ScreenUtil.dp2px(cornerBottomRightRadius)
        calculateRadiiAndRectF(true)
    }

    fun setMaskColor(@ColorInt maskColor: Int) {
        this.maskColor = maskColor
        invalidate()
    }
}