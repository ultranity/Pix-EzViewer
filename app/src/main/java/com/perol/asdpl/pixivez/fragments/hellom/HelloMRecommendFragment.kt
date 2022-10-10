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

package com.perol.asdpl.pixivez.fragments.hellom


import android.content.Context
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
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.OKWebViewActivity
import com.perol.asdpl.pixivez.activity.PixivsionActivity
import com.perol.asdpl.pixivez.adapters.*
import com.perol.asdpl.pixivez.databinding.FragmentRecommendBinding
import com.perol.asdpl.pixivez.fragments.BaseFragment
import com.perol.asdpl.pixivez.objects.AdapterRefreshEvent
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.objects.ScreenUtil
import com.perol.asdpl.pixivez.services.GlideApp
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.LinearItemDecoration
import com.perol.asdpl.pixivez.viewmodel.HelloMRecomModel
import com.youth.banner.Banner
import com.youth.banner.loader.ImageLoader
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HelloMRecommendFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class HelloMRecommendFragment : BaseFragment() {
    override fun loadData() {
        viewmodel.onRefreshListener()
    }

    override fun onResume() {
        isLoaded = picListXAdapter.data.isNotEmpty()
        super.onResume()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AdapterRefreshEvent) {
        runBlocking {
            val allTags = blockViewModel.getAllTags()
            blockTags = allTags.map {
                it.name
            }
            picListXAdapter.filter.blockTags = blockTags
            picListXAdapter.notifyDataSetChanged()
        }
    }

    private fun lazyLoad() {
        viewmodel = ViewModelProvider(this)[HelloMRecomModel::class.java]
        viewmodel.illusts.observe(this) {
            binding.swiperefreshRecom.isRefreshing = false
            if (it != null) {
                picListXAdapter.setNewInstance(it)
                binding.recyclerviewRecom.smoothScrollToPosition(0)
            } else {
                picListXAdapter.loadMoreFail()
            }
        }
        viewmodel.addillusts.observe(this) {
            if (it != null) {
                picListXAdapter.addData(it)
            } else {
                picListXAdapter.loadMoreFail()
            }
        }
        viewmodel.nextUrl.observe(this) {
            if (it == null) {
                picListXAdapter.loadMoreEnd()
            } else {
                picListXAdapter.loadMoreComplete()
            }
        }
        viewmodel.banners.observe(this) {
            if (!PreferenceManager.getDefaultSharedPreferences(PxEZApp.instance)
                    .getBoolean("use_new_banner", true)
            ) {
                val arrayList = ArrayList<String>()
                it.map {
                    arrayList.add(it.thumbnail)
                }
                banner.setDelayTime(3800)
                banner.setImages(arrayList)
                banner.setOnBannerListener {
                    startActivity(
                        Intent(
                            requireActivity().applicationContext,
                            PixivsionActivity::class.java
                        )
                    )
                }
                banner.start()
            } else {
                pixiVisionAdapter.setNewInstance(it)
                pixiVisionAdapter.setOnItemClickListener { adapter, view, position ->
                    val intent = Intent(context, OKWebViewActivity::class.java)
                    intent.putExtra("url", it[position].article_url)
                    startActivity(intent)
                    view.findViewById<View>(R.id.pixivision_viewed).setBackgroundColor(Color.YELLOW)
                }
                val spotlightView = bannerView.findViewById<RecyclerView>(R.id.pixivisionList)
                spotlightView.layoutAnimation = LayoutAnimationController(
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.left_in
                    )
                ).also {
                    it.order = LayoutAnimationController.ORDER_NORMAL
                    it.delay = 1f
                    it.interpolator = AccelerateInterpolator(0.5f)
                }
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
    }


    private lateinit var picListXAdapter: PicItemAdapterBase
    private lateinit var pixiVisionAdapter: PixiVisionAdapter
    private lateinit var viewmodel: HelloMRecomModel
    private lateinit var banner: Banner

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        lazyLoad()
    }

    private var exitTime = 0L
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerviewRecom.apply {
            layoutManager = StaggeredGridLayoutManager(
                1 + context.resources.configuration.orientation,
                StaggeredGridLayoutManager.VERTICAL
            )
            adapter = picListXAdapter
        }
        binding.swiperefreshRecom.setOnRefreshListener {
            viewmodel.onRefreshListener()
        }
        picListXAdapter.loadMoreModule.setOnLoadMoreListener {
            viewmodel.onLoadMorePicRequested()
        }

        if (!PreferenceManager.getDefaultSharedPreferences(PxEZApp.instance)
                .getBoolean("use_new_banner", true)
        ) {
            banner = bannerView.findViewById(R.id.banner)
            banner.setImageLoader(object : ImageLoader() {
                override fun displayImage(context: Context, path: Any?, imageView: ImageView?) {
                    GlideApp.with(context).load(path).into(imageView!!)
                }
            })
        } else {
            val spotlightView = bannerView.findViewById<RecyclerView>(R.id.pixivisionList)
            pixiVisionAdapter.loadMoreModule.setOnLoadMoreListener {
                viewmodel.onLoadMoreBannerRequested()
            }
            /*val logo = LayoutInflater.from(requireContext()).inflate(R.layout.header_pixvision_logo, null)
            logo.setOnClickListener {
                startActivity(
                    Intent(
                        requireActivity().applicationContext,
                        PixivsionActivity::class.java
                    )
                )
            }
            pixiVisionAdapter.setHeaderView(logo)
            logo.scaleX =0.6f
            logo.scaleY =0.6f*/
            val manager = LinearLayoutManager(requireContext())
            manager.orientation = LinearLayoutManager.HORIZONTAL
            spotlightView.layoutManager = manager
            spotlightView.adapter = pixiVisionAdapter
            spotlightView.layoutAnimationListener = object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                }

                override fun onAnimationEnd(animation: Animation) {
                    spotlightView.smoothScrollToPosition(0)
                    spotlightView.layoutAnimation = null //show animation only at first time
                }

                override fun onAnimationRepeat(animation: Animation) {}
            }
            spotlightView.addItemDecoration(LinearItemDecoration(ScreenUtil.dip2px(4.0f)))
            //LinearSnapHelper().attachToRecyclerView(spotlightView)
            PagerSnapHelper().attachToRecyclerView(spotlightView)
            //CardScaleHelper(true).run{
            //    mCurrentItemOffset=393
            //    attachToRecyclerView(spotlightView)
            //}

            binding.swiperefreshRecom.isRefreshing = true
        }
        parentFragment?.view?.findViewById<TabLayout>(R.id.tablayout)?.getTabAt(0)
            ?.view?.setOnClickListener {
                if ((System.currentTimeMillis() - exitTime) > 3000) {
                    Toast.makeText(
                        PxEZApp.instance,
                        getString(R.string.back_to_the_top),
                        Toast.LENGTH_SHORT
                    ).show()
                    exitTime = System.currentTimeMillis()
                } else {
                    binding.recyclerviewRecom.scrollToPosition(0)
                }
            }
    }

    private lateinit var bannerView: View
    private lateinit var binding: FragmentRecommendBinding
    private lateinit var filter: IllustFilter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        filter = IllustFilter(isR18on)
        picListXAdapter =
            if(PreferenceManager.getDefaultSharedPreferences(PxEZApp.instance)
                    .getBoolean("use_picX_layout_main",true)) {
                if (PreferenceManager.getDefaultSharedPreferences(PxEZApp.instance)
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
            }
            else {
                if (PreferenceManager.getDefaultSharedPreferences(PxEZApp.instance)
                        .getBoolean("show_user_img_main", true)
                ) {
                    PicListXBtnUserAdapter(
                        R.layout.view_ranking_item,
                        null,
                        filter
                    )
                } else {
                    PicListXBtnAdapter(
                        R.layout.view_recommand_item,
                        null,
                        filter
                    )
                }
            }
        //picListXAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        if(PreferenceManager.getDefaultSharedPreferences(PxEZApp.instance)
                .getBoolean("use_new_banner",true)){
            bannerView = inflater.inflate(R.layout.header_pixivision, container, false)
            pixiVisionAdapter = PixiVisionAdapter(
                R.layout.view_pixivision_item_small,
                null,
                requireActivity())
        } else {
            bannerView = inflater.inflate(R.layout.header_recom, container, false)
        }
        picListXAdapter.apply {
            addHeaderView(bannerView)
        }

        binding = FragmentRecommendBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
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
            HelloMRecommendFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
