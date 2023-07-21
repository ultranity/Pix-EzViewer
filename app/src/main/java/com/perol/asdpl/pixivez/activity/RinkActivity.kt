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

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.services.PxEZApp

// base activity with i18n +Theme support
abstract class RinkActivity : AppCompatActivity() {
    protected var className = javaClass.simpleName + " "
    //private val Activity.simpleName get() = javaClass.simpleName

    @SuppressLint("InternalInsetResource")
    private fun getNavigationBarHeight(): Int {
        val resourceId: Int = this.resources.getIdentifier(
            "navigation_bar_height",
            "dimen",
            "android"
        )
        // Log.v("dbg", "Navi height:$height")
        return resources.getDimensionPixelSize(resourceId)
    }

    protected fun buildContainerTransform(entering: Boolean, contentTarget:Boolean=true, duration: Long=300): MaterialContainerTransform {
        val transform = MaterialContainerTransform(this, entering)
        // Use all 3 container layer colors since this transform can be configured using any fade mode
        // and some of the start views don't have a background and the end view doesn't have a
        // background.
        //transform.setAllContainerColors(
        //    MaterialColors.getColor(findViewById(R.id.content), com.google.android.material.R.attr.colorSurface)
        //)
        if (contentTarget)
            transform.addTarget(android.R.id.content)
        transform.duration = duration
        transform.interpolator = FastOutSlowInInterpolator()
        transform.pathMotion = MaterialArcMotion()
        transform.fadeMode = MaterialContainerTransform.FADE_MODE_IN
        return transform
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageUtil.setLanguage(this, PxEZApp.language)
        ThemeUtil.themeInit(this)
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        window.sharedElementsUseOverlay = false
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        if (getNavigationBarHeight() < 88) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            // window.decorView.fitsSystemWindows = true
            window.navigationBarColor = Color.TRANSPARENT
            window.statusBarColor = ThemeUtil.getColorPrimary(this)
        }
    }
}
