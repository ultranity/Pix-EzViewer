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
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.PixiVisionAdapter
import com.perol.asdpl.pixivez.databinding.ActivityPixivisionBinding
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.viewmodel.PixivisionModel

class PixivsionActivity : RinkActivity() {
    private lateinit var binding: ActivityPixivisionBinding
    private lateinit var viewmodel: PixivisionModel
    private lateinit var pixiVisionAdapter: PixiVisionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPixivisionBinding.inflate(layoutInflater)
        viewmodel = ViewModelProvider(this)[PixivisionModel::class.java]
        setContentView(binding.root)
        setSupportActionBar(binding.toobarPixivision)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        initbind()
        viewmodel.onRefreshListener()
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
        viewmodel.banners.observe(this)  {
            if (it != null) {
                pixiVisionAdapter.setNewInstance(it)
            } else {
                pixiVisionAdapter.loadMoreFail()
            }
        }
        viewmodel.addbanners.observe(this) {
            if (it != null) {
                pixiVisionAdapter.addData(it)
            } else {
                pixiVisionAdapter.loadMoreFail()
            }
        }
        viewmodel.nextPixivisonUrl.observe(this) {
            if (::pixiVisionAdapter.isInitialized) {
                if (it == null) {
                    pixiVisionAdapter.loadMoreModule.loadMoreEnd()
                } else {
                    pixiVisionAdapter.loadMoreModule.loadMoreComplete()
                }
            }
        }
        binding.recyclerviewPixivision.layoutManager = LinearLayoutManager(applicationContext)
        binding.recyclerviewPixivision.adapter = pixiVisionAdapter
        pixiVisionAdapter.addChildClickViewIds(R.id.imageView_pixivision)
        pixiVisionAdapter.setOnItemChildClickListener { adapter, view, position ->
            val intent = Intent(this@PixivsionActivity,
                if (PreferenceManager.getDefaultSharedPreferences(PxEZApp.instance).getBoolean("disableproxy",false))
                    WebViewActivity::class.java else OKWebViewActivity::class.java)
            intent.putExtra("url", pixiVisionAdapter.data[position].article_url)
            startActivity(intent)
        }
    }
}
