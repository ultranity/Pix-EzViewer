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
        "**涛 x20",
        "*蒂",
        "C*a",
        "H*m",
        "Y*H",
        "**A",
        "**涵 x2",
        "*喵 x2",
        "*宋",
        "*恒",
        "*苦",
        "*枣",
        "*少：小白条沉浸",
        "*菟",
        "*心",
        "*手",
        "*面",
        "*土",
        "L*Q",
        "C*g",
        "*J",
        "*m",
        "*毛",
        "*🍁",
        "*。 x3",
        "*梦",
        "*苦",
        "v*t",
        "*奇",
        "**豪",
        "*政",
        "**信",
        "*泰",
        "*磊",
        "*江",
        "l*t",
        "*诚",
        "*道",
        "K*n",
        "*雨",
        "[*]",
        "T*i",
        "*星",
        "D*n",
        "*飞",
        "*？",
        "*姬",
        "*昏",
        "D*r",
        "f*8",
        "*所",
        "y*h",
        "*尧",
        "*丸",
        "*生",
        "*寻",
        "*鱼",
        "*猫",
        "*.",
        "*帅 20",
        "**——",
        "**辉",
        "**帅",
        "*N",
        "*年",
        "*孑",
        "C*.",
        "a*e",
        "*中",
        "*?",
        ".*.",
        "N*o",
        "混*r",
        "*辰",
        "*奔",
        "*放",
        "**—— 30",
        "*俊",
        "**强",
        "**林",
        "**益",
        "*辰",
        "**涛",
        "破风繁星",
        "*円",
        "*缘",
        "*熊",
        "*所",
        "*月",
        "*乐",
        "*昏",
        "I*c",
        "*星",
        "*。",
        "*明",
        "*神",
        "*",
        "*ナ",
        "*咕",
        "C*t",
        "**涵",
        "*鑫",
        "**韬 30",
        "*杰 50",
        "**涛",
        "**权",
        "*远",
        "**鹏",
        "**波",
        "*旭",
        "*阳",
        "**宇",
        "**航",
        "**烨 6.66",
        "**程",
        "板板（**锐） 20",
        "**鑫",
        "**韬",
        "**帅",
        "**阁",
        "**强",
        "*钢",
        "**奇",
        "**仪 20",
        "**禧",
        "**民",
        "*莉",
        "**龙",
        "**磊",
        "**烨",
        "*ド",
        "*鸣",
        "*王",
        "*识",
        "*韬",
        "*绫",
        "M*H",
        "f*t",
        "*漫",
        "*",
        "*糖",
        "*落",
        "*海",
        "D*n",
        "a*k",
        "*年",
        "*w",
        "x*x",
        "*撇",
        "*💵",
        "*手",
        "*人",
        "*波 50",
        "k*x",
        "*宇",
        "*!",
        "*X x2",
        "*苏",
        "*帆",
        "*梦",
        "R*d",
        "*散",
        "*权",
        "*权",
        "**健 x3",
        "**哲",
        "**璇",
        "2021:",
        "*风",
        "*尔",
        "+*+",
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
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
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
            val sharedPreferencesServices = SharedPreferencesServices.getInstance()
            sharedPreferencesServices.setInt("lastsupport",
                calendar.get(Calendar.DAY_OF_YEAR)*100+calendar.get(Calendar.HOUR_OF_DAY))
            val totaldownloadcount = sharedPreferencesServices.getInt("totaldownloadcount", File(PxEZApp.storepath).list()?.size?:0)
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
                        sharedPreferencesServices.setInt("lastsupport",
                            calendar.get(Calendar.DAY_OF_YEAR)*100+500+calendar.get(Calendar.HOUR_OF_DAY))
                        sharedPreferencesServices.setInt("supports", sharedPreferencesServices.getInt("supports" ) +1 )
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
                    sharedPreferencesServices.setInt("lastsupport",
                        calendar.get(Calendar.DAY_OF_YEAR)*100+500+calendar.get(Calendar.HOUR_OF_DAY))
                    sharedPreferencesServices.setInt("supports", sharedPreferencesServices.getInt("supports" ) +1 )
                }.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}