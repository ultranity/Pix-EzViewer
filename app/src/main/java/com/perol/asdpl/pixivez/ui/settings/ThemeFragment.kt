package com.perol.asdpl.pixivez.ui.settings

import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.GridItem
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.services.PxEZApp

class ThemeFragment : PreferenceFragmentCompat() {
    private class BackgroundGridItem(@ColorRes private val color: Int, override val title: String) :
        GridItem {

        override fun populateIcon(imageView: ImageView) {
            imageView.apply {
                setBackgroundColor(ContextCompat.getColor(imageView.context, color))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    marginEnd = 4.dp
                    marginStart = 4.dp
                }
            }
        }

        private val Int.dp: Int get() = toFloat().dp.toInt()

        private val Float.dp: Float
            get() = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                this,
                Resources.getSystem().displayMetrics
            )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pre_theme)
        findPreference<Preference>("dark_mode")!!.setOnPreferenceChangeListener { preference, newValue ->
            AppCompatDelegate.setDefaultNightMode(newValue.toString().toInt())
            PxEZApp.instance.setTheme(R.style.AppThemeBase_pink)
            true
        }
        findPreference<Preference>("material3")?.apply {
            setOnPreferenceChangeListener { preference, newValue ->
                ThemeUtil.resetColor(requireActivity())
                snackbarApplyConfig()
                //PxEZApp.ActivityCollector.recreate()
                true
            }
        }
        findPreference<Preference>("dynamicColor")?.apply {
            if (!PxEZApp.instance.pre.getBoolean("material3", true)) {
                isEnabled = false
            }
            if (!DynamicColors.isDynamicColorAvailable()) {
                summary = getString(R.string.dynamicColorAPIAlert)
                isEnabled = false
            } else {
                setOnPreferenceChangeListener { preference, newValue ->
                    ThemeUtil.resetColor(requireActivity())
                    PxEZApp.ActivityCollector.recreate()
                    true
                }
            }
        }
        findPreference<Preference>("theme")?.apply {
            //icon = ColorDrawable(ThemeUtil.getColorPrimary(requireContext()))
            if (PxEZApp.instance.pre.getBoolean("dynamicColor", false)) {
                isEnabled = false
                summary = "Dynamic"
            } else {
                val colorItems = listOf(
                    BackgroundGridItem(R.color.colorPrimary, "Primary"),
                    BackgroundGridItem(R.color.md_blue_300, "Blue"),
                    BackgroundGridItem(R.color.pink, "Pink"),
                    BackgroundGridItem(R.color.miku, "Miku"),
                    BackgroundGridItem(R.color.md_purple_500, "Purple"),
                    BackgroundGridItem(R.color.md_cyan_300, "Cyan"),
                    BackgroundGridItem(R.color.md_green_300, "Green"),
                    BackgroundGridItem(R.color.md_indigo_300, "Indigo"),
                    BackgroundGridItem(R.color.md_red_500, "Red"),
                    BackgroundGridItem(R.color.now, "Pale green")
                )
                summary =
                    colorItems[
                        PxEZApp.instance.pre.getInt("colorint", 0)
                    ].title
                setOnPreferenceClickListener {
                    MaterialDialog(
                        requireContext(),
                        BottomSheet(LayoutMode.WRAP_CONTENT)
                    ).show {
                        lateinit var action: () -> Unit

                        title(R.string.title_change_theme)
                        val gridItems = gridItems(colorItems) { _, index, item ->
                            it.summary = item.title
                            PxEZApp.instance.pre.edit {
                                putInt("colorint", index)
                            }
                            ThemeUtil.resetColor(requireActivity())
                            action = {
                                PxEZApp.ActivityCollector.recreate()
                            }
                        }
                        onDismiss {
                            action.invoke()
                        }
                        cornerRadius(16.0F)
                        negativeButton(android.R.string.cancel)
                        positiveButton(R.string.action_apply)
                        lifecycleOwner(this@ThemeFragment)
                    }
                    true
                }
            }
        }


        findPreference<SwitchPreference>("refreshTab")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarApplyConfig()
            true
        }
        findPreference<SwitchPreference>("use_picX_layout_main")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarApplyConfig()
            true
        }
        findPreference<SwitchPreference>("show_user_img_main")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarApplyConfig()
            true
        }
        findPreference<SwitchPreference>("banner_auto_loop")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarApplyConfig()
            true
        }

        findPreference<SwitchPreference>("animation")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.animationEnable = newValue as Boolean
            true
        }
    }

    private fun snackbarApplyConfig() {
        Snackbar.make(requireView(), getString(R.string.title_change_theme), Snackbar.LENGTH_SHORT)
            .setAction(R.string.restart_now) {
                PxEZApp.ActivityCollector.recreate()
            }.show()
    }
}