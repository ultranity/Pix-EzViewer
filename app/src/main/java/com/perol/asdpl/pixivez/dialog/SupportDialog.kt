package com.perol.asdpl.pixivez.dialog

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.SettingActivity
import com.perol.asdpl.pixivez.adapters.ThanksAdapter
import com.perol.asdpl.pixivez.databinding.DialogThanksBinding
import com.perol.asdpl.pixivez.databinding.DialogWeixinUltranityBinding
import com.perol.asdpl.pixivez.repository.UserInfoSharedPreferences
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Calendar

class SupportDialog : BaseVBDialogFragment<DialogThanksBinding>() {

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
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            MediaScannerConnection.scanFile(
                PxEZApp.instance,
                arrayOf(path.absolutePath),
                arrayOf(
                    MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(
                            file.extension
                        )
                )
            ) { _, _ ->
            }
        }
    }

    override fun onCreateDialogBinding(builder: MaterialAlertDialogBuilder) {
        var full = true
        arguments?.let {
            full = it.getBoolean(ARG_PARAM1)
        }
    val calendar = Calendar.getInstance()
    val userInfoSharedPreferences = UserInfoSharedPreferences.getInstance()
    userInfoSharedPreferences.setInt(
        "lastsupport",
        calendar.get(Calendar.DAY_OF_YEAR) * 24 + calendar.get(Calendar.HOUR_OF_DAY)
    )
    val totaldownloadcount = userInfoSharedPreferences.getInt("totaldownloadcount", File(PxEZApp.storepath).list()?.size ?: 0)

    val bindingWX = DialogWeixinUltranityBinding.inflate(layoutInflater)
    val spannableString = SpannableString(getString(R.string.support_static).format(totaldownloadcount))
    val colorSpan = ForegroundColorSpan(Color.parseColor("#F44336"))
    if (!full) {
        bindingWX.qrCode.visibility = View.GONE
    }
    spannableString.setSpan(
        colorSpan,
        getString(R.string.support_static).length - 6,
        spannableString.length,
        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
    )
    bindingWX.textStatic.text = spannableString
    val array = requireContext().resources.openRawResource(R.raw.thanks_list).reader().readLines()
    binding.list.adapter = ThanksAdapter(R.layout.simple_list_item, array).apply {
        setHeaderView(bindingWX.root)
    }
    binding.list.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

    builder
        .setTitle(getString(R.string.support_popup_title))
        .apply {
            if (full) {
                setNegativeButton(R.string.wechat) { _, _ ->
                    gotoWeChat()
                    userInfoSharedPreferences.setInt(
                        "lastsupport",
                        calendar.get(Calendar.DAY_OF_YEAR) * 24 + 240 + calendar.get(Calendar.HOUR_OF_DAY)
                    )
                    userInfoSharedPreferences.setInt(
                        "supports",
                        userInfoSharedPreferences.getInt("supports") + 1
                    )
                }
                setPositiveButton(R.string.ali) { _, _ ->
                    val clipboard =
                        requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip: ClipData = ClipData.newPlainText(
                        "simple text",
                        String(
                            Base64.decode(
                                "I+e7meaIkei9rOi0piPplb/mjInlpI3liLbmraTmnaHmtojmga/vvIzljrvmlK/ku5jlrp3pppbp\n" +
                                    "obXov5vooYzmkJzntKLnspjotLTljbPlj6/nu5nmiJHovazotKZUaFlXMlhqNzBTdyM=\n",
                                Base64.DEFAULT
                            )
                        )
                    )
                    clipboard.setPrimaryClip(clip)
                    gotoAliPay()
                    userInfoSharedPreferences.setInt(
                        "lastsupport",
                        calendar.get(Calendar.DAY_OF_YEAR) * 24 + 240 + calendar.get(Calendar.HOUR_OF_DAY)
                    )
                    userInfoSharedPreferences.setInt(
                        "supports",
                        userInfoSharedPreferences.getInt("supports") + 1
                    )
                }
            }
            else {
                setPositiveButton(R.string.supporttitle) { _, _ ->
                    startActivity(
                        Intent(requireActivity(), SettingActivity::class.java).apply {
                            putExtra("page", 1)
                        }
                    )
                }
            }
        }
    }

    companion object {

        private const val ARG_PARAM1 = "full"

        @JvmStatic
        fun newInstance(param1: Boolean) =
            SupportDialog().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_PARAM1, param1)
                }
            }
    }
}
