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


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.PicListBtnUserAdapter
import com.perol.asdpl.pixivez.databinding.FragmentSwiperefreshRecyclerviewBinding
import com.perol.asdpl.pixivez.fragments.BaseFragment
import com.perol.asdpl.pixivez.objects.AdapterRefreshEvent
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.GridItemDecoration
import com.perol.asdpl.pixivez.viewmodel.RankingMViewModel
import com.perol.asdpl.pixivez.viewmodel.factory.RankingShareViewModel
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RankingMFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class RankingMFragment : BaseFragment() {


    private var picDate: String? = null
    private lateinit var viewmodel: RankingMViewModel
    private lateinit var sharemodel: RankingShareViewModel
    private lateinit var picListBtnUserAdapter: PicListBtnUserAdapter
    private var param1: String? = null
    private var param2: Int? = null

    override fun loadData() {
        viewmodel.first(param1!!, picDate)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AdapterRefreshEvent) {
        runBlocking {
            val allTags = blockViewModel.getAllTags()
            blockTags = allTags.map {
                it.name
            }
            picListBtnUserAdapter.blockTags = blockTags
            picListBtnUserAdapter.hideBookmarked = sharemodel.hideBookmarked.value!!
            picListBtnUserAdapter.notifyDataSetChanged()
        }
    }

    private fun initViewModel() {
        sharemodel.sortCoM.observe(viewLifecycleOwner) {
            picListBtnUserAdapter.sortCoM = it
            EventBus.getDefault().post(AdapterRefreshEvent())
        }
        sharemodel.picDateShare.observe(viewLifecycleOwner) {
            viewmodel.datePick(param1!!, it)
        }
        sharemodel.hideBookmarked.observe(viewLifecycleOwner) {
            picListBtnUserAdapter.hideBookmarked = it
            EventBus.getDefault().post(AdapterRefreshEvent())
        }

        viewmodel.addillusts.observe(viewLifecycleOwner) {
            if (it != null) {
                picListBtnUserAdapter.addData(it)
            } else {
                picListBtnUserAdapter.loadMoreFail()
            }
        }
        viewmodel.illusts.observe(viewLifecycleOwner) {
            binding.swiperefreshLayout.isRefreshing = false
            if (it != null) {
                picListBtnUserAdapter.setNewInstance(it)
            } else {
                picListBtnUserAdapter.loadMoreFail()
            }
        }
        viewmodel.nextUrl.observe(viewLifecycleOwner) {
            if (it == null) {
                picListBtnUserAdapter.loadMoreEnd()
            } else {
                picListBtnUserAdapter.loadMoreComplete()
            }
        }
    }

    private var exitTime = 0L
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        picDate = if (sharemodel.picDateShare.value == "$year-$month-$day") {
            null
        } else {
            sharemodel.picDateShare.value
        }
        binding.swiperefreshLayout.setOnRefreshListener {
            viewmodel.onRefresh(param1!!, picDate)
        }
        picListBtnUserAdapter.loadMoreModule.setOnLoadMoreListener {
            viewmodel.onLoadMore()
        }
        binding.recyclerview.apply {
            layoutManager = StaggeredGridLayoutManager(
                1 + context.resources.configuration.orientation,
                StaggeredGridLayoutManager.VERTICAL
            )
            adapter = picListBtnUserAdapter
            addItemDecoration(GridItemDecoration())
        }
        parentFragment?.view?.findViewById<TabLayout>(R.id.tablayout_rankingm)?.getTabAt(param2!!)
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

    override fun onDestroyView() {
        super.onDestroyView()
        parentFragment?.view?.findViewById<TabLayout>(R.id.tablayout_rankingm)?.getTabAt(param2!!)
            ?.view?.setOnClickListener(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getInt(ARG_PARAM2)
        }
        viewmodel = ViewModelProvider(this)[RankingMViewModel::class.java]
        sharemodel = ViewModelProvider(requireActivity())[RankingShareViewModel::class.java]
    }

    private lateinit var binding: FragmentSwiperefreshRecyclerviewBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val headerView = layoutInflater.inflate(R.layout.header_mdynamics, null)
        headerView.findViewById<SwitchMaterial>(R.id.swith_hidebookmarked).apply {
            isChecked = sharemodel.hideBookmarked.value == 1
            setOnCheckedChangeListener { compoundButton, state ->
                sharemodel.hideBookmarked.value = if (state) 1 else 0
            }
        }
        headerView.findViewById<Spinner>(R.id.spinner_CoM).onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    sharemodel.sortCoM.value = position
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        picListBtnUserAdapter = PicListBtnUserAdapter(
            R.layout.view_ranking_item,
            null,
            isR18on, blockTags
        )
        picListBtnUserAdapter.addHeaderView(headerView)
        binding = FragmentSwiperefreshRecyclerviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param position Parameter 2.
         * @return A new instance of fragment RankingMFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, position: Int) =
            RankingMFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putInt(ARG_PARAM2, position)
                }
            }
    }
}
