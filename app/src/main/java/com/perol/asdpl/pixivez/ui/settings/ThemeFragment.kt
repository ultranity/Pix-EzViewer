package com.perol.asdpl.pixivez.ui.settings

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil.showColorThemeDialog
import com.perol.asdpl.pixivez.services.PxEZApp

class ThemeFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pre_theme)
        findPreference<Preference>("dark_mode")!!.setOnPreferenceChangeListener { preference, newValue ->
            AppCompatDelegate.setDefaultNightMode(newValue.toString().toInt())
            PxEZApp.instance.setTheme(R.style.AppThemeBase)
            true
        }
        findPreference<Preference>("material3")?.apply {
            setOnPreferenceChangeListener { preference, newValue ->
                ThemeUtil.resetColor(requireActivity())
                snackbarApplyConfig()
                //PxEZApp.ActivityCollector.recreate()
                findPreference<Preference>("dynamicColor")?.isEnabled = newValue as Boolean
                colorThemeConfig()
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
                    colorThemeConfig()
                    ThemeUtil.resetColor(requireActivity())
                    PxEZApp.ActivityCollector.recreate()
                    true
                }
            }
        }
        findPreference<Preference>("harmonizeColor")?.apply {
            if (findPreference<Preference>("dynamicColor")?.isEnabled == true
                && PxEZApp.instance.pre.getBoolean("dynamicColor", true)
            ) {
                isEnabled = false
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                summary = getString(R.string.harmonizeColorAPIAlert)
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
            setOnPreferenceClickListener {
                if (isEnabled)
                    showColorThemeDialog(this@ThemeFragment, it)
                true
            }
        }
        colorThemeConfig()

        findPreference<SwitchPreferenceCompat>("bottomAppbar")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarApplyConfig()
            true
        }
        findPreference<SwitchPreferenceCompat>("refreshTab")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarApplyConfig()
            true
        }
        findPreference<SwitchPreferenceCompat>("banner_auto_loop")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarApplyConfig()
            true
        }

        findPreference<SwitchPreferenceCompat>("animation")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.animationEnable = newValue as Boolean
            true
        }
    }

    private fun colorThemeConfig() = findPreference<Preference>("theme")?.apply {
        //icon = ColorDrawable(ThemeUtil.getColorPrimary(requireContext()))
        if (PxEZApp.instance.pre.getBoolean("material3", true)
            && PxEZApp.instance.pre.getBoolean("dynamicColor", true)
        ) {
            isEnabled = false
            summary = "Dynamic"
            findPreference<Preference>("harmonizeColor")?.apply {
                isEnabled = false
                summary = "Dynamic"
            }
        } else {
            summary = PxEZApp.instance.pre.getString("color_theme", "1")
        }
    }

    private fun snackbarApplyConfig() {
        Snackbar.make(requireView(), getString(R.string.title_change_theme), Snackbar.LENGTH_SHORT)
            .setAction(R.string.restart_now) {
                PxEZApp.ActivityCollector.recreate()
            }.show()
    }
}