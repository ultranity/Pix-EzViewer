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

package com.perol.asdpl.pixivez.fragments

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.*
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BasicGridItem
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.HelloMActivity
import com.perol.asdpl.pixivez.databinding.CustomformatviewBinding
import com.perol.asdpl.pixivez.databinding.DialogMeBinding
import com.perol.asdpl.pixivez.databinding.DialogMirrorLinkBinding
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.services.checkUpdate
import java.io.File
import java.io.FilenameFilter

class SettingFragment : PreferenceFragmentCompat() {
    private val storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private lateinit var pre: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pre = PxEZApp.instance.pre
        defaultComponent =
            ComponentName(requireContext().packageName, "com.perol.asdpl.pixivez.normal")
        testComponent =
            ComponentName(requireContext().packageName, "com.perol.asdpl.pixivez.triangle")
        mdComponent = ComponentName(requireContext().packageName, "com.perol.asdpl.pixivez.md")
    }

    private lateinit var defaultComponent: ComponentName
    private lateinit var testComponent: ComponentName
    private lateinit var mdComponent: ComponentName
    private fun enableComponent(componentName: ComponentName) {
        Log.d("compon", componentName.packageName)
        val state = activity?.packageManager!!.getComponentEnabledSetting(componentName)
        if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return
        }
        activity?.packageManager!!.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun disableComponent(componentName: ComponentName) {
        val state = activity?.packageManager!!.getComponentEnabledSetting(componentName)
        if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            return
        }
        activity?.packageManager!!.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun getCrashReportFiles(): Array<String>? {
        val filesDir = activity?.filesDir
        val filter = FilenameFilter { dir, name -> name.endsWith(".cr") }
        return filesDir?.list(filter)
    }

    private fun onClick(position: Int) {
        Toast.makeText(PxEZApp.instance, "正在尝试更换，等待启动器刷新", Toast.LENGTH_SHORT).show()
        when (position) {
            0 -> {
                enableComponent(defaultComponent)
                disableComponent(testComponent)
                disableComponent(mdComponent)
            }
            1 -> {
                enableComponent(testComponent)
                disableComponent(defaultComponent)
                disableComponent(mdComponent)
            }
            2 -> {
                enableComponent(mdComponent)
                disableComponent(defaultComponent)
                disableComponent(testComponent)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_settings)
        findPreference<SwitchPreference>("disableproxy")!!.apply {
            if (Works.mirrorLinkDownload || Works.mirrorLinkView) {
                summary = getString(R.string.mirror) + ":" + Works.mirrorURL
            }
            setOnPreferenceClickListener {
                showMirrorLinkDialog()
                Works.spximg = Works.lookup(Works.opximg)
                Works.smirrorURL = Works.lookup(Works.mirrorURL)
                snackbarForceRestart()
                true
            }
            setOnPreferenceChangeListener { preference, newValue ->
                true
            }
        }

        findPreference<Preference>("storepath1")!!.apply {
            setDefaultValue(PxEZApp.storepath)
            summary = PxEZApp.storepath
        }
        findPreference<Preference>("filesaveformat")!!.apply {
            setDefaultValue(PxEZApp.saveformat)
            summary = PxEZApp.saveformat
        }
        findPreference<Preference>("R18Folder")!!.summary = PxEZApp.R18FolderPath
        findPreference<Preference>("version")!!.apply {
            try {
                // ---get the package info---
                val pm = context.packageManager
                val pi = pm.getPackageInfo(context.packageName, 0)
                summary = pi.versionName
            } catch (e: Exception) {
                Log.e("VersionInfo", "Exception", e)
            }
        }
        findPreference<ListPreference>("language")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.language = newValue.toString().toInt()
            PxEZApp.locale = LanguageUtil.langToLocale(PxEZApp.language)
            snackbarForceRestart()
            true
        }
        findPreference<SwitchPreference>("refreshTab")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarRestart()
            true
        }
        findPreference<SwitchPreference>("use_picX_layout_main")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarRestart()
            true
        }
        findPreference<SwitchPreference>("show_user_img_main")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarRestart()
            true
        }
        findPreference<SwitchPreference>("banner_auto_loop")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarRestart()
            true
        }

        findPreference<SwitchPreference>("r18on")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarRestart()
            true
        }
        findPreference<SwitchPreference>("resume_unfinished_task")!!.setOnPreferenceChangeListener { preference, newValue ->
            Toasty.normal(PxEZApp.instance, getString(R.string.needtorestart), Toast.LENGTH_SHORT)
                .show()
            true
        }
        findPreference<SwitchPreference>("animation")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.animationEnable = newValue as Boolean
            true
        }
        findPreference<SwitchPreference>("R18Folder")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.R18Folder = newValue as Boolean
            true
        }
        findPreference<SwitchPreference>("R18Private")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.R18Private = newValue as Boolean
            true
        }
        findPreference<SwitchPreference>("ShowDownloadToast")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.ShowDownloadToast = newValue as Boolean
            true
        }
        findPreference<ListPreference>("CollectMode")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.CollectMode = (newValue as String).toInt()
            snackbarRestart()
            true
        }
    }

    private fun snackbarForceRestart() {
        Snackbar.make(requireView(), getString(R.string.needtorestart), Snackbar.LENGTH_SHORT)
            .setAction(R.string.restart_now) {
                val intent = Intent(context, HelloMActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                requireContext().startActivity(intent)
            }.show()
    }

    private fun snackbarRestart() {
        Snackbar.make(requireView(), getString(R.string.needtorestart), Snackbar.LENGTH_SHORT)
            .setAction(R.string.restart_now) {
                PxEZApp.ActivityCollector.recreate()
            }.show()
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (resultCode == Activity.RESULT_OK) {
//
//            if (requestCode == 887) {
//                val path = data!!.getStringExtra("path")
//                PxEZApp.storepath = path
//                pre.edit().putString("storepath1", PxEZApp.storepath).apply()
//                findPreference<Preference>("storepath1")!!.apply {
//                    summary = path
//                }
//
//            }
//
//
//        }
//
//    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (requireContext().allPermissionsGranted(storagePermissions)) {
                showDirectorySelectionDialog()
            }
            else {
                Toast.makeText(
                    requireContext(),
                    "Permissions not granted by the user",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "me" -> {
                try {
                    val binding = DialogMeBinding.inflate(layoutInflater)
                    val dialog = MaterialDialog(requireContext(), BottomSheet()).show {
                        cornerRadius(16f)
                        customView(view = binding.root)
                    }
                    binding.bg.setOnClickListener {
                        val url = if (BuildConfig.FLAVOR == "play") {
                            "https://youtu.be/Wu4fVGsEn8s"
                        }
                        else {
                            "https://www.bilibili.com/video/BV1E741137mf"
                        }
                        val uri = Uri.parse(url)
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            "me0" -> {
                val url = "https://github.com/ultranity"
                val uri = Uri.parse(url)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            "check" -> {
                if (BuildConfig.FLAVOR != "bugly") {
                    try {
                        val uri = Uri.parse("https://github.com/ultranity/Pix-EzViewer/releases/latest")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toasty.info(PxEZApp.instance, "no browser found", Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    checkUpdate()
                }
            }
            "storepath1" -> {
//                startActivityForResult(Intent(activity, PathProviderActivity::class.java), 887)

                if (requireContext().allPermissionsGranted(storagePermissions)) {
                    showDirectorySelectionDialog()
                }
                else {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        storagePermissions,
                        REQUEST_CODE_PERMISSIONS
                    )
                }
            }
            "filesaveformat" -> {
                if (requireContext().allPermissionsGranted(storagePermissions)) {
                    showSaveFormatDialog()
                }
                else {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        storagePermissions,
                        REQUEST_CODE_PERMISSIONS
                    )
                }
            }
            "R18Folder" -> {
                if (PxEZApp.R18Folder) {
                    // onPreferenceTreeClick called after switch change
                    MaterialDialog(requireContext()).show {
                        title(R.string.block_tag)
                        message(R.string.R18_folder)
                        input(
                            prefill = PxEZApp.R18FolderPath,
                            hint = "xRestrict/"
                        ) { dialog, text ->
                            PxEZApp.R18FolderPath =
                                if (text.isBlank()) {
                                    "xRestrict/"
                                }
                                else {
                                    text.toString().removePrefix("/").removeSuffix("/") + "/"
                                }
                        }
                        positiveButton(R.string.save) { dialog ->
                            pre.apply {
                                putString("R18FolderPath", PxEZApp.R18FolderPath)
                            }
                            findPreference<Preference>("R18Folder")!!.apply {
                                summary = PxEZApp.R18FolderPath
                            }
                        }
                        negativeButton(android.R.string.cancel)
                        lifecycleOwner(this@SettingFragment)
                    }
                }
            }
            "version" -> {
                if (BuildConfig.FLAVOR == "play") {
                    try {
                        val uri =
                            Uri.parse("https://play.google.com/store/apps/details?id=com.perol.asdpl.play.pixivez")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toasty.info(PxEZApp.instance, "no browser found", Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    val url = "https://github.com/ultranity/Pix-EzViewer"
                    val uri = Uri.parse(url)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
            }
            "icons" -> {
                showApplicationIconReplacementDialog()
            }
            "viewreport" -> {
                val list = getCrashReportFiles()
                var report = ""
                for (a in list!!.indices) {
                    if (a > 10) {
                        continue
                    }
                    val cr = File(activity?.filesDir, list[a])
                    report += cr.readText()
                }
                report = report.replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\:", ":")
                val dialogBuild = MaterialAlertDialogBuilder(requireActivity())
                dialogBuild.setMessage(report).setTitle(getString(R.string.crash_title))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                    }
                    .create().show()
            }
        }

        return super.onPreferenceTreeClick(preference)
    }
    private fun showMirrorLinkDialog() {
        val binding = DialogMirrorLinkBinding.inflate(layoutInflater)
        val descTable = binding.formatDescTable
        val urlInput = binding.urlInput
        val formatInput = binding.formatInput
        val mirrorLinkDownload = binding.mirrorLinkDownload
        val mirrorLinkView = binding.mirrorLinkView
        mirrorLinkView.isChecked = pre.getBoolean("mirrorLinkView", false)
        mirrorLinkDownload.isChecked = pre.getBoolean("mirrorLinkDownload", false)
        urlInput.setText(Works.mirrorURL)
        formatInput.setText(Works.mirrorFormat)
        val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT))
        dialog.show {
            title(R.string.saveformat)
            customView(view = binding.root, scrollable = true, horizontalPadding = true)
            positiveButton(R.string.save) { dialog ->
                Works.mirrorURL = "${urlInput.text}"
                Works.mirrorFormat = "${formatInput.text}"
                Works.mirrorLinkView = mirrorLinkView.isChecked
                Works.mirrorLinkDownload = mirrorLinkDownload.isChecked
                findPreference<Preference>("disableproxy")!!.apply {
                    summary = Works.mirrorURL + Works.mirrorFormat
                }
                pre.apply {
                    putString("mirrorURL", Works.mirrorURL)
                    putString("mirrorFormat", Works.mirrorFormat)
                    putBoolean("mirrorLinkView", Works.mirrorLinkView)
                    putBoolean("mirrorLinkDownload", Works.mirrorLinkDownload)
                }
            }
            negativeButton(android.R.string.cancel)
            lifecycleOwner(this@SettingFragment)
        }
        val urlInputEditable = formatInput.editableText
        for (i in 1 until descTable.childCount)
            descTable.getChildAt(i).setOnClickListener {
                urlInputEditable.insert(formatInput.selectionStart, it.tag.toString())
            }
    }
    private fun showSaveFormatDialog() {
        // Setup custom view content
        val binding = CustomformatviewBinding.inflate(layoutInflater)
        val descTable = binding.formatDescTable
        val sampleTable = binding.formatSampleTable
        val customizedFormatInput = binding.customizedformat
        val tagSeparator = binding.tagSeparator
        customizedFormatInput.setText(PxEZApp.saveformat)
        tagSeparator.setText(PxEZApp.TagSeparator)
        val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT))
        dialog.show {
            title(R.string.saveformat)
            customView(view = binding.root, scrollable = true, horizontalPadding = true)
            positiveButton(R.string.save) { dialog ->
                PxEZApp.saveformat = "${customizedFormatInput.text}"
                pre.apply {
                    putString("filesaveformat", PxEZApp.saveformat)
                }
                findPreference<Preference>("filesaveformat")!!.apply {
                    summary = PxEZApp.saveformat
                }
                PxEZApp.TagSeparator = "${tagSeparator.text}"
                pre.apply {
                    putString("TagSeparator", PxEZApp.TagSeparator)
                }
            }
            negativeButton(android.R.string.cancel)
            lifecycleOwner(this@SettingFragment)
        }
        val inputEditable = customizedFormatInput.editableText
        for (i in 1 until descTable.childCount)
            descTable.getChildAt(i).setOnClickListener {
                inputEditable.insert(customizedFormatInput.selectionStart, it.tag.toString())
            }
        for (i in 1 until sampleTable.childCount)
            sampleTable.getChildAt(i).setOnClickListener {
                inputEditable.clear()
                inputEditable.insert(0, it.tag.toString())
            }
    }

    private fun showDirectorySelectionDialog() {
        MaterialDialog(requireContext()).show {
            title(R.string.title_save_path)
            folderChooser(initialDirectory = File(PxEZApp.storepath), allowFolderCreation = true, context = context) { _, folder ->
                with(folder.absolutePath) {
                    PxEZApp.storepath = this
                    pre.apply {
                        putString("storepath1", PxEZApp.storepath)
                    }
                    findPreference<Preference>("storepath1")!!.apply {
                        summary = this@with
                    }
                }
            }
            cornerRadius(2.0F)
            negativeButton(android.R.string.cancel)
            positiveButton(R.string.action_select)
            lifecycleOwner(this@SettingFragment)
        }
    }

    private fun showApplicationIconReplacementDialog() {
        val items = listOf(
            BasicGridItem(R.mipmap.ic_launcher, "MD"),
            BasicGridItem(R.mipmap.ic_launcherep, "Triangle"),
            BasicGridItem(R.mipmap.ic_launchermd, "Probe")
        ) // my bad

        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(R.string.title_change_icon)
            gridItems(items) { _, index, _ ->
                onClick(index)
            }
            cornerRadius(16.0F)
            negativeButton(android.R.string.cancel)
            positiveButton(R.string.action_change)
            lifecycleOwner(this@SettingFragment)
        }
    }

    private fun Context.allPermissionsGranted(permissions: Array<String>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private inline fun SharedPreferences.apply(modifier: SharedPreferences.Editor.() -> Unit) {
        edit().apply { modifier() }.run { apply() }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1
    }
}
