package com.perol.asdpl.pixivez.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.networks.SharedPreferencesServices

class FirstInfoDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val normalDialog = MaterialAlertDialogBuilder(it)
            normalDialog.setMessage(R.string.app_help)
            normalDialog.setTitle(R.string.read_it)
            normalDialog.setPositiveButton(R.string.I_know) { _, _ ->
                SharedPreferencesServices.getInstance().setBoolean("firstinfo", true)
            }
            normalDialog.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}