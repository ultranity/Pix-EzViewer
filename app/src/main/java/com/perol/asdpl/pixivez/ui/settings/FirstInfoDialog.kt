package com.perol.asdpl.pixivez.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.data.AppDataRepo

class FirstInfoDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialogs(requireContext()).create {
            setTitle(R.string.read_it)
            setMessage(R.string.app_help)
            setPositiveButton(R.string.I_know) { _, _ ->
                AppDataRepo.pre.setBoolean("firstinfo", true)
            }
            setNeutralButton(R.string.other) { _, _ ->
                MaterialDialogs(requireContext()).show {
                    setTitle(R.string.other)
                    setMessage(
                        Html.fromHtml(
                            getString(R.string.app_features) +
                                    getString(R.string.hide_downloaded_summary) +
                                    getString(R.string.hide_downloaded_detail)
                        )
                    )
                    confirmButton()
                }
            }
        }
    }
}
