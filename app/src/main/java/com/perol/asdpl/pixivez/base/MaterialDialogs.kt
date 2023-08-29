package com.perol.asdpl.pixivez.base

import android.content.Context
import android.content.DialogInterface
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.perol.asdpl.pixivez.R


open class MaterialDialogs(context: Context) : MaterialAlertDialogBuilder(context) {
    private val showListeners = mutableListOf<(AlertDialog) -> Unit>()
    fun onShow(func: (AlertDialog) -> Unit) {
        showListeners.add(func)
    }

    override fun show(): AlertDialog {
        val dialog = create()
        dialog.show()
        showListeners.forEach {
            it(dialog)
        }
        return dialog
    }

    inline fun create(block: MaterialDialogs.() -> Unit): AlertDialog {
        block()
        return create()
    }

    inline fun show(block: MaterialDialogs.() -> Unit): AlertDialog {
        block()
        return show()
    }

    fun <T : MaterialAlertDialogBuilder> T.confirmButton(
        textId: Int = android.R.string.ok,
        listener: DialogInterface.OnClickListener? = null
    ): MaterialAlertDialogBuilder {
        return setPositiveButton(textId, listener)
    }

    fun <T : MaterialAlertDialogBuilder> T.cancelButton(
        textId: Int = android.R.string.cancel,
        listener: DialogInterface.OnClickListener? = null
    ): MaterialAlertDialogBuilder {
        return setNegativeButton(textId, listener)
    }
}

fun <T : MaterialAlertDialogBuilder> T.confirmButton(
    textId: Int = android.R.string.ok,
    listener: DialogInterface.OnClickListener? = null
): MaterialAlertDialogBuilder {
    return setPositiveButton(textId, listener)
}

fun <T : MaterialAlertDialogBuilder> T.cancelButton(
    textId: Int = android.R.string.cancel,
    listener: DialogInterface.OnClickListener? = null
): MaterialAlertDialogBuilder {
    return setNegativeButton(textId, listener)
}

fun MaterialDialogs.setInput(showIME: Boolean = false, config: TextInputLayout.() -> Unit) {
    setView(R.layout.dialog_item_edit_text)
    onShow {
        val layout = getInputLayout(it)
        layout.config()
        if (showIME) {
            layout.editText?.let { edit ->
                edit.post {
                    edit.requestFocus()
                    edit.performClick()
                    val imm =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    val shown = imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT)
                    shown//TODO: ime not showing
                }
            }
        }
    }
}

fun MaterialDialogs.getInputLayout(dialog: DialogInterface): TextInputLayout {
    return (dialog as AlertDialog).findViewById(R.id.text_input_layout)!!
}

fun MaterialDialogs.getInputField(dialog: DialogInterface): TextInputEditText {
    return (dialog as AlertDialog).findViewById(R.id.edit_text)!!
}