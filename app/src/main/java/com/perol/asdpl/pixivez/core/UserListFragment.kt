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

package com.perol.asdpl.pixivez.core

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseVBFragment
import com.perol.asdpl.pixivez.databinding.FragmentListBinding
import com.perol.asdpl.pixivez.databinding.ViewRestrictButtonBinding
import com.perol.asdpl.pixivez.objects.InteractionUtil.visRestrictTag
import com.perol.asdpl.pixivez.objects.argument
import com.perol.asdpl.pixivez.objects.argumentNullable
import com.perol.asdpl.pixivez.objects.getMaxColumn
import com.perol.asdpl.pixivez.ui.FragmentActivity
import com.perol.asdpl.pixivez.view.AverageGridItemDecoration

/**
 * Fragment of userid following/followed users
 */
class UserListFragment : BaseVBFragment<FragmentListBinding>() {
    override fun loadData() {
        viewModel.onLoadFirst()
    }

    override fun onResume() {
        isLoaded = userListAdapter.data.isNotEmpty()
        super.onResume()
    }

    override var TAG: String by argument("UserListFragment")

    // -----------------
    private var keyword: String? by argumentNullable()

    // -----------------
    private var userid: Int? by argumentNullable()
    // -----------------

    private var _bindingHeader: ViewRestrictButtonBinding? = null
    private val bindingHeader: ViewRestrictButtonBinding
        get() = requireNotNull(_bindingHeader) { "The property of binding has been destroyed." }
    private lateinit var userListAdapter: UserListAdapter
    private val viewModel: UserListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (keyword != null) {
            viewModel.keyword = keyword!!
        } else if (userid != null) {
            viewModel.userid = userid!!
        }
        viewModel.setonLoadFirstRx(TAG)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (viewModel.needHeader && _bindingHeader == null) {
            _bindingHeader = ViewRestrictButtonBinding.inflate(inflater)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userListAdapter = UserListAdapter(R.layout.view_usershow_item)

        initViewModel()

        if (viewModel.needHeader) {
            userListAdapter.addHeaderView(bindingHeader.root)
            bindingHeader.buttonPublic.isChecked = true
            bindingHeader.buttonAll.visibility = View.GONE
            bindingHeader.toggleRestrict.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    val checkedIndex = group.indexOfChild(group.findViewById(checkedId))
                    val value = visRestrictTag(checkedIndex == 2)
                    if (viewModel.restrict.value != value) {
                        viewModel.restrict.value = value
                        viewModel.onLoadFirst()
                    }
                }
            }
        }
        binding.recyclerview.apply {
            adapter = userListAdapter
            layoutManager =
                GridLayoutManager(requireContext(), getMaxColumn(UserListAdapter.itemWidth))
            addItemDecoration(AverageGridItemDecoration(UserListAdapter.itemWidthPx))
            // FlexboxLayoutManager(context, FlexDirection.ROW, FlexWrap.WRAP)
            //    .apply { justifyContent = JustifyContent.SPACE_AROUND }
        }
        userListAdapter.setOnLoadMoreListener {
            viewModel.onLoadMore()
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.onLoadFirst()
        }
    }

    private fun initViewModel() {
        viewModel.data.observe(viewLifecycleOwner) {
            if (it != null) {
                userListAdapter.setList(it)
            } else {
                userListAdapter.loadMoreFail()
            }
        }
        viewModel.nextUrl.observe(viewLifecycleOwner) {
            if (it != null) {
                userListAdapter.loadMoreComplete()
            } else {
                userListAdapter.loadMoreEnd()
            }
        }
        viewModel.dataAdded.observe(viewLifecycleOwner) {
            if (it != null) {
                userListAdapter.addData(it)
            } else {
                userListAdapter.loadMoreFail()
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
        fun start(
            context: Context,
            userid: Int,
            getFollowing: Boolean? = null,
            getMyPixiv: Boolean = false
        ) {
            val title = when (getFollowing) {
                true -> R.string.following
                false -> R.string.followers
                null -> if (getMyPixiv) R.string.goodpfriend else R.string.related
            }
            FragmentActivity.start(context, "UserList", title, Bundle().apply {
                putInt("userid", userid)
                putString(
                    "TAG", when (getFollowing) {
                        true -> "Following"
                        false -> "Follower"
                        null -> if (getMyPixiv) "MyPixiv" else "Related"
                    }
                )
            })
        }

        @JvmStatic
        fun newInstance(userid: Int, getFollowing: Boolean? = null) =
            UserListFragment().apply {
                this.userid = userid
                this.TAG = when (getFollowing) {
                    true -> "Following"
                    false -> "Follower"
                    null -> "Related"
                }
            }

        @JvmStatic
        fun searchUser(keyword: String) =
            UserListFragment().apply {
                this.TAG = "Search"
                this.keyword = keyword
            }

        @JvmStatic
        fun recommendUser() =
            UserListFragment().apply {
                this.TAG = "Recommend"
            }
    }
}
