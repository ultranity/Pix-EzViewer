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

package com.perol.asdpl.pixivez.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BasicGridItem
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.drag.IDraggable
import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseBindingItem
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.onClick
import com.perol.asdpl.pixivez.base.onLongClick
import com.perol.asdpl.pixivez.base.setItems
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.databinding.DialogApiConfigBinding
import com.perol.asdpl.pixivez.databinding.DialogMeBinding
import com.perol.asdpl.pixivez.databinding.DialogSaveFormatBinding
import com.perol.asdpl.pixivez.databinding.SimpleTextItemBinding
import com.perol.asdpl.pixivez.networks.ImageHttpDns
import com.perol.asdpl.pixivez.networks.ImageHttpDns.ipPattern
import com.perol.asdpl.pixivez.objects.ClipBoardUtil
import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.AppUpdater
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.ui.MainActivity
import java.io.File
import java.io.FilenameFilter

class SettingsFragment : PreferenceFragmentCompat() {
    private val storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        defaultComponent =
            ComponentName(requireContext().packageName, "com.perol.asdpl.pixivez.pure")
        normalComponent =
            ComponentName(requireContext().packageName, "com.perol.asdpl.pixivez.normal")
        testComponent =
            ComponentName(requireContext().packageName, "com.perol.asdpl.pixivez.triangle")
        mdComponent = ComponentName(requireContext().packageName, "com.perol.asdpl.pixivez.md")
    }

    private lateinit var defaultComponent: ComponentName
    private lateinit var normalComponent: ComponentName
    private lateinit var testComponent: ComponentName
    private lateinit var mdComponent: ComponentName
    private fun enableComponent(componentName: ComponentName) {
        CrashHandler.instance.d("compon", componentName.packageName)
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

    private fun changeCompoent(position: Int) {
        Toasty.warning(PxEZApp.instance, R.string.changeing_icon_tip)
        when (position) {
            0 -> {
                enableComponent(defaultComponent)
                disableComponent(normalComponent)
                disableComponent(testComponent)
                disableComponent(mdComponent)
            }

            1 -> {
                enableComponent(normalComponent)
                disableComponent(defaultComponent)
                disableComponent(testComponent)
                disableComponent(mdComponent)
            }

            2 -> {
                enableComponent(testComponent)
                disableComponent(defaultComponent)
                disableComponent(normalComponent)
                disableComponent(mdComponent)
            }

            else -> {
                enableComponent(mdComponent)
                disableComponent(defaultComponent)
                disableComponent(normalComponent)
                disableComponent(testComponent)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_settings)
        findPreference<Preference>("APIConfig")!!.apply {
            if (Works.mirrorForDownload || Works.mirrorForView) {
                summary = getString(R.string.mirror) + ":" + Works.mirrorURL
            }
            setOnPreferenceClickListener {
                showAPIConfigDialog()
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
                CrashHandler.instance.e("VersionInfo", "Exception", e)
            }
        }
        findPreference<ListPreference>("language")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.language = newValue.toString().toInt()
            PxEZApp.locale = LanguageUtil.langToLocale(PxEZApp.language)
            snackbarForceRestart()
            true
        }

        findPreference<SwitchPreferenceCompat>("r18on")!!.setOnPreferenceChangeListener { preference, newValue ->
            snackbarRestart()
            true
        }
        findPreference<SwitchPreferenceCompat>("resume_unfinished_task")!!.setOnPreferenceChangeListener { preference, newValue ->
            Toasty.normal(PxEZApp.instance, R.string.needtorestart)
            true
        }
        findPreference<SwitchPreferenceCompat>("R18Folder")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.R18Folder = newValue as Boolean
            true
        }
        findPreference<SwitchPreferenceCompat>("R18Private")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.R18Private = newValue as Boolean
            true
        }
        findPreference<SwitchPreferenceCompat>("ShowDownloadToast")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.ShowDownloadToast = newValue as Boolean
            true
        }
        findPreference<ListPreference>("CollectMode")!!.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.CollectMode = (newValue as String).toInt()
            snackbarRestart()
            true
        }
        findPreference<ListPreference>("qualityDownload")!!.setOnPreferenceChangeListener { preference, newValue ->
            Works.qualityDownload = (newValue as String).toInt()
            true
        }
        findPreference<SeekBarPreference>("restrictSanity")!!.apply {
            max = AppDataRepo.currentUser.x_restrict + 6
        }.setOnPreferenceChangeListener { preference, newValue ->
            PxEZApp.restrictSanity = (newValue as Int)
            true
        }
    }

    private fun snackbarForceRestart() {
        Snackbar.make(requireView(), getString(R.string.needtorestart), Snackbar.LENGTH_SHORT)
            .setAction(R.string.restart_now) {
                val intent =
                    Intent(requireContext(), MainActivity::class.java).setAction("app.restart")
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
//                PxEZApp.instance.pre.edit().putString("storepath1", PxEZApp.storepath).apply()
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
            } else {
                Toasty.error(requireContext(), "Permissions not granted by the user")
            }
        }
    }

    @SuppressLint("CheckResult")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "me" -> {
                try {
                    val binding = DialogMeBinding.inflate(layoutInflater)
                    MaterialDialog(requireContext(), BottomSheet()).show {
                        cornerRadius(16f)
                        customView(view = binding.root)
                    }
                    binding.bg.setOnClickListener {
                        val url = if (BuildConfig.FLAVOR == "play") {
                            "https://youtu.be/Wu4fVGsEn8s"
                        } else {
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
                AppUpdater.checkUpgrade(requireActivity(), requireView())
            }

            "storepath1" -> {
//                startActivityForResult(Intent(activity, PathProviderActivity::class.java), 887)

                if (requireContext().allPermissionsGranted(storagePermissions)) {
                    showDirectorySelectionDialog()
                } else {
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
                } else {
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
                                } else {
                                    text.toString().removePrefix("/").removeSuffix("/") + "/"
                                }
                        }
                        positiveButton(R.string.save) { dialog ->
                            PxEZApp.instance.pre.edit {
                                putString("R18FolderPath", PxEZApp.R18FolderPath)
                            }
                            findPreference<Preference>("R18Folder")!!.apply {
                                summary = PxEZApp.R18FolderPath
                            }
                        }
                        negativeButton(android.R.string.cancel)
                        lifecycleOwner(this@SettingsFragment)
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
                        Toasty.info(PxEZApp.instance, "no browser found")
                    }
                } else {
                    val url = "https://github.com/ultranity/Pix-EzViewer"
                    val uri = Uri.parse(url)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
            }

            "icons" -> {
                showApplicationIconReplacementDialog()
            }

            "view_report" -> {
                val list = getCrashReportFiles() ?: arrayOf()
                val cr = list.map {
                    File(activity?.filesDir, it)
                        .readText().replace("\\n", "\n")
                        .replace("\\t", "\t")
                        .replace("\\:", ":")
                }
                MaterialDialogs(requireContext()).show {
                    setTitle(R.string.crash_title)
                    setItems(cr.map { LogsBindingItem(it) })
                        .onClick { v, adapter, item, position ->
                            MaterialDialogs(requireContext()).show {
                                setMessage(item.text)
                            }
                            true
                        }.onLongClick { v, adapter, item, position ->
                            ClipBoardUtil.putTextIntoClipboard(requireContext(), item.text)
                            true
                        }
                    confirmButton()
                    setNeutralButton(R.string.clearhistory) { _, _ ->
                        list.forEach {
                            File(activity?.filesDir, it).delete()
                        }
                    }
                }
            }

            "view_logs" -> {
                MaterialDialogs(requireContext()).show {
                    setTitle("Logs")
                    setItems(CrashHandler.instance.logs.map { LogsBindingItem(it.toString()) })
                        .onClick { v, adapter, item, position ->
                            MaterialDialogs(requireContext()).show {
                                setMessage(item.text)
                            }
                            true
                        }.onLongClick { v, adapter, item, position ->
                            ClipBoardUtil.putTextIntoClipboard(requireContext(), item.text)
                            true
                        }
                    confirmButton()
                    setNeutralButton(R.string.clearhistory) { _, _ ->
                        CrashHandler.instance.logs.clear()
                    }
                }
            }
        }

        return super.onPreferenceTreeClick(preference)
    }

    private fun showAPIConfigDialog() {
        val binding = DialogApiConfigBinding.inflate(layoutInflater)
        val descTable = binding.formatDescTable
        val urlInput = binding.urlInput
        val formatInput = binding.formatInput
        PxEZApp.instance.pre.run {
            binding.dnsProxy.isChecked = getBoolean("dnsProxy", false)
            binding.forceIP.isChecked = getBoolean("forceIP", true)
            binding.shuffleIP.isChecked = getBoolean("shuffleIP", true)
            binding.mirrorLinkView.isChecked = getBoolean("mirrorLinkView", false)
            binding.mirrorLinkDownload.isChecked = getBoolean("mirrorLinkDownload", false)
        }
        binding.ipInput.setText(ImageHttpDns.customIPs.joinToString())
        binding.ipInput.addTextChangedListener(afterTextChanged = { s -> //onTextChanged ={ s: CharSequence?, _,_,_->
            if (!s.isNullOrEmpty()) {
                val isValid = ipPattern.matches(s)
                if (isValid) {
                    binding.ipInput.error = null
                } else {
                    binding.ipInput.error = "Invalid IP address list format"
                }
            }
        })
        urlInput.setText(Works.mirrorURL)
        formatInput.setText(Works.mirrorFormat)
        val urlInputEditable = formatInput.editableText
        for (i in 1 until descTable.childCount)
            descTable.getChildAt(i).setOnClickListener {
                urlInputEditable.insert(formatInput.selectionStart, it.tag.toString())
            }
        val dialog =
            MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT))
        dialog.show {
            title(R.string.saveformat)
            customView(view = binding.root, scrollable = true, horizontalPadding = true)
            positiveButton(R.string.save) { dialog ->
                Works.mirrorURL = "${urlInput.text}"
                Works.mirrorFormat = "${formatInput.text}"
                Works.forceIP = binding.forceIP.isChecked
                ImageHttpDns.shuffleIP = binding.shuffleIP.isChecked
                ImageHttpDns.setCustomIPs(binding.ipInput.text.toString())
                Works.mirrorForView = binding.mirrorLinkView.isChecked
                Works.mirrorForDownload = binding.mirrorLinkDownload.isChecked
                findPreference<Preference>("APIConfig")!!.apply {
                    summary = Works.mirrorURL + Works.mirrorFormat
                }
                PxEZApp.instance.pre.edit {
                    putString("mirrorURL", Works.mirrorURL)
                    putString("mirrorFormat", Works.mirrorFormat)
                    putBoolean("dnsProxy", binding.dnsProxy.isChecked)
                    putBoolean("forceIP", Works.forceIP)
                    putBoolean("shuffleIP", ImageHttpDns.shuffleIP)
                    putString("customIPs", ImageHttpDns.customIPs.joinToString())
                    putBoolean("mirrorLinkView", Works.mirrorForView)
                    putBoolean("mirrorLinkDownload", Works.mirrorForDownload)
                }
                Works.smirrorURL = Works.lookup(Works.mirrorURL)
                snackbarForceRestart()
            }
            negativeButton(android.R.string.cancel)
            lifecycleOwner(this@SettingsFragment)
        }
    }

    private fun showSaveFormatDialog() {
        // Setup custom view content
        val binding = DialogSaveFormatBinding.inflate(layoutInflater)
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
                PxEZApp.instance.pre.edit {
                    putString("filesaveformat", PxEZApp.saveformat)
                }
                findPreference<Preference>("filesaveformat")!!.apply {
                    summary = PxEZApp.saveformat
                }
                PxEZApp.TagSeparator = "${tagSeparator.text}"
                PxEZApp.instance.pre.edit {
                    putString("TagSeparator", PxEZApp.TagSeparator)
                }
            }
            negativeButton(android.R.string.cancel)
            lifecycleOwner(this@SettingsFragment)
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
            folderChooser(
                initialDirectory = File(PxEZApp.storepath),
                allowFolderCreation = true,
                context = context
            ) { _, folder ->
                with(folder.absolutePath) {
                    PxEZApp.storepath = this
                    PxEZApp.instance.pre.edit {
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
            lifecycleOwner(this@SettingsFragment)
        }
    }

    @SuppressLint("CheckResult")
    private fun showApplicationIconReplacementDialog() {
        val items = listOf(
            BasicGridItem(R.mipmap.ic_launcher_blue, "Pure"),
            BasicGridItem(R.mipmap.ic_launcher, "MD"),
            BasicGridItem(R.mipmap.ic_launcherep, "Triangle"),
            BasicGridItem(R.mipmap.ic_launchermd, "Probe")
        ) // my bad

        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(R.string.title_change_icon)
            gridItems(items) { _, index, _ ->
                changeCompoent(index)
            }
            cornerRadius(16.0F)
            negativeButton(android.R.string.cancel)
            positiveButton(R.string.action_change)
            lifecycleOwner(this@SettingsFragment)
        }
    }

    private fun Context.allPermissionsGranted(permissions: Array<String>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1
    }
}

class LogsBindingItem(val text: String) : BaseBindingItem<SimpleTextItemBinding>(), IDraggable {
    override val type: Int = R.id.text
    override val isDraggable: Boolean = true
    override fun bindView(binding: SimpleTextItemBinding, payloads: List<Any>) {
        binding.text.text = text
    }
}