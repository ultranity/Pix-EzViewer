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

package com.perol.asdpl.pixivez.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.adapters.UserShowAdapter
import com.perol.asdpl.pixivez.databinding.FragmentIllustratorBinding
import com.perol.asdpl.pixivez.objects.ScreenUtil.getMaxColumn
import com.perol.asdpl.pixivez.responses.SearchUserResponse
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.viewmodel.IllustratorViewModel
import kotlin.properties.Delegates


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [IllustratorFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class IllustratorFragment : BaseFragment(), AdapterView.OnItemSelectedListener {
    override fun loadData() {
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        when (p2) {
            0 -> {
                restrict = "public"
            }
            1 -> {
                restrict = "private"
            }
        }
        viewModel.onRefresh(userid, restrict, getFollowing)
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }

    private lateinit var userShowAdapter: UserShowAdapter
    private lateinit var viewModel: IllustratorViewModel
    var restrict: String = "public"
    private var exitTime = 0L
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        binding.recyclerviewIllustrator.adapter = userShowAdapter
        binding.recyclerviewIllustrator.layoutManager =
            GridLayoutManager(requireContext(), getMaxColumn(400))
        //FlexboxLayoutManager(context, FlexDirection.ROW, FlexWrap.WRAP)
        //    .apply { justifyContent = JustifyContent.SPACE_AROUND }
        binding.spinnerIllustrator.onItemSelectedListener = this
        userShowAdapter.loadMoreModule.setOnLoadMoreListener {
            viewModel.onLoadMore(viewModel.nextUrl.value!!)
        }
        binding.swiperefreshIllustrator.setOnRefreshListener {
            viewModel.onRefresh(userid, restrict, getFollowing)
        }
        userShowAdapter.setOnItemClickListener { _, _, position ->
            UserMActivity.start(requireContext(), userShowAdapter.data[position].user)
        }

        //parentFragment?.view?.findViewById<TabLayout>(R.id.tablayout)? 重复ID问题导致只有单个有用
        ((parentFragment?.view as ViewGroup?)?.getChildAt(0) as TabLayout?)
            ?.getTabAt(1)
            ?.view?.setOnClickListener {
                if ((System.currentTimeMillis() - exitTime) > 3000) {
                    Toast.makeText(
                        PxEZApp.instance,
                        getString(R.string.back_to_the_top),
                        Toast.LENGTH_SHORT
                    ).show()
                    exitTime = System.currentTimeMillis()
                } else {
                    binding.recyclerviewIllustrator.scrollToPosition(0)
                }
            }
    }

    private fun initViewModel() {
        viewModel.userpreviews.observe(viewLifecycleOwner) {
            userpreviews(it)
        }
        viewModel.nextUrl.observe(viewLifecycleOwner) {
            nextUrl(it)
        }
        viewModel.adduserpreviews.observe(viewLifecycleOwner) {
            if (it != null) {
                userShowAdapter.addData(it)
            }
        }
        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            binding.swiperefreshIllustrator.isRefreshing = it
        }

    }

    private fun nextUrl(it: String?) {
        if (it != null) {
            userShowAdapter.loadMoreModule.loadMoreComplete()
        } else {
            userShowAdapter.loadMoreModule.loadMoreEnd()
        }
    }

    private fun userpreviews(it: ArrayList<SearchUserResponse.UserPreviewsBean>?) {
        if (it != null) {
            userShowAdapter.setNewInstance(it)
        }
    }

    // TODO: Rename and change types of parameters
    private var userid by Delegates.notNull<Long>()
    private var getFollowing by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userid = it.getLong(ARG_PARAM1)
            getFollowing = it.getBoolean(ARG_PARAM2)
        }
        viewModel = ViewModelProvider(this)[IllustratorViewModel::class.java]
    }

    private lateinit var binding: FragmentIllustratorBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        userShowAdapter = UserShowAdapter(R.layout.view_usershow_item)

        binding = FragmentIllustratorBinding.inflate(inflater, container, false)

        return binding.root
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param userid Parameter 1.
         * @param getFollowing Parameter 2.
         * @return A new instance of fragment IllustratorFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(userid: Long, getFollowing: Boolean) =
            IllustratorFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PARAM1, userid)
                    putBoolean(ARG_PARAM2, getFollowing)
                }
            }
    }
}
