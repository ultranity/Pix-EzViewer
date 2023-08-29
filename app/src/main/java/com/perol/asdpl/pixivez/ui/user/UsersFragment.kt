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

package com.perol.asdpl.pixivez.ui.user

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.KotlinUtil.launchCatching
import com.perol.asdpl.pixivez.core.UserListAdapter
import com.perol.asdpl.pixivez.data.RetrofitRepository
import com.perol.asdpl.pixivez.data.model.ListUserResponse
import com.perol.asdpl.pixivez.databinding.FragmentListBinding
import com.perol.asdpl.pixivez.objects.getMaxColumn
import com.perol.asdpl.pixivez.ui.FragmentActivity
import com.perol.asdpl.pixivez.view.AverageGridItemDecoration

//TODO: merge with UerListFragment
class UsersFragment : Fragment() {
    companion object {
        fun start(context: Context, illustid: Int) {
            FragmentActivity.start(context, "Users", R.string.bookmarked, Bundle().apply {
                putInt("illustid", illustid)
            })
        }
    }

    // TODO: view model
    private lateinit var usersAdapter: UsersAdapter
    private var nextUrl: String? = null
    private val retrofit = RetrofitRepository.getInstance()
    private var illustId: Int = 0
    private var illustData: ListUserResponse? = null
    private lateinit var binding: FragmentListBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerview.apply {
            layoutManager =
                GridLayoutManager(requireContext(), getMaxColumn(UserListAdapter.itemWidth))
            // FlexboxLayoutManager(this, FlexDirection.ROW, FlexWrap.WRAP)
            //    .apply { justifyContent = JustifyContent.SPACE_AROUND }
            addItemDecoration(AverageGridItemDecoration(UserListAdapter.itemWidthPx))
        }
        requireArguments().let {
            illustId = it.getInt("illustid")
            requireActivity().actionBar?.setTitle(R.string.bookmark)
            //binding.title.setText(R.string.bookmark)
            initIllustData()
        }
    }

    private fun initIllustData() {
        usersAdapter = UsersAdapter(R.layout.view_usershow_item)
        binding.recyclerview.adapter = usersAdapter
        lifecycleScope.launchCatching({
            retrofit.api.getIllustBookmarkUsers(illustId)
        }, {
            illustData = it
            nextUrl = it.next_url
            usersAdapter.setNewInstance(it.users)
            usersAdapter.setOnLoadMoreListener {
                if (nextUrl == null) {
                    usersAdapter.loadMoreEnd()
                } else {
                    onLoadMoreUser()
                }
            }
        }, { usersAdapter.loadMoreFail() })
    }

    private fun onLoadMoreUser() {
        lifecycleScope.launchCatching({ retrofit.getNextUser(nextUrl!!) }, {
            illustData = it
            nextUrl = it.next_url
            usersAdapter.addData(it.users)
            usersAdapter.loadMoreComplete()
        }, { usersAdapter.loadMoreFail() })
    }
}
