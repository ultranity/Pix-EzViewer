package com.perol.asdpl.pixivez.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.repository.UserInfoSharedPreferences

class FirstInfoDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val normalDialog = MaterialAlertDialogBuilder(it)
            normalDialog.setMessage(R.string.app_help)
            normalDialog.setTitle(R.string.read_it)
            normalDialog.setPositiveButton(R.string.I_know) { _, _ ->
                UserInfoSharedPreferences.getInstance().setBoolean("firstinfo", true)
            }
            normalDialog.setNegativeButton(R.string.other) { _, _ ->
                MaterialDialog(it).show {
                    title(R.string.other)
                    message(text = getString(R.string.app_features) + getString(R.string.hide_downloaded_summary) + getString(R.string.hide_downloaded_detail)) {
                        html()
                    }
                    positiveButton(android.R.string.ok) {}
                }
            }
            normalDialog.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
