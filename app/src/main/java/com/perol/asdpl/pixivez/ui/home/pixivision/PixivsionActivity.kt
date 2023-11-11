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
import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.GridLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.base.factory.sharedViewModel
import com.perol.asdpl.pixivez.databinding.ActivityPixivisionBinding
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.OKWebViewActivity
import com.perol.asdpl.pixivez.ui.WebViewActivity

class PixivsionActivity : RinkActivity() {
    private lateinit var binding: ActivityPixivisionBinding
    private val viewmodel: PixivisionModel by sharedViewModel("pixivision")
    private lateinit var pixiVisionAdapter: PixiVisionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPixivisionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toobarPixivision)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        initbind()
        if (viewmodel.data.value == null) {
            viewmodel.onRefreshListener()
        }
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
        pixiVisionAdapter = PixiVisionAdapter(
            R.layout.view_pixivision_item,
            null
        )
        viewmodel.data.observe(this) {
            if (it != null) {
                pixiVisionAdapter.setNewInstance(it)
            } else {
                pixiVisionAdapter.loadMoreFail()
            }
        }
        viewmodel.dataAdded.observe(this) {
            if (it != null) {
                pixiVisionAdapter.addData(it)
            } else {
                pixiVisionAdapter.loadMoreFail()
            }
        }
        viewmodel.nextUrl.observe(this) {
            if (::pixiVisionAdapter.isInitialized) {
                if (it == null) {
                    pixiVisionAdapter.loadMoreEnd()
                } else {
                    pixiVisionAdapter.loadMoreComplete()
                }
            }
        }
        binding.recyclerviewPixivision.layoutManager =
            GridLayoutManager(this, resources.configuration.orientation)
        binding.recyclerviewPixivision.adapter = pixiVisionAdapter
        pixiVisionAdapter.addChildClickViewIds(R.id.imageView_pixivision)
        pixiVisionAdapter.setOnItemChildClickListener { adapter, view, position ->
            val intent = Intent(
                this@PixivsionActivity,
                if (PxEZApp.instance.pre.getBoolean("dnsProxy", false)) {
                    OKWebViewActivity::class.java
                } else {
                    WebViewActivity::class.java
                }
            ).setAction("your.custom.action")
            intent.putExtra("url", pixiVisionAdapter.data[position].article_url)
            startActivity(intent)
        }
    }
}
