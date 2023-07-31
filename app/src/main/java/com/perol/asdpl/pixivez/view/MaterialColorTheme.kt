package com.perol.asdpl.pixivez.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.google.android.material.R
import com.google.android.material.color.ColorResourcesOverride
import com.google.android.material.color.MaterialColorUtilitiesHelper
import com.google.android.material.color.utilities.DislikeAnalyzer
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.TemperatureCache
import com.google.android.material.color.utilities.TonalPalette
import com.google.android.material.color.utilities.Variant
import com.google.android.material.resources.MaterialAttributes


/**
 * Provides important settings for creating colors dynamically, and 6 color palettes. Requires: 1. A
 * color. (source color) 2. A theme. (Variant) 3. Whether or not its dark mode. 4. Contrast level.
 * (-1 to 1, currently contrast ratio 3.0 and 7.0)
 *
 * @hide
 */
/*
@SuppressLint("RestrictedApi")
open class DynamicScheme(
    val sourceColorHct: Hct,
    val variant: Variant,
    val isDark: Boolean,
    val contrastLevel: Double,
    val primaryPalette: TonalPalette,
    val secondaryPalette: TonalPalette,
    val tertiaryPalette: TonalPalette,
    val neutralPalette: TonalPalette,
    val neutralVariantPalette: TonalPalette
) {
    val sourceColorArgb: Int
    val errorPalette: TonalPalette

    init {
        sourceColorArgb = sourceColorHct.toInt()
        errorPalette = TonalPalette.fromHueAndChroma(25.0, 84.0)
    }
}
*/

/**
 * A scheme that places the source color in Scheme.primaryContainer.
 *
 *
 * Primary Container is the source color, adjusted for color relativity. It maintains constant
 * appearance in light mode and dark mode. This adds ~5 tone in light mode, and subtracts ~5 tone in
 * dark mode.
 *
 *
 * Tertiary Container is an analogous color, specifically, the analog of a color wheel divided
 * into 6, and the precise analog is the one found by increasing hue. This is a scientifically
 * grounded equivalent to rotating hue clockwise by 60 degrees. It also maintains constant
 * appearance.
 *
 * @hide
 */
@SuppressLint("RestrictedApi")
class SchemeContent(sourceColorHct: Hct, isDark: Boolean, contrastLevel: Double) : DynamicScheme(
    sourceColorHct,
    Variant.CONTENT,
    isDark,
    contrastLevel,
    TonalPalette.fromHueAndChroma(sourceColorHct.hue, sourceColorHct.chroma),
    TonalPalette.fromHueAndChroma(
        sourceColorHct.hue,
        Math.max(sourceColorHct.chroma - 32.0, sourceColorHct.chroma * 0.5)
    ),
    TonalPalette.fromHct(
        DislikeAnalyzer.fixIfDisliked(
            TemperatureCache(sourceColorHct)
                .getAnalogousColors( /* count= */3,  /* divisions= */6)[2]
        )
    ),
    TonalPalette.fromHueAndChroma(sourceColorHct.hue, sourceColorHct.chroma / 8.0),
    TonalPalette.fromHueAndChroma(
        sourceColorHct.hue, sourceColorHct.chroma / 8.0 + 4.0
    )
)


@SuppressLint("RestrictedApi")
fun isLightTheme(context: Context): Boolean {
    return MaterialAttributes.resolveBoolean(
        context, R.attr.isLightTheme,  /* defaultValue= */true
    )
}

@SuppressLint("RestrictedApi")
fun applySeedColorToActivityIfAvailable(
    activity: Activity,
    contentBasedSeedColor: Int,
    chromaOffset: Double = 0.0,
    isDark: Boolean? = null,
    contrastLevel: Double = 0.0
) {
    // Only retrieves the theme overlay if we're applying just dynamic colors.
    // Applies content-based dynamic colors if content-based source is provided.
    val hctSource = Hct.fromInt(contentBasedSeedColor)
    //hctSource.chroma += chromaOffset
    val scheme = SchemeContent(
        hctSource,
        isDark ?: (!isLightTheme(activity)),
        contrastLevel
    )
    val resourcesOverride = ColorResourcesOverride.getInstance()
    if (resourcesOverride == null) {
        return
    } else {
        if (!resourcesOverride.applyIfPossible(
                activity,
                MaterialColorUtilitiesHelper.createColorResourcesIdsToColorValues(scheme)
            )
        ) {
            return
        }
    }
}