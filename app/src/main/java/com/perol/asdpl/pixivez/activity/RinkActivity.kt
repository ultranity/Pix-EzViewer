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

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.services.PxEZApp

// base activity with i18n +Theme support
abstract class RinkActivity : AppCompatActivity() {
    protected var className = javaClass.simpleName + " "
    private fun getColorPrimary() =
        ThemeUtil.getColor(this, androidx.appcompat.R.attr.colorPrimary)

    /*fun getColorPrimaryDark(): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true)
        return typedValue.data
    }

    fun getColorAccent(): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }*/

    fun getColorHighlight(): Int =
        ThemeUtil.getColor(this, com.google.android.material.R.attr.badgeTextColor)

    private fun getNavigationBarHeight(): Int {
        val resourceId: Int = this.resources.getIdentifier(
            "navigation_bar_height",
            "dimen",
            "android"
        )
        //Log.v("dbg", "Navi height:$height")
        return resources.getDimensionPixelSize(resourceId)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageUtil.setLanguage(this, PxEZApp.language)
        ThemeUtil.themeInit(this)
        if(getNavigationBarHeight()<88) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            //window.decorView.fitsSystemWindows = true
            window.navigationBarColor = Color.TRANSPARENT
            window.statusBarColor = getColorPrimary()
        }
    }
}



