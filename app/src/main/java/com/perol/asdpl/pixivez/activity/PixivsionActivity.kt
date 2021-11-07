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
import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.PixiVisionAdapter
import com.perol.asdpl.pixivez.databinding.ActivityPixivisionBinding
import com.perol.asdpl.pixivez.networks.SharedPreferencesServices
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.SpotlightResponse
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class PixivsionActivity : RinkActivity() {


    lateinit var sharedPreferencesServices: SharedPreferencesServices
    private var Authorization: String? = null
    private var Nexturl: String? = null
    private var data: SpotlightResponse? = null
    private val retrofitRepository  = RetrofitRepository.getInstance()
    private lateinit var binding: ActivityPixivisionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPixivisionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toobarPixivision)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        sharedPreferencesServices = SharedPreferencesServices.getInstance()
        initbind()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                this.finish() // back button
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initbind() {
            retrofitRepository.getPixivison("all")
                .subscribe(object : Observer<SpotlightResponse> {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onNext(spotlightResponse: SpotlightResponse) {
                        data = spotlightResponse
                        Nexturl = spotlightResponse.next_url
                        val pixiviSionAdapter = PixiVisionAdapter(R.layout.view_pixivision_item, spotlightResponse.spotlight_articles, this@PixivsionActivity)
                        binding.recyclerviewPixivision.layoutManager = LinearLayoutManager(applicationContext)
                        binding.recyclerviewPixivision.adapter = pixiviSionAdapter
                        pixiviSionAdapter.loadMoreModule?.setOnLoadMoreListener {
                                retrofitRepository.getNextPixivisionArticles(
                                    Nexturl!!
                                )
                                    .subscribe(object : Observer<SpotlightResponse> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onNext(spotlightResponse: SpotlightResponse) {
                                            Nexturl = spotlightResponse.next_url
                                            pixiviSionAdapter.addData(spotlightResponse.spotlight_articles)
                                        }

                                        override fun onError(e: Throwable) {
                                            pixiviSionAdapter.loadMoreModule?.loadMoreFail()
                                        }

                                        override fun onComplete() {
                                            pixiviSionAdapter.loadMoreModule?.loadMoreComplete()
                                        }
                                    })
                        }
                        pixiviSionAdapter.addChildClickViewIds(R.id.imageView_pixivision)
                        pixiviSionAdapter.setOnItemChildClickListener { adapter, view, position ->
                            val intent = Intent(this@PixivsionActivity, WebViewActivity::class.java)
                            intent.putExtra("url", data!!.spotlight_articles[position].article_url)
                            startActivity(intent)
                        }
                    }

                    override fun onError(e: Throwable) {
                    }

                    override fun onComplete() {

                    }
                })
    }
}
