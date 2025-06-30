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

package com.perol.asdpl.pixivez.ui.pic

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.chrynan.parcelable.core.getParcelable
import com.chrynan.parcelable.core.putParcelable
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.databinding.ActivityPictureBinding
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.services.PxEZApp


class PictureActivity : RinkActivity() {
    companion object {
        private const val ARG_ILLUSTOBJ = "illustobj"
        private const val ARG_ILLUSTID = "illustid"
        private const val ARG_ILLUSTIDLIST = "illustidlist"
        private const val ARG_POSITION = "position"
        const val ARG_THUMB_HINT = "SquareThumbHint"
        fun start(
            context: Context, id: Int, arrayList: IntArray? = IntArray(1) { id },
            options: Bundle? = null
        ) {
            val extras = Bundle()
            extras.putIntArray(ARG_ILLUSTIDLIST, arrayList)
            extras.putInt(ARG_ILLUSTID, id)
            val intent =
                Intent(context, PictureActivity::class.java).setAction("pic.view")
            intent.putExtras(extras)
            context.startActivity(intent, options)
        }

        fun start(
            context: Context,
            id: Int,
            position: Int,
            //limit: Int = 30,
            squareThumbHint: Boolean = false,
            options: Bundle? = null
        ) {
            val extras = Bundle()
            // bundle.putInt(ARG_POSITION, position - max(position - limit, 0))
            extras.putInt(ARG_POSITION, position)
            extras.putInt(ARG_ILLUSTID, id)
            if (squareThumbHint) extras.putBoolean(ARG_THUMB_HINT, true)
            val intent =
                Intent(context, PictureActivity::class.java).setAction("pic.view")
            intent.putExtras(extras)
            context.startActivity(intent, options)
        }

        fun start(
            context: Context,
            illust: Illust,
        ) {
            val extras = Bundle()
            extras.putParcelable(ARG_ILLUSTOBJ, illust)
            extras.putInt(ARG_ILLUSTID, illust.id)
            val intent =
                Intent(context, PictureActivity::class.java).setAction("pic.view")
            intent.putExtras(extras)
            context.startActivity(intent)
        }
    }

    private var illustId: Int = 0
    private var illustIdList: IntArray? = null
    private var illustList: List<Illust>? = null
    private var nowPosition: Int = 0

    private lateinit var binding: ActivityPictureBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (PxEZApp.instance.pre.getBoolean("needactionbar", false)) {
            setSupportActionBar(binding.toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
        } else {
            binding.toolbar.visibility = View.GONE
        }
        if (PxEZApp.instance.pre.getBoolean("needstatusbar", false)) {
            binding.rootContainer.fitsSystemWindows = true
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            if (ThemeUtil.isDarkMode(this)) window.statusBarColor = Color.TRANSPARENT
        } else {
            window.decorView.fitsSystemWindows = false
            //window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            //window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            // window.navigationBarColor = Color.TRANSPARENT
        }

        val bundle = this.intent.extras!!
        illustId = bundle.getInt(ARG_ILLUSTID)
        nowPosition = bundle.getInt(ARG_POSITION, 0)
        val picturePagerAdapter: PicturePagerAdapter
        when {
            bundle.containsKey(ARG_ILLUSTIDLIST) -> {
                illustIdList = bundle.getIntArray(ARG_ILLUSTIDLIST)
                nowPosition = illustIdList!!.indexOf(illustId)
                picturePagerAdapter =
                    PicturePagerAdapter(supportFragmentManager, illustIdList!!)
            }

            DataHolder.checkIllustList(nowPosition, illustId) -> {
                illustList = DataHolder.getIllustList() // ?.toList()
                illustIdList = if (illustList != null) {
                    illustList!!.map { it.id }.toIntArray()
                } else {
                    IntArray(1) { illustId }
                }

                picturePagerAdapter =
                    PicturePagerAdapter(supportFragmentManager, illustIdList, illustList)
                DataHolder.picPagerAdapter = binding.viewpagePicture.adapter
            }

            else -> {
                illustIdList = IntArray(1) { illustId }
                nowPosition = 0 // illustIdList!!.indexOf(illustId)
                picturePagerAdapter = bundle.getParcelable<Illust>(ARG_ILLUSTOBJ)?.let {
                    PicturePagerAdapter(supportFragmentManager, illustIdList, arrayListOf(it))
                } ?: PicturePagerAdapter(supportFragmentManager, illustIdList)
            }
        }
        binding.viewpagePicture.adapter = picturePagerAdapter
        binding.viewpagePicture.currentItem = nowPosition
        binding.viewpagePicture.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                nowPosition = position
            }
        })
        supportPostponeEnterTransition()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                finishAfterTransition()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        DataHolder.picPagerAdapter = null
        super.onDestroy()
    }
}
