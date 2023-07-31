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

package com.perol.asdpl.pixivez.objects

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.preference.Preference
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.GridItem
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.color.DynamicColors
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.settings.ThemeFragment
import com.perol.asdpl.pixivez.view.applySeedColorToActivityIfAvailable


object ThemeUtil {
    val colorThemeArray = arrayOf(
        R.style.AppThemeBase3,
        R.style.primary,
        R.style.blue,
        R.style.pink,
        R.style.miku,
        R.style.purple,
        R.style.cyan,
        R.style.green,
        R.style.indigo,
        R.style.red,
        R.style.now,
        R.style.primary1,
        R.style.primary2,
        R.style.primary3,
        R.style.primary4,
        R.style.primary5,
        R.style.primary6,
        R.style.primary7,
        R.style.primary8,
        R.style.primary9,
        R.style.primary10,
        R.style.primary11,
        R.style.primary12,
        R.style.primary13,
        R.style.primary14,
        R.style.primary15,
        R.style.primary16,
        R.style.primary17,
        R.style.primary18,
        R.style.primary19,
        R.style.primary20,
    )
    private val colorMap = HashMap<Int, Int>()
    const val halftrans = 0x089a9a9a
    const val transparent = 0x00000000
    fun resetColor(context: Context) {
        colorMap.clear()
    }

    fun getColorPrimary(context: Context) =
        getAttrColor(context, androidx.appcompat.R.attr.colorPrimary)

    fun getColorPrimaryDark(context: Context) =
        getAttrColor(context, androidx.appcompat.R.attr.colorPrimaryDark)

    fun getColorHighlight(context: Context) =
        getAttrColor(context, com.google.android.material.R.attr.badgeTextColor)

    fun getTextColorPrimary(context: Context) =
        getAttrColor(context, android.R.attr.textColorPrimary)

    fun getTextColorPrimaryInverse(context: Context) =
        getAttrColor(context, android.R.attr.textColorPrimaryInverse)

    fun getTextColorPrimaryResID(context: Context) =
        getAttrResID(context, android.R.attr.textColorPrimary)

