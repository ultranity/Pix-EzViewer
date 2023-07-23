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

package com.perol.asdpl.pixivez.ui.home.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseVBFragment
import com.perol.asdpl.pixivez.databinding.FragmentIllustListBinding
import com.perol.asdpl.pixivez.databinding.ViewRestrictButtonBinding
import com.perol.asdpl.pixivez.objects.InteractionUtil.visRestrictTag
import com.perol.asdpl.pixivez.objects.ScreenUtil.getMaxColumn
import com.perol.asdpl.pixivez.objects.argument
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import com.perol.asdpl.pixivez.ui.user.UserShowAdapter
import com.perol.asdpl.pixivez.view.AverageGridItemDecoration

/**
 * Fragment of userid following/followed users
 */
class UserListFragment : BaseVBFragment<FragmentIllustListBinding>() {
    override fun loadData() {
        viewModel.onRefresh(userid, restrict, getFollowing)
    }

    override fun onResume() {
        isLoaded = userShowAdapter.data.isNotEmpty()
        super.onResume()
    }

    private var userid: Long by argument()
    private var getFollowing: Boolean by argument()

    private var _bindingHeader: ViewRestrictButtonBinding? = null
    private val bindingHeader: ViewRestrictButtonBinding
        get() = requireNotNull(_bindingHeader) { "The property of binding has been destroyed." }
    private lateinit var userShowAdapter: UserShowAdapter
    private val viewModel: UserListViewModel by viewModels()
    private var restrict: String = "public"
    private var exitTime = 0L
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (_bindingHeader == null) {
            _bindingHeader = ViewRestrictButtonBinding.inflate(inflater)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userShowAdapter = UserShowAdapter(R.layout.view_usershow_item)
        userShowAdapter.addHeaderView(bindingHeader.root)

        initViewModel()

        binding.recyclerview.apply {
            adapter = userShowAdapter
            layoutManager =
                GridLayoutManager(requireContext(), getMaxColumn(UserShowAdapter.itemWidth))
            addItemDecoration(AverageGridItemDecoration(UserShowAdapter.itemWidthPx))
            // FlexboxLayoutManager(context, FlexDirection.ROW, FlexWrap.WRAP)
            //    .apply { justifyContent = JustifyContent.SPACE_AROUND }
        }
        bindingHeader.apply {
            buttonPublic.isChecked = true
            buttonAll.visibility = View.GONE
            toggleRestrict.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked){
                    val checkedIndex = group.indexOfChild(group.findViewById(checkedId))
                    val value = visRestrictTag(checkedIndex == 2)
                    if (restrict != value) {
                        restrict = value
                        viewModel.onRefresh(userid, restrict, getFollowing)
                    }
                }
            }
        }
        userShowAdapter.setOnLoadMoreListener {
            viewModel.onLoadMore()
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.onRefresh(userid, restrict, getFollowing)
        }
        userShowAdapter.setOnItemClickListener { _, _, position ->
            UserMActivity.start(requireContext(), userShowAdapter.data[position].user)
        }

        // parentFragment?.view?.findViewById<TabLayout>(R.id.tablayout)? 重复ID问题导致只有单个有用
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
                    binding.recyclerview.scrollToPosition(0)
                }
            }
    }

    private fun initViewModel() {
        viewModel.data.observe(viewLifecycleOwner) {
            if (it != null) {
                userShowAdapter.setNewInstance(it)
            } else {
                userShowAdapter.loadMoreFail()
            }
        }
        viewModel.nextUrl.observe(viewLifecycleOwner) {
            if (it != null) {
                userShowAdapter.loadMoreComplete()
            } else {
                userShowAdapter.loadMoreEnd()
            }
        }
        viewModel.adddata.observe(viewLifecycleOwner) {
            if (it != null) {
                userShowAdapter.addData(it)
            } else {
                userShowAdapter.loadMoreFail()
            }
        }
        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            binding.swipeRefreshLayout.isRefreshing = it
        }
    }

    override fun onDestroy() {
        _bindingHeader = null
        super.onDestroy()
    }

    companion object {
        @JvmStatic
        fun newInstance(userid: Long, getFollowing: Boolean) =
            UserListFragment().apply {
                this.userid = userid
                this.getFollowing = getFollowing
            }
    }
}
