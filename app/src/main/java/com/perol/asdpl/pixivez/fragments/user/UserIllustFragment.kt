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
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.adapters.PicItemAdapterBase
import com.perol.asdpl.pixivez.adapters.PicListXBtnAdapter
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
            picListBtnAdapter.filter.hideBookmarked = viewActivity.viewModel.hideBookmarked.value!!
            picListBtnAdapter.filter.blockTags = blockTags
            picListBtnAdapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("UserIllustFragment","UserIllustFragment resume")
    }

    private fun initView() {
        picListBtnAdapter.loadMoreModule.setOnLoadMoreListener {
            viewModel.onLoadMoreListener()
        }
        binding.mrefreshlayout.setOnRefreshListener {
            viewModel.onRefreshListener(param1!!, param2!!)
        }
        binding.mrecyclerview.apply {
            layoutManager = StaggeredGridLayoutManager(
                1 + context.resources.configuration.orientation,
                StaggeredGridLayoutManager.VERTICAL
            )
            adapter = picListBtnAdapter
            //addItemDecoration(GridItemDecoration())
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
        viewModel = ViewModelProvider(this)[UserMillustViewModel::class.java]

        viewModel.nextUrl.observe(viewLifecycleOwner) {
            if (it.isNullOrEmpty()) {
                picListBtnAdapter.loadMoreEnd()
            } else {
                picListBtnAdapter.loadMoreComplete()
            }
        }
        viewActivity = requireActivity() as UserMActivity
        viewModel.data.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.mrefreshlayout.isRefreshing = false
                picListBtnAdapter.setNewInstance(it.toMutableList())
            }
        }
        viewModel.adddata.observe(viewLifecycleOwner) {
            if (it != null) {
                picListBtnAdapter.addData(it)
                picListBtnAdapter.loadMoreComplete()
            } else {
                picListBtnAdapter.loadMoreModule.loadMoreFail()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initView()
    }

    lateinit var viewModel: UserMillustViewModel
    lateinit var filter: IllustFilter
    private lateinit var viewActivity: UserMActivity

    private lateinit var picListBtnAdapter: PicItemAdapterBase
    private lateinit var binding: FragmentUserIllustBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        filter = IllustFilter(isR18on,
                blockTags,
                PreferenceManager.getDefaultSharedPreferences(requireActivity())
                    .getInt(UserMActivity.HIDE_BOOKMARKED_ITEM, 0))
        // Inflate the layout for this fragment
        picListBtnAdapter = PicListXBtnAdapter(
            R.layout.view_recommand_item,
            null,
            filter
        )
        binding = FragmentUserIllustBinding.inflate(inflater, container, false)
        return binding.root
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment UserIllustFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: Long, param2: String) =
            UserIllustFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
