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

package com.perol.asdpl.pixivez.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.SpotlightAdapter
import com.perol.asdpl.pixivez.databinding.ActivitySpotlightBinding
import com.perol.asdpl.pixivez.networks.SharedPreferencesServices
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.objects.Spotlight
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.IllustDetailResponse
import com.perol.asdpl.pixivez.services.PxEZApp
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

// parsed pixvision - for now use webview PixivisionActivity instead
class SpotlightActivity : RinkActivity() {
    private val reurls = HashSet<Int>()
    private val retrofitRepository = RetrofitRepository.getInstance()
    private var sharedPreferencesServices: SharedPreferencesServices? = null
    private var spotlightAdapter: SpotlightAdapter? = null
    private val list = ArrayList<Spotlight>()
    private var url = ""
    private var num = 0

    private lateinit var binding: ActivitySpotlightBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpotlightBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferencesServices = SharedPreferencesServices.getInstance()
        getData()
    }

    private fun getData() {
        val intent = intent
        url = intent.getStringExtra("url").toString()
        binding.textViewTest.setOnClickListener {
            val intent = Intent()
            intent.action = "android.intent.action.VIEW"
            val contentUrl = Uri.parse(url)
            intent.data = contentUrl
            startActivity(intent)
        }
        val local = LanguageUtil.langToLocale(PxEZApp.language)
        Observable.create { emitter ->
            val builder = OkHttpClient.Builder()
            val okHttpClient = builder.build()
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept-Language", "${local.language}_${local.country}")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val result = response.body!!.string()
            emitter.onNext(result)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<String> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(s: String) {
                    val urls = getImgStra(s)

                    val doc = Jsoup.parse(s)

                    val elements1 = doc.select("div[class=am__description _medium-editor-text]")
                    val articletitle = ""
                    val stringBuilder = StringBuilder(articletitle)

                    for (element in elements1) {
                        stringBuilder.append(element.text())
                    }
                    binding.textViewTest.text = stringBuilder
                    for (string in urls) {
                        if (!string.contains("svg") && string.contains("https://www.pixiv.net/member_illust.php?")) {
                            reurls.add(Integer.valueOf(string.replace("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=", "")))
                        }
                    }
                    getspolight()
                }

                override fun onError(e: Throwable) {
                }

                override fun onComplete() {
                }
            })
    }

    private fun getspolight() {
        Observable.create { emitter ->
            num = 0
            for (id in reurls) {
                num += 1
                retrofitRepository.getIllust(id.toLong())
                    .subscribe(object : Observer<IllustDetailResponse> {
                        override fun onSubscribe(d: Disposable) {
                        }

                        override fun onNext(illustDetailResponse: IllustDetailResponse) {
                            emitter.onNext(illustDetailResponse)
                        }

                        override fun onError(e: Throwable) {
                        }

                        override fun onComplete() {
                        }
                    })
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<IllustDetailResponse> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(illustDetailResponse: IllustDetailResponse) {
                    val name = illustDetailResponse.illust.user.name
                    val title = illustDetailResponse.illust.title
                    list.add(Spotlight(title, name, illustDetailResponse.illust.user.profile_image_urls.medium, illustDetailResponse.illust.image_urls.large, illustDetailResponse.illust.id.toString(), illustDetailResponse.illust.user.id))
                    if (num == reurls.size) {
                        onComplete()
                    }
                }

                override fun onError(e: Throwable) {
                }

                override fun onComplete() {
                    spotlightAdapter = SpotlightAdapter(R.layout.view_spotlight_item, list)
                    binding.recyclerviewSpotlight.layoutManager =
                        LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)

                    binding.recyclerviewSpotlight.adapter = spotlightAdapter
                    binding.recyclerviewSpotlight.isNestedScrollingEnabled = false
                }
            })
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
