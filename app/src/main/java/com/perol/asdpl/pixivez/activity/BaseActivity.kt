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

package com.perol.asdpl.pixivez.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.services.PxEZApp

// base activity with i18n +Theme support
abstract class BaseActivity<Layout : ViewBinding?> : AppCompatActivity() {

    companion object {
        const val ASK_URI = 42
        fun newInstance(intent: Intent?, context: Context) {
            context.startActivity(intent)
        }
    }

    private var mContext: Context? = null
    private var mActivity: FragmentActivity? = null
    private var mLayoutID = 0
    protected var binding: Layout? = null
    protected var className = this.javaClass.simpleName + " "
    private fun initModel() {}
    private fun initBundle(bundle: Bundle?) {}
    protected abstract fun initLayout(): Int
    protected abstract fun initView()
    protected abstract fun initData()
    fun hideStatusBar(): Boolean {
        return false
    }

    fun gray(gray: Boolean) {
        if (gray) {
            val grayPaint = Paint()
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0.0f)
            grayPaint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            window.decorView.setLayerType(View.LAYER_TYPE_HARDWARE, grayPaint)
        }
        else {
            val normalPaint = Paint()
            window.decorView.setLayerType(View.LAYER_TYPE_HARDWARE, normalPaint)
        }
    }

    private fun getNavigationBarHeight(): Int {
        val resourceId: Int = this.resources.getIdentifier(
            "navigation_bar_height",
            "dimen",
            "android"
        )
        // Log.v("dbg", "Navi height:$height")
        return resources.getDimensionPixelSize(resourceId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            LanguageUtil.setLanguage(this, PxEZApp.language)
            ThemeUtil.themeInit(this)
            if (getNavigationBarHeight() < 88) {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                // window.decorView.fitsSystemWindows = true
                window.navigationBarColor = Color.TRANSPARENT
                window.statusBarColor = ThemeUtil.getColorPrimary(this)
            }
            mLayoutID = initLayout()
            mContext = this
            mActivity = this
            val intent = intent
            if (intent != null) {
                val bundle = intent.extras
                bundle?.let { initBundle(it) }
            }

            initModel()
            initView()
            initData()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
