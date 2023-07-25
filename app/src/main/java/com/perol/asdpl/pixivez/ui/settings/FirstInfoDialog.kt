package com.perol.asdpl.pixivez.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.AppDataRepo

class FirstInfoDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireActivity())
            .setMessage(R.string.app_help)
            .setTitle(R.string.read_it)
            .setPositiveButton(R.string.I_know) { _, _ ->
                AppDataRepo.pre.setBoolean("firstinfo", true)
            }
            .setNeutralButton(R.string.other) { _, _ ->
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.other)
                    .setMessage(
                        Html.fromHtml(
                            getString(R.string.app_features) +
                                    getString(R.string.hide_downloaded_summary) +
                                    getString(R.string.hide_downloaded_detail)
                        )
                    )
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()
            }
            .create()
    }
}
