/*
 * MIT License
 *
 * Copyright (c) 2023 ultranity
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

package com.perol.asdpl.pixivez.ui.home.recom

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.core.view.setPadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.factory.sharedViewModel
import com.perol.asdpl.pixivez.core.PicListFilter
import com.perol.asdpl.pixivez.core.PicListFragment
import com.perol.asdpl.pixivez.core.PicListViewModel
import com.perol.asdpl.pixivez.core.TAG_TYPE
import com.perol.asdpl.pixivez.data.model.IllustNext
import com.perol.asdpl.pixivez.objects.dp
import com.perol.asdpl.pixivez.objects.screenWidthPx
import com.perol.asdpl.pixivez.services.Event
import com.perol.asdpl.pixivez.services.FlowEventBus
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.OKWebViewActivity
import com.perol.asdpl.pixivez.ui.WebViewActivity
import com.perol.asdpl.pixivez.ui.home.pixivision.PixiVisionAdapter
import com.perol.asdpl.pixivez.ui.home.pixivision.PixivisionModel
import com.perol.asdpl.pixivez.ui.home.pixivision.PixivsionActivity
import com.perol.asdpl.pixivez.view.LinearItemDecoration


class RecomViewModel : PicListViewModel() {
    lateinit var bannerLoader: () -> Unit
    var loadNew = false
    val onLoadFirstDataRx = {
        if (loadNew)
            retrofit.getNew()
        else
            retrofit.getRecommend().map { IllustNext(it.illusts, it.next_url) }
    }

    override fun setonLoadFirstRx(mode: String, extraArgs: MutableMap<String, Any?>?) {
        onLoadFirstRx = {
            bannerLoader()
            onLoadFirstDataRx()
        }
    }
}

class RecomFragment : PicListFragment() {
    override var TAG: String = TAG_TYPE.Recommend.name

    private fun initViewModel() {
        pixivisionModel.banners.observe(viewLifecycleOwner) {
            //TODO: check if loaded
            spotlightView.setPadding(0)
            spotlightView.layoutManager =
                LoopingLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL)
            pixiVisionAdapter.setNewInstance(it)
        }
        pixivisionModel.addbanners.observe(viewLifecycleOwner) {
            if (it != null) {
                pixiVisionAdapter.addData(it)
            } else {
                pixiVisionAdapter.loadMoreFail()
            }
        }
        pixivisionModel.nextPixivisonUrl.observe(viewLifecycleOwner) {
            if (::pixiVisionAdapter.isInitialized) {
                if (it == null) {
                    pixiVisionAdapter.loadMoreEnd()
                } else {
                    pixiVisionAdapter.loadMoreComplete()
                }
            }
        }

        FlowEventBus.observe<Event.BlockTagsChanged>(viewLifecycleOwner) {
            filter.blockTags = it.blockTags
            picListAdapter.notifyFilterChanged()
        }
    }

    private val pixivisionModel: PixivisionModel by sharedViewModel("pixivision")
    override val viewModel: RecomViewModel by viewModels({ requireActivity() })
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.bannerLoader = pixivisionModel::onRefreshListener
    }

    private lateinit var headerLogo: View
    private lateinit var spotlightView: RecyclerView
    private lateinit var pixiVisionAdapter: PixiVisionAdapter
    private lateinit var filter: PicListFilter
    override fun configByTAG() {
        headerBinding.imgBtnSpinner.apply {
            setText(R.string.newwork)
            setIconResource(R.drawable.ic_menu_gallery)
        }
        headerBinding.imgBtnSpinner.setOnClickListener {
            viewModel.apply {
                loadNew = loadNew.not()
                setonLoadFirstRx(TAG)
                onLoadFirst(onLoadFirstDataRx)
                headerBinding.imgBtnSpinner.setText(if (loadNew) R.string.recommend else R.string.newwork)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        // pixivision
        spotlightView = LayoutInflater.from(requireContext())
            .inflate(R.layout.header_pixivision, null).rootView as RecyclerView
        headerLogo =
            LayoutInflater.from(requireContext()).inflate(R.layout.header_banner_empty, null)
        headerLogo.setOnClickListener {
            val options = if (PxEZApp.animationEnable) {
                ActivityOptions.makeSceneTransitionAnimation(
                    context as Activity,
                    Pair(headerLogo, "shared_element_container")
                ).toBundle()
            } else null
            startActivity(Intent(context, PixivsionActivity::class.java), options)
        }
        pixiVisionAdapter = PixiVisionAdapter(
            R.layout.view_pixivision_item_small,
            null
        )
        picListAdapter.apply {
            setAnimationWithDefault(BaseQuickAdapter.AnimationType.SlideInBottom)
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            headerWithEmptyEnable = true
            footerWithEmptyEnable = true
            addHeaderView(spotlightView, 0)
            //addHeaderView(headerLogo, 0)
        }
        pixiVisionAdapter.setHeaderView(headerLogo, orientation = RecyclerView.HORIZONTAL)
        //}
        spotlightView.addItemDecoration(LinearItemDecoration(4.dp))
        // spotlightView.setHasFixedSize(true)
        PagerSnapHelper().attachToRecyclerView(spotlightView)
        spotlightView.adapter = pixiVisionAdapter
        pixiVisionAdapter.setOnItemClickListener { adapter, view, position ->
            val intent = Intent(
                context,
                if (PxEZApp.instance.pre.getBoolean("disableproxy", false)) {
                    WebViewActivity::class.java
                } else {
                    OKWebViewActivity::class.java
                }
            )
            intent.putExtra("url", pixiVisionAdapter.data[position].article_url)
            startActivity(intent)
            view.findViewById<View>(R.id.pixivision_viewed).setBackgroundColor(Color.YELLOW)
        }
        val autoLoop = PxEZApp.instance.pre
            .getBoolean("banner_auto_loop", true)
        spotlightView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        if (!autoLoop) {
            pixiVisionAdapter.setOnLoadMoreListener {
                pixivisionModel.onLoadMoreBannerRequested()
            }
        }
        //if (pixivisionModel.banners.value.isNullOrEmpty()){
        //    spotlightView.visibility = View.GONE
        //} else {
        //    headerLogo.visibility = View.GONE
        if (pixivisionModel.banners.value.isNullOrEmpty()) {
            headerLogo.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val padding: Int = (screenWidthPx() - headerLogo.measuredWidth) / 2 - 10.dp
            spotlightView.setPadding(padding, 0, padding, 0)
        }
        /* reset layoutManager after data loaded to prevent flicker loop
        if (autoLoop) {
            // spotlightView.layoutManager = LoopingLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL)
            spotlightView.addItemDecoration(LinearItemDecoration(4.dp))
            PagerSnapHelper().attachToRecyclerView(spotlightView)
            // spotlightView.addItemDecoration(LinePagerIndicatorDecoration(headerNum = 0))
            // LoopingSnapHelper().attachToRecyclerView(spotlightView)
        } else {
            spotlightView.addItemDecoration(LinearItemDecoration(4.dp))
            PagerSnapHelper().attachToRecyclerView(spotlightView)
            LinearSnapHelper().attachToRecyclerView(spotlightView)
             CardScaleper(true).run{
                mCurrentItemOffset=393
                attachToRecyclerView(spotlightView)
             }*/
        spotlightView.layoutAnimation = LayoutAnimationController(
            AnimationUtils.loadAnimation(context, R.anim.right_in)
        ).apply {
            order = LayoutAnimationController.ORDER_NORMAL
            delay = 1f
            interpolator = AccelerateInterpolator(0.5f)
        }
    }
}
