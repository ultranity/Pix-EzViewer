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

package com.perol.asdpl.pixivez.fragments.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.adapters.PicListAdapter
import com.perol.asdpl.pixivez.adapters.PicListBtnAdapter
import com.perol.asdpl.pixivez.databinding.FragmentUserIllustBinding
import com.perol.asdpl.pixivez.fragments.BaseFragment
import com.perol.asdpl.pixivez.objects.AdapterRefreshEvent
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.viewmodel.UserMillustViewModel
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [UserIllustFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class UserIllustFragment : BaseFragment() {
    override fun loadData() {
        viewModel.first(param1!!, param2!!)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AdapterRefreshEvent) {
        runBlocking {
            val allTags = blockViewModel.getAllTags()
            blockTags = allTags.map {
                it.name
            }
            picListAdapter.filter.hideBookmarked = viewActivity.viewModel.hideBookmarked.value!!
            picListAdapter.filter.blockTags = blockTags
            picListAdapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        isLoaded = viewModel.data.value != null
        super.onResume()
        Log.d("UserIllustFragment", "UserIllustFragment resume")
    }

    private fun initView() {
        picListAdapter.loadMoreModule.setOnLoadMoreListener {
            viewModel.onLoadMoreListener()
        }
        binding.refreshlayout.setOnRefreshListener {
            viewModel.onRefreshListener(param1!!, param2!!)
        }
        binding.recyclerview.apply {
            layoutManager = StaggeredGridLayoutManager(
                2 * context.resources.configuration.orientation,
                StaggeredGridLayoutManager.VERTICAL
            )
            adapter = picListAdapter
            // addItemDecoration(GridItemDecoration())
        }
    }

    private var param1: Long? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getLong(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private fun initViewModel() {
        // TODO: ViewModelProvider by activity
        viewModel = ViewModelProvider(this)[UserMillustViewModel::class.java]
        viewActivity = requireActivity() as UserMActivity

        viewModel.nextUrl.observe(viewLifecycleOwner) {
            if (it.isNullOrEmpty()) {
                picListAdapter.loadMoreEnd()
            }
            else {
                picListAdapter.loadMoreComplete()
            }
        }
        viewModel.data.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.refreshlayout.isRefreshing = false
                picListAdapter.setNewInstance(it.toMutableList())
            }
        }
        viewModel.adddata.observe(viewLifecycleOwner) {
            if (it != null) {
                picListAdapter.addData(it)
                picListAdapter.loadMoreComplete()
            }
            else {
                picListAdapter.loadMoreModule.loadMoreFail()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        filter = IllustFilter(
            isR18on,
            blockTags,
            hideBookmarked = viewActivity.viewModel.hideBookmarked.value!!,
            hideDownloaded = viewActivity.viewModel.hideDownloaded.value!!)
        // Inflate the layout for this fragment
        picListAdapter = PicListBtnAdapter(
            R.layout.view_recommand_item,
            null,
            filter
        )
        initView()
    }

    private lateinit var viewModel: UserMillustViewModel
    private lateinit var filter: IllustFilter
    private lateinit var viewActivity: UserMActivity

    private lateinit var picListAdapter: PicListAdapter
    private lateinit var binding: FragmentUserIllustBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserIllustBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param userid Parameter 1.
         * @param tag Parameter 2.
         * @return A new instance of fragment UserIllustFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(userid: Long, tag: String) =
            UserIllustFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PARAM1, userid)
                    putString(ARG_PARAM2, tag)
                }
            }
    }
}
