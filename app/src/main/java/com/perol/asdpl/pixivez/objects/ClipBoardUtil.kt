package com.perol.asdpl.pixivez.objects

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Origin:https://github.com/RikkaW/SearchByImage.git
 *
 *
 * Created by Rikka on 2015/12/18.
 */
object ClipBoardUtil {
    private lateinit var mApplication: Application
    fun init(application: Application) {
        mApplication = application
    }

    @JvmOverloads
    fun putTextIntoClipboard(context: Context, text: String, showHint: Boolean = true) {
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("copy text", text)
        clipboardManager.setPrimaryClip(clipData)
        if (showHint) {
            Toasty.info(context, text)
        }
    }

    fun getClipboardContent(context: Context): String? {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val data = cm.primaryClip
        if (data != null && data.itemCount > 0) {
            val item = data.getItemAt(0)
            if (item != null) {
                val sequence = item.coerceToText(context)
                if (sequence != null) {
                    return sequence.toString()
                }
            }
        }
        return null
    }
}
