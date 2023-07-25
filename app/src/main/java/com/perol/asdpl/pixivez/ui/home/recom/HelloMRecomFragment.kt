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

package com.perol.asdpl.pixivez.ui.home.recom

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseFragment
import com.perol.asdpl.pixivez.base.factory.sharedViewModel
import com.perol.asdpl.pixivez.core.PicListAdapter
import com.perol.asdpl.pixivez.core.PicListBtnAdapter
import com.perol.asdpl.pixivez.core.PicListBtnUserAdapter
import com.perol.asdpl.pixivez.core.PicListXAdapter
import com.perol.asdpl.pixivez.core.PicListXUserAdapter
import com.perol.asdpl.pixivez.databinding.FragmentRecommendBinding
import com.perol.asdpl.pixivez.objects.AdapterRefreshEvent
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.objects.dp
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.OKWebViewActivity
import com.perol.asdpl.pixivez.ui.WebViewActivity
import com.perol.asdpl.pixivez.ui.home.pixivision.PixiVisionAdapter
import com.perol.asdpl.pixivez.ui.home.pixivision.PixivisionModel
import com.perol.asdpl.pixivez.ui.home.pixivision.PixivsionActivity
import com.perol.asdpl.pixivez.view.LinearItemDecoration
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * A simple [Fragment] subclass.
 * Use the [HelloMRecomFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class HelloMRecomFragment : BaseFragment() {
    override fun loadData() {
        viewmodel.onRefreshListener()
        pixivisionModel.onRefreshListener()
    }

    override fun onResume() {
        isLoaded = picListAdapter.data.isNotEmpty()
        super.onResume()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AdapterRefreshEvent) {
        runBlocking {
            picListAdapter.notifyDataSetChanged()
        }
    }

    private fun initViewModel() {
        viewmodel.illusts.observe(viewLifecycleOwner) {
            binding.swipeRefreshLayout.isRefreshing = false
            if (it != null) {
                picListAdapter.setList(it)
                // binding.recyclerview.smoothScrollToPosition(0)
                // pixivisionModel.onRefreshListener()
            } else {
                picListAdapter.loadMoreFail()
            }
        }
        viewmodel.illustsAdded.observe(viewLifecycleOwner) {
            if (it != null) {
                picListAdapter.addData(it)
            } else {
                picListAdapter.loadMoreFail()
            }
        }
        viewmodel.nextUrl.observe(viewLifecycleOwner) {
            if (it == null) {
                picListAdapter.loadMoreEnd()
            } else {
                picListAdapter.loadMoreComplete()
            }
        }
        pixivisionModel.banners.observe(viewLifecycleOwner) {
            pixiVisionAdapter.setNewInstance(it)
            val spotlightView = bannerView.findViewById<RecyclerView>(R.id.pixivisionList)
            val autoLoop = PxEZApp.instance.pre
                .getBoolean("banner_auto_loop", true)
            if (autoLoop) {
                spotlightView.layoutManager =
                    LoopingLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL)
            }
            spotlightView.layoutAnimation = LayoutAnimationController(
                AnimationUtils.loadAnimation(
                    context,
                    R.anim.right_in
                )
            ).also {
                it.order = LayoutAnimationController.ORDER_NORMAL
                it.delay = 1f
                it.interpolator = AccelerateInterpolator(0.5f)
            }
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
    }

    private lateinit var picListAdapter: PicListAdapter
    private lateinit var pixiVisionAdapter: PixiVisionAdapter
    private val viewmodel: HelloMRecomModel by activityViewModels()
    private val pixivisionModel: PixivisionModel by sharedViewModel("pixivision")

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        // viewmodel = ViewModelProvider(requireActivity())[HelloMRecomModel::class.java]
    }

    private var exitTime = 0L
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()

        binding.recyclerview.apply {
            layoutManager = StaggeredGridLayoutManager(
                2 * context.resources.configuration.orientation,
                StaggeredGridLayoutManager.VERTICAL
            )
            adapter = picListAdapter
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewmodel.onRefreshListener()
            pixivisionModel.onRefreshListener()
        }
        picListAdapter.setOnLoadMoreListener {
            viewmodel.onLoadMore()
        }
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
        if (!autoLoop) {
            pixiVisionAdapter.setOnLoadMoreListener {
                pixivisionModel.onLoadMoreBannerRequested()
            }
        }
        // pixivision logo
        val logo =
            LayoutInflater.from(requireContext()).inflate(R.layout.header_pixivision_logo, null)
        logo.setOnClickListener {
            startActivity(Intent(context, PixivsionActivity::class.java))
        }
        pixiVisionAdapter.setHeaderView(logo, orientation = RecyclerView.HORIZONTAL)

        val spotlightView = bannerView.findViewById<RecyclerView>(R.id.pixivisionList)
        spotlightView.adapter = pixiVisionAdapter
        if (autoLoop) {
            // spotlightView.layoutManager = LoopingLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL)
            spotlightView.addItemDecoration(LinearItemDecoration(4.dp))
            PagerSnapHelper().attachToRecyclerView(spotlightView)
            // spotlightView.addItemDecoration(LinePagerIndicatorDecoration(headerNum = 0))
            // LoopingSnapHelper().attachToRecyclerView(spotlightView)
        } else {
            spotlightView.addItemDecoration(LinearItemDecoration(4.dp))
            PagerSnapHelper().attachToRecyclerView(spotlightView)
            // LinearSnapHelper().attachToRecyclerView(spotlightView)
            // CardScaleHelper(true).run{
            //    mCurrentItemOffset=393
            //    attachToRecyclerView(spotlightView)
            // }
        }
        spotlightView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        spotlightView.layoutAnimationListener = object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                // spotlightView.smoothScrollToPosition(2)
                spotlightView.layoutAnimation = null // show animation only at first time
            }
        }

        binding.swipeRefreshLayout.isRefreshing = true
        // parentFragment?.view?.findViewById<TabLayout>(R.id.tablayout)? 重复ID问题导致只有单个有用
        ((parentFragment?.view as ViewGroup?)?.getChildAt(0) as TabLayout?)?.getTabAt(0)
            ?.view?.setOnClickListener {
                if ((System.currentTimeMillis() - exitTime) > 3000) {
                    Toast.makeText(
                        PxEZApp.instance,
                        getString(R.string.back_to_the_top),
                        Toast.LENGTH_SHORT
                    ).show()
                    exitTime = System.currentTimeMillis()
                } else {
                    binding.recyclerview.scrollToPosition(0)
                }
            }
    }

    private lateinit var bannerView: View
    private lateinit var binding: FragmentRecommendBinding
    private lateinit var filter: IllustFilter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        filter = IllustFilter(isR18on)
        picListAdapter =
            if (PxEZApp.instance.pre
                    .getBoolean("use_picX_layout_main", true)
            ) {
                if (PxEZApp.instance.pre
                        .getBoolean("show_user_img_main", true)
                ) {
                    PicListXUserAdapter(
                        R.layout.view_ranking_item_s,
                        null,
                        filter
                    )
                } else {
                    PicListXAdapter(
                        R.layout.view_recommand_item_s,
                        null,
                        filter
                    )
                }
            } else {
                if (PxEZApp.instance.pre
                        .getBoolean("show_user_img_main", true)
                ) {
                    PicListBtnUserAdapter(
                        R.layout.view_ranking_item,
                        null,
                        filter
                    )
                } else {
                    PicListBtnAdapter(
                        R.layout.view_recommand_item,
                        null,
                        filter
                    )
                }
            }
        bannerView = inflater.inflate(R.layout.header_pixivision, container, false)
        pixiVisionAdapter = PixiVisionAdapter(
            R.layout.view_pixivision_item_small,
            null
        )
        picListAdapter.apply {
            setAnimationWithDefault(BaseQuickAdapter.AnimationType.SlideInBottom)
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            headerWithEmptyEnable = true
            footerWithEmptyEnable = true
            addHeaderView(bannerView)
        }

        binding = FragmentRecommendBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {

        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HelloMRecommendFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HelloMRecomFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
