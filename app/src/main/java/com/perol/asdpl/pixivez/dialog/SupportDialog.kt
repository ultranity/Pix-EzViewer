package com.perol.asdpl.pixivez.dialog

import android.app.Dialog
import android.content.*
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.ThanksAdapter
import com.perol.asdpl.pixivez.networks.SharedPreferencesServices
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*


class SupportDialog : DialogFragment() {

    val ThanksArray = listOf(
        "**涛:支持功能开发\n本地识别图片pid批量重命名 进度50%",
        "*蒂",
        "C*a",
        "H*m",
        "Y*H",
        "**A",
        "**涵",
        "*喵",
        "*宋",
        "*恒",
        "*苦",
        "*枣",
        "*少"
    )
    private fun gotoWeChat() {
        val intent = Intent("com.tencent.mm.action.BIZSHORTCUT")
        intent.setPackage("com.tencent.mm")
        intent.putExtra("LauncherUI.From.Scaner.Shortcut", true)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        )
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "你好像没有安装微信", Toast.LENGTH_SHORT).show()
        }
    }
    private fun gotoAliPay() {
        val uri =
            Uri.parse("alipayqr://platformapi/startapp?saId=10000007")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        )
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "你好像没有安装支付宝", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendPictureStoredBroadcast(file: File) {
        runBlocking {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            MediaScannerConnection.scanFile(
                PxEZApp.instance, arrayOf(path.absolutePath), arrayOf(
                    MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(
                            file.extension
                        )
                )
            ) { _, _ ->
            }
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val calendar = Calendar.getInstance()
            SharedPreferencesServices.getInstance().setInt("lastsupport",
                calendar.get(Calendar.DAY_OF_YEAR)*100+calendar.get(Calendar.HOUR_OF_DAY))

            val totaldownloadcount = SharedPreferencesServices.getInstance().getInt("totaldownloadcount", File(PxEZApp.storepath).list()?.size?:0)
            val builder = MaterialAlertDialogBuilder(requireActivity())
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_thanks, null)
            val re = view.findViewById<RecyclerView>(R.id.list)
            val msg = it.layoutInflater.inflate(R.layout.dialog_weixin_ultranity, null)
            val static = msg.findViewById<TextView>(R.id.textStatic)
            val spannableString = SpannableString(getString(R.string.support_static).format(totaldownloadcount))
            val colorSpan = ForegroundColorSpan(Color.parseColor("#F44336"))
            spannableString.setSpan(
                colorSpan,
                getString(R.string.support_static).length - 6,
                spannableString.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            static.text = spannableString
            re.adapter = ThanksAdapter(R.layout.simple_list_item, ThanksArray).apply {
                setHeaderView(msg)
            }
            re.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

            builder.setTitle(getString(R.string.support_popup_title))
                    .setView(view).setNegativeButton(R.string.wechat)
                    { _,_->
                        gotoWeChat()
                        SharedPreferencesServices.getInstance().setInt("supports", SharedPreferencesServices.getInstance().getInt("supports" ) +1 )
                    }
                    .setPositiveButton(R.string.ali)
                    { _,_->
                    val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip: ClipData = ClipData.newPlainText("simple text",
                        String(
                            Base64.decode("I+e7meaIkei9rOi0piPplb/mjInlpI3liLbmraTmnaHmtojmga/vvIzljrvmlK/ku5jlrp3pppbp\n" +
                                "obXov5vooYzmkJzntKLnspjotLTljbPlj6/nu5nmiJHovazotKZUaFlXMlhqNzBTdyM=\n",
                                Base64.DEFAULT)
                        )
                    )
                    clipboard.setPrimaryClip(clip)
                    gotoAliPay()
                        SharedPreferencesServices.getInstance().setInt("supports", SharedPreferencesServices.getInstance().getInt("supports" ) +1 )
                }.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}