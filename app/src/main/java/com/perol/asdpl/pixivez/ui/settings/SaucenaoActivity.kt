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

package com.perol.asdpl.pixivez.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.KotlinUtil.launchCatching
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivitySaucenaoBinding
import com.perol.asdpl.pixivez.networks.RestClient
import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.Storage
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.SaucenaoService
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup
import retrofit2.Retrofit
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetAddress
import java.util.Date

class SaucenaoActivity : RinkActivity() {

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let(::search)
        }
    private lateinit var binding: ActivitySaucenaoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySaucenaoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fab.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
            CrashHandler.instance.d(
                "aaa",
                "a:$message"
            )
        }
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val builder1 = OkHttpClient.Builder()
        builder1.addInterceptor(Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("User-Agent", RestClient.UA)
                .addHeader("referer", "https://app-api.pixiv.net/")
            val request = requestBuilder.build()
            chain.proceed(request)
        }).addInterceptor(httpLoggingInterceptor).dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val list = ArrayList<InetAddress>()
                try {
                    list.addAll(Dns.SYSTEM.lookup(hostname))
                } catch (e: Exception) {
                    if (list.isEmpty()) {
                        list.add(InetAddress.getByName("45.32.0.237"))
                    }
                }

                return list
            }
        })
        val client = builder1.build()
        val service: Retrofit = Retrofit.Builder()
            .baseUrl(
                "https://saucenao.com"
            )
            .client(client)
            .build()
        binding.ssl.isChecked = false
        api = service.create(SaucenaoService::class.java)

        if (intent != null) {
            val action = intent.action
            val type = intent.type
            if (action != null && type != null) {
                if (action == Intent.ACTION_SEND && type.startsWith("image/")) {
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(::search)
                }
            }
        }
    }

    lateinit var api: SaucenaoService

    /** Re-encodes the picked/shared image as JPEG and uploads it. */
    private fun search(uri: Uri) {
        lifecycleScope.launchCatching({
            val jpeg = ByteArrayOutputStream().also { out ->
                val image = Storage.openInput(uri).use { BitmapFactory.decodeStream(it) }
                    ?: throw IOException("not an image: $uri")
                image.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }.toByteArray()
            withContext(Dispatchers.Main) {
                Toasty.success(this@SaucenaoActivity, R.string.saucenao_compress_success)
            }
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "${Date().time}.jpg", jpeg.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                .build()
            api.search(body.part(0))
        }, {
            Toasty.success(this, R.string.saucenao_upload_success)
            tryToParseHtml(it.string())
        }, {
            Toasty.error(this, getString(R.string.saucenao_upload_error) + it.message)
        })
    }

    private fun tryToParseHtml(string: String) {
        val arrayList = ArrayList<Int>()
        runBlocking {
            val doc = Jsoup.parse(string)
            val el = doc.select("a[href]")
            for (i in el.indices) {
                val url = el[i].attr("href")
                CrashHandler.instance.d("w", url)
                if (url.startsWith("https://www.pixiv.net/member_illust.php")) {
                    url.toUri().getQueryParameter("illust_id")
                        ?.toIntOrNull()?.let { arrayList.add(it) }
                } else if (url.startsWith("https://www.pixiv.net/artworks/")) {
                    url.replace("https://www.pixiv.net/artworks/", "")
                        .toIntOrNull()?.let { arrayList.add(it) }
                }
            }
        }
        val bundle = Bundle()

        if (arrayList.isNotEmpty()) {
            val it = arrayList.toIntArray()
            Toasty.success(this, "id: " + it[0].toString())
            PictureActivity.start(this, it[0], it)
        } else {
            Glide.with(this).load(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.buzhisuocuo
                )
            ).into(binding.imageview)
            // prevent CORS
            binding.webview.loadDataWithBaseURL(
                "https://saucenao.com",
                string,
                "text/html",
                "UTF-8",
                ""
            )
            binding.webview.settings.blockNetworkImage = false
            // webview.settings.javaScriptEnabled = true
            binding.webview.settings.userAgentString
            binding.webview.visibility = View.VISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
