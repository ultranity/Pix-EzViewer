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

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.databinding.ActivitySaucenaoBinding
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.SaucenaoService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.util.*

class SaucenaoActivity : RinkActivity() {

    private val IMAGE = 1
    private lateinit var binding: ActivitySaucenaoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySaucenaoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fab.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(intent, IMAGE)
        }
        val httpLoggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Log.d("aaa", "a:$message")
            }
        })
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val builder1 = OkHttpClient.Builder()
        builder1.addInterceptor(object : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader(
                        "User-Agent",
                        "PixivAndroidApp/5.0.234 (Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MODEL})"
                    )
                    .addHeader("referer", "https://app-api.pixiv.net/")
                val request = requestBuilder.build()
                return chain.proceed(request)
            }
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
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
        binding.ssl.isChecked = false
        api = service.create(SaucenaoService::class.java)

        if (intent != null) {
            val action = intent.action
            val type = intent.type
            if (action != null && type != null) {
                if (action == Intent.ACTION_SEND && type.startsWith("image/")) {
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

                    if (uri != null) {
                        val parcelFileDescriptor =
                            contentResolver.openFileDescriptor(uri, "r")
                        val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
                        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                        parcelFileDescriptor.close()
                        val file = File(cacheDir, Date().time.toString() + ".jpg")
                        val out = file.outputStream()
                        runBlocking {
                            withContext(Dispatchers.IO) {
                                image.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                out.flush()
                                out.close()
                            }
                        }
                        Toasty.success(this, getString(R.string.saucenao_compress_success), Toast.LENGTH_SHORT).show()
                        val builder = MultipartBody.Builder()
                        builder.setType(MultipartBody.FORM)
//                        val baos = ByteArrayOutputStream();
//                        if (file.length() > 10000000L) {
//                            val options = BitmapFactory.Options()
//                            options.inSampleSize = 4
//                            val decodeFile = BitmapFactory.decodeFile(path, options)
//                            builder.addFormDataPart("file", file.name, object : RequestBody() {
//                                override fun contentType(): MediaType? =
//                                    "image/jpeg".toMediaTypeOrNull()
//
//                                override fun writeTo(sink: BufferedSink) {
//                                    decodeFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//                                }
//
//                            })
//                            Log.d("bitmap", "large file")
//                        } else
                        builder.addFormDataPart(
                            "file",
                            file.name,
                            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        )
                        api.searchpicforresult(builder.build().part(0)).subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread()).subscribe(
                                {
                                    Toasty.success(
                                        PxEZApp.instance,
                                        getString(R.string.saucenao_upload_success),
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    if (file.exists()) {
                                        file.delete()
                                    }
                                    tryToParseHtml(it.string())
                                },
                                {
                                    Toasty.error(PxEZApp.instance, getString(R.string.saucenao_upload_error) + it.message).show()
                                    if (file.exists()) {
                                        file.delete()
                                    }
                                },
                                {
                                }
                            )
                    }
                }
            }
        }
    }

    lateinit var api: SaucenaoService
    private fun trytosearch(path: String) {
        Toasty.success(this, getString(R.string.saucenao_compress_success), Toast.LENGTH_SHORT)
            .show()
        val file = File(path)
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        val body = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        builder.addFormDataPart("file", file.name, body)
        api.searchpicforresult(builder.build().part(0)).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe({
                Toasty.success(
                    this,
                    getString(R.string.saucenao_upload_success),
                    Toast.LENGTH_SHORT
                ).show()
                tryToParseHtml(it.string())
            }, { Toasty.error(this, getString(R.string.saucenao_upload_error) + it.message).show() }, {
            })
    }

    private fun tryToParseHtml(string: String) {
        val arrayList = ArrayList<Long>()
        runBlocking {
            val doc = Jsoup.parse(string)
            val el = doc.select("a[href]")
            for (i in el.indices) {
                val string = el[i].attr("href")
                Log.d("w", string)
                if (string.startsWith("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=")) {
                    val id = string.replace(
                        "https://www.pixiv.net/member_illust.php?mode=medium&illust_id=",
                        ""
                    ).toLong()
                    arrayList.add(id)
                }
            }
        }
        val bundle = Bundle()

        if (arrayList.isNotEmpty()) {
            val it = arrayList.toLongArray()
            Toasty.success(this, "id: " + it[0].toString(), Toast.LENGTH_LONG).show()
            bundle.putLongArray("illustidlist", it)
            bundle.putLong("illustid", it[0])
            val intent2 = Intent(applicationContext, PictureActivity::class.java)
            intent2.putExtras(bundle)
            startActivity(intent2)
        }
        else {
            Glide.with(this).load(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.buzhisuocuo
                )
            ).into(binding.imageview)
            // prevent CORS
            binding.webview.loadDataWithBaseURL("https://saucenao.com", string, "text/html", "UTF-8", "")
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

    var path: String? = null

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage = data.data
            val filePathColumns = arrayOf(MediaStore.Images.Media.DATA)
            val c = contentResolver.query(selectedImage!!, filePathColumns, null, null, null)
            c!!.moveToFirst()
            val columnIndex = c.getColumnIndex(filePathColumns[0])
            val imagePath = c.getString(columnIndex)
            trytosearch(imagePath)
            c.close()
        }
    }
}
