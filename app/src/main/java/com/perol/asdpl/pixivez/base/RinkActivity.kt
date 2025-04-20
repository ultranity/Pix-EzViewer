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

package com.perol.asdpl.pixivez.base

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            return window.decorView.rootWindowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
//        }
        val resourceId: Int = this.resources.getIdentifier(
            "navigation_bar_height",
            "dimen",
            "android"
        )
        // CrashHandler.instance.d("dbg", "Navi height:$height")
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
        // Enable Activity Transitions. Optionally enable Activity transitions in your
        // theme with <item name=”android:windowActivityTransitions”>true</item>.
        //window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        // set false to kKeep system bars (status bar, navigation bar) persistent throughout the transition.
        //window.sharedElementsUseOverlay = false
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        findViewById<View>(android.R.id.content).transitionName = "shared_element_container"
        // Set this Activity’s enter and return transition to a MaterialContainerTransform
        window.sharedElementReturnTransition = MaterialContainerTransform().apply {
            addTarget(android.R.id.content)
            duration = 250L
        }
        super.onCreate(savedInstanceState)
        LanguageUtil.setLocale(this, PxEZApp.locale)
        ThemeUtil.themeInit(this)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.statusBars())
            isAppearanceLightStatusBars = !ThemeUtil.isDarkMode(this@RinkActivity)
            isAppearanceLightNavigationBars = !ThemeUtil.isDarkMode(this@RinkActivity)
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        }
        window.statusBarColor = ThemeUtil.getColorPrimary(this)
        if ((getNavigationBarHeight() < 88) or !PxEZApp.instance.pre.getBoolean(
                "bottomAppbar",
                true
            )
        ) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            // window.decorView.fitsSystemWindows = true
            window.navigationBarColor = resources.getColor(android.R.color.transparent)
        } else {
            window.navigationBarColor = ThemeUtil.getColorPrimary(this)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
}
