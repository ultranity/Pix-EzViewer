/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
 * Copyright (c) 2019 Perol_Notsfsssf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package com.perol.asdpl.pixivez.ui.home.pixivision

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.KotlinUtil.launchCatching
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.data.RetrofitRepository
import com.perol.asdpl.pixivez.databinding.ActivitySpotlightBinding
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.objects.Spotlight
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.regex.Matcher
import java.util.regex.Pattern

// parsed pixvision - for now use webview PixivisionActivity instead
class SpotlightActivity : RinkActivity() {
    private val reurls = HashSet<Int>()
    private val retrofit = RetrofitRepository.getInstance()
    private var spotlightAdapter: SpotlightAdapter? = null
    private val list = ArrayList<Spotlight>()
    private var url = ""
    private var num = 0

    private lateinit var binding: ActivitySpotlightBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpotlightBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getData()
    }

    private fun getData() {
        val intent = intent
        url = intent.getStringExtra("url").toString()
        binding.textViewDesc.setOnClickListener {
            val intent = Intent()
            intent.action = "android.intent.action.VIEW"
            val contentUrl = Uri.parse(url)
            intent.data = contentUrl
            startActivity(intent)
        }
        val local = LanguageUtil.langToLocale(PxEZApp.language)
        lifecycleScope.launchCatching({
            val builder = OkHttpClient.Builder()
            val okHttpClient = builder.build()
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept-Language", "${local.language}_${local.country}")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val result = response.body!!.string()
            result
        }, {
            val urls = getImgStra(it)
            val doc = Jsoup.parse(it)
            val elements1 = doc.select("div[class=am__description _medium-editor-text]")
            val articletitle = ""
            val stringBuilder = StringBuilder(articletitle)
            for (element in elements1) {
                stringBuilder.append(element.text())
            }
            binding.textViewDesc.text = stringBuilder
            for (string in urls) {
                if (!string.contains("svg") && string.contains("https://www.pixiv.net/member_illust.php?")) {
                    reurls.add(
                        Integer.valueOf(
                            string.replace(
                                "https://www.pixiv.net/member_illust.php?mode=medium&illust_id=",
                                ""
                            )
                        )
                    )
                }
            }
            getSpolights()
        }, { })
    }

    private fun getSpolights() = lifecycleScope.launch{
        for (id in reurls) {
            retrofit.api.getIllust(id.toLong()).let {
                val name = it.illust.user.name
                val title = it.illust.title
                list.add(Spotlight(title, name, it.illust))
            }
        }
        spotlightAdapter = SpotlightAdapter(R.layout.view_spotlight_item, list)
        binding.recyclerviewSpotlight.layoutManager =
            LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)

        binding.recyclerviewSpotlight.adapter = spotlightAdapter
        binding.recyclerviewSpotlight.isNestedScrollingEnabled = false
    }

    companion object {

        fun getImgStr(htmlStr: String): Set<String> {
            val pics = HashSet<String>()
            var img = ""
            //     String regEx_img = "<img.*src=(.*?)[^>]*?>"; //图片链接地址
            val regEx_img = "<img.*src\\s*=\\s*(.*?)[^>]*?>"
            val p_image: Pattern = Pattern.compile(regEx_img, Pattern.CASE_INSENSITIVE)
            val m_image: Matcher = p_image.matcher(htmlStr)
            while (m_image.find()) {
                img = m_image.group()
                val m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img)
                while (m.find()) {
                    m.group(1)?.let { pics.add(it) }
                }
            }
            return pics
        }

        fun getImgStra(htmlStr: String): Set<String> {
            val pics = HashSet<String>()
            val doc = Jsoup.parse(htmlStr)
            val elements = doc.body().allElements
            for (element in elements) {
                pics.add(element.select("a").attr("href"))
            }
            return pics
        }
    }
}
