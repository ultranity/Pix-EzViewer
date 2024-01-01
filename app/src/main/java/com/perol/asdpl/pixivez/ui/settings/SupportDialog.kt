package com.perol.asdpl.pixivez.ui.settings

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
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseDialogFragment
import com.perol.asdpl.pixivez.base.linear
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.databinding.DialogEmptyListBinding
import com.perol.asdpl.pixivez.databinding.DialogWeixinUltranityBinding
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.TimeUnit

class SupportDialog : BaseDialogFragment<DialogEmptyListBinding>() {

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
            Toasty.error(requireContext(), "你好像没有安装微信").show()
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
            Toasty.error(requireContext(), "你好像没有安装支付宝").show()
        }
    }

    private fun sendPictureStoredBroadcast(file: File) {
        runBlocking {
            val path =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
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
        AppDataRepo.pre.setLong(
            "last_time_ms",
            System.currentTimeMillis()
        )
        val totaldownloadcount = AppDataRepo.pre.getInt(
            "totaldownloadcount",
            File(PxEZApp.storepath).list()?.size ?: 0
        )

        val bindingWX = DialogWeixinUltranityBinding.inflate(layoutInflater)
        val spannableString =
            SpannableString(getString(R.string.support_static).format(totaldownloadcount))
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
        val array =
            requireContext().resources.openRawResource(R.raw.thanks_list).reader().readLines()
        binding.list.linear().adapter = ThanksAdapter(R.layout.simple_list_item, array).apply {
            setHeaderView(bindingWX.root)
        }
        builder
            .setTitle(R.string.support_popup_title)
            .setOnDismissListener {
                AppDataRepo.pre.setLong(
                    "last_time_ms",
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                )
            }
            .apply {
                if (full) {
                    setNegativeButton(R.string.wechat) { _, _ ->
                        gotoWeChat()
                        AppDataRepo.pre.setLong("last_time_ms", System.currentTimeMillis())
                        AppDataRepo.pre.setIntpp("supports")
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
                        AppDataRepo.pre.setLong("last_time_ms", System.currentTimeMillis())
                        AppDataRepo.pre.setIntpp("supports")
                    }
                } else {
                    setPositiveButton(R.string.supporttitle) { _, _ ->
                        startActivity(
                            Intent(
                                requireActivity(),
                                SettingsActivity::class.java
                            ).setAction("your.custom.action").apply {
                                putExtra("page", 1)
                            }
                        )
                    }
                }
            }
    }

    companion object {

        private val TAG: String = javaClass.simpleName
        private const val ARG_PARAM1 = "full"

        @JvmStatic
        fun newInstance(param1: Boolean) = SupportDialog().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_PARAM1, param1)
            }
        }

        fun checkTime(supportFragmentManager: FragmentManager): Boolean {
            val time: Long = System.currentTimeMillis()
            return if (BuildConfig.FLAVOR == "git" &&
                TimeUnit.MILLISECONDS.toDays(
                    (time - AppDataRepo.pre.getLong("last_time_ms", time))
                ) >= 90
            ) {
                SupportDialog().show(supportFragmentManager, TAG)
                false
            } else {
                AppDataRepo.pre
                    .setLong(
                        "last_time_ms",
                        AppDataRepo.pre.getLong("last_time_ms") - 1
                    )
                true
            }
        }
    }
}