    /**
     * Returns a color associated with a particular attr ID
     * <p>
     * Starting in {@link Build.VERSION_CODES#M}, the returned
     * color will be styled for the specified Context's theme.
     *
     * @param attrId The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     * @return A single color value in the form 0xAARRGGBB.
     * @throws android.content.res.Resources.NotFoundException if the given ID
     *         does not exist.
     */
    fun getAttrColor(context: Context, attrId: Int): Int {
        if (colorMap[attrId] == null) {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attrId, typedValue, true)
            colorMap[attrId] = ContextCompat.getColor(context, typedValue.resourceId)
        }
        return colorMap[attrId]!!
    }

    fun getAttrColor(context: Context, attr: Int, alpha: Float): Int {
        val color = getAttrColor(context, attr)
        val originalAlpha = Color.alpha(color)
        return ColorUtils.setAlphaComponent(color, Math.round(originalAlpha * alpha))
    }

    fun getAttrResID(context: Context, attrId: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrId, typedValue, true)
        return typedValue.resourceId
    }

    @ColorRes
    fun Context.getColorResIDFromStyle(@StyleRes styleRes: Int): Int {
        val typedArray = theme.obtainStyledAttributes(
            styleRes,
            intArrayOf(androidx.appcompat.R.attr.colorPrimary)
        )
        val textColor = typedArray.getResourceId(0, 0)
        typedArray.recycle()
        return textColor
    }

    @ColorInt
    fun Context.getColorFromStyle(@StyleRes styleRes: Int): Int {
        val typedArray = theme.obtainStyledAttributes(
            styleRes,
            intArrayOf(androidx.appcompat.R.attr.colorPrimary)
        )
        val textColor = typedArray.getColor(0, 0)
        typedArray.recycle()
        return textColor
    }

    @JvmStatic
    fun themeInit(activity: AppCompatActivity) {
        activity.apply {
            val theme3 = PxEZApp.instance.pre.getBoolean("material3", true)
            val dynamicColor = PxEZApp.instance.pre.getBoolean("dynamicColor", true)
            val harmonizeColor = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    && PxEZApp.instance.pre.getBoolean("harmonizeColor", true)
            val seedColor = PxEZApp.instance.pre.getInt("color_int", -16738310)
            val colorTheme = PxEZApp.instance.pre.getString("color_theme", "1")
            val colorIndex = colorTheme?.toIntOrNull()?.coerceIn(0, colorThemeArray.size - 1) ?: 0
            if (theme3) {
                setTheme(colorThemeArray[0])
            }
            if (dynamicColor and DynamicColors.isDynamicColorAvailable()) {
                DynamicColors.applyToActivityIfAvailable(activity)
                //DynamicColors.applyToActivitiesIfAvailable(PxEZApp.instance)
            } else {
                if (harmonizeColor || colorIndex == 0)
                    applySeedColorToActivityIfAvailable(activity, seedColor)
                //seedColor = activity.getColorFromStyle(colorThemeArray[intColor])
                else
                    theme.applyStyle(colorThemeArray[colorIndex], true)
                // setTheme(colorThemeArray[intColor])
            }
            /*if (theme3 && HarmonizedColors.isHarmonizedColorAvailable()) {
                val MATERIAL_ATTRIBUTES = intArrayOf(
                    com.google.android.material.R.attr.colorOnPrimary,
                    //com.google.android.material.R.attr.colorSecondary,
                    //com.google.android.material.R.attr.colorAccent,
                    com.google.android.material.R.attr.colorError,
                    com.google.android.material.R.attr.colorOnError,
                    com.google.android.material.R.attr.colorErrorContainer,
                    com.google.android.material.R.attr.colorOnErrorContainer
                )
                val options = HarmonizedColorsOptions.Builder()
                    .setColorAttributes(HarmonizedColorAttributes.create(MATERIAL_ATTRIBUTES, R.style.ThemeOverlay_Material3_HarmonizedColors))
                    .build()
                HarmonizedColors.applyToContextIfAvailable(this, options)
            }*/
        }
    }

    fun showColorThemeDialog(themeFragment: ThemeFragment, it: Preference) {
        MaterialDialog(
            themeFragment.requireContext(),
            BottomSheet(LayoutMode.WRAP_CONTENT)
        ).show {
            val seedColor = PxEZApp.instance.pre.getInt("color_int", -16738310)
            val colorArray = colorThemeArray.mapIndexed { index, it ->
                if (index == 0) BackgroundGridItem(
                    R.drawable.ic_color_palette,
                    themeFragment.getString(R.string.action_select)
                )
                else BackgroundGridItem(context.getColorFromStyle(it), index.toString(), false)
            }
            var action: () -> Unit = { }
            title(R.string.title_change_theme)
            val gridItems = gridItems(colorArray) { _, index, item ->
                if (index == 0) {
                    action = {
                        ColorPickerDialog.Builder(context)
                            //.setTitle("Pick Theme")           	// Default "Choose Color"
                            .setColorShape(ColorShape.SQAURE)   // Default ColorShape.CIRCLE
                            .setDefaultColor(seedColor)     // Pass Default Color
                            .setColorListener { color, colorHex ->
                                // Handle Color Selection
                                it.summary = colorHex
                                PxEZApp.instance.pre.edit {
                                    putString("color_theme", colorHex)
                                    putInt("color_int", color)
                                }
                                resetColor(themeFragment.requireActivity())
                                PxEZApp.ActivityCollector.recreate()
                            }
                            .show()
                    }
                } else {
                    action = {
                        it.summary = item.title
                        PxEZApp.instance.pre.edit {
                            putString("color_theme", index.toString())
                            putInt("color_int", item.color)
                            resetColor(themeFragment.requireActivity())
                            PxEZApp.ActivityCollector.recreate()
                        }
                    }
                }
            }
            onDismiss {
                action.invoke()
            }
            cornerRadius(16.0F)
            negativeButton(android.R.string.cancel)
            positiveButton(R.string.action_apply)
            lifecycleOwner(themeFragment)
        }
    }
}

class BackgroundGridItem(var color: Int, override val title: String, val isResID: Boolean = true) :
    GridItem {
    var icon: Int? = null

    constructor(icon: Int, title: String) : this(-16738310, title, false) {
        this.icon = icon
    }

    override fun populateIcon(imageView: ImageView) {
        imageView.apply {
            icon?.also {
                setImageResource(R.drawable.ic_color_palette)
                imageTintList = ColorStateList.valueOf(ThemeUtil.getTextColorPrimary(context))
            } ?: run {
                color = if (isResID) {
                    ContextCompat.getColor(context, color)
                } else color
                setBackgroundColor(color)
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                marginEnd = 4.dp
                marginStart = 4.dp
            }
        }
    }
}