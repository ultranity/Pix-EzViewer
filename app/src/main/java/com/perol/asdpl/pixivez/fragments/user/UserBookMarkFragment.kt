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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.adapters.PicListAdapter
import com.perol.asdpl.pixivez.adapters.PicListBtnAdapter
import com.perol.asdpl.pixivez.adapters.PicListBtnUserAdapter
import com.perol.asdpl.pixivez.databinding.FragmentUserBookMarkBinding
import com.perol.asdpl.pixivez.databinding.HeaderBookmarkBinding
import com.perol.asdpl.pixivez.dialog.TagsShowDialog
import com.perol.asdpl.pixivez.fragments.BaseFragment
import com.perol.asdpl.pixivez.objects.AdapterRefreshEvent
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.objects.argument
import com.perol.asdpl.pixivez.repository.AppDataRepository
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.GridItemDecoration
import com.perol.asdpl.pixivez.viewmodel.UserBookMarkViewModel
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * A simple [Fragment] subclass.
 * Use the [UserBookMarkFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */

class UserBookMarkFragment : BaseFragment(), TagsShowDialog.Callback {
    override fun loadData() {
        viewModel.first(uid, pub)
    }

    private lateinit var filter: IllustFilter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        filter = IllustFilter(isR18on, blockTags,
            hideBookmarked = viewActivity.viewModel.hideBookmarked.value!!,
            hideDownloaded = viewActivity.viewModel.hideDownloaded.value!!)
        picItemAdapter =
            if (PxEZApp.instance.pre.getBoolean("show_user_img_bookmarked", true)) {
                PicListBtnUserAdapter(
                    R.layout.view_ranking_item,
                    null,
                    filter
                )
                // singleLine = false,
            }
            else {
                PicListBtnAdapter(
                    R.layout.view_recommand_item,
                    null,
                    filter
                )
            }

        binding.recyclerview.apply {
            layoutManager = StaggeredGridLayoutManager(
                2 * context.resources.configuration.orientation,
                StaggeredGridLayoutManager.VERTICAL
            )
            adapter = picItemAdapter
            addItemDecoration(GridItemDecoration())
        }
        picItemAdapter.setOnLoadMoreListener {
            viewModel.onLoadMoreListener()
        }

        binding.refreshlayout.setOnRefreshListener {
            viewModel.onRefreshListener(uid, pub, null)
        }
        if (viewModel.isSelfPage()) {
            val header = HeaderBookmarkBinding.inflate(layoutInflater)
            picItemAdapter.addHeaderView(header.root)
            header.imagebuttonShowtags.setOnClickListener {
                showTagDialog()
            }
        }
    }

    override fun onClick(string: String, public: String) {
        viewModel.onRefreshListener(
            uid,
            public,
            string.ifBlank {
                null
            }
        )
    }

    override fun onResume() {
        isLoaded = viewModel.data.value != null
        super.onResume()
        Log.d("UserBookMarkFragment", "UserBookMarkFragment resume")
    }

    private fun initViewModel() {
        viewModel.id = uid
        this.viewActivity = activity as UserMActivity

        viewModel.nextUrl.observe(viewLifecycleOwner) {
            if (it.isNullOrEmpty()) {
                picItemAdapter.loadMoreEnd()
            }
            else {
                picItemAdapter.loadMoreComplete()
            }
        }
        viewModel.data.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.refreshlayout.isRefreshing = false
                picItemAdapter.setList(it)
            }
            else {
                picItemAdapter.loadMoreFail()
            }
        }
        viewModel.dataAdded.observe(viewLifecycleOwner) {
            if (it != null) {
                picItemAdapter.addData(it)
                picItemAdapter.loadMoreComplete()
            }
            else {
                picItemAdapter.loadMoreFail()
            }
        }
        // viewModel.tags.observe(viewLifecycleOwner) {
        //
        // }
    }

    private var uid: Long by argument()
    private var TAG: String by argument()

    private var pub = "public"

    val viewModel: UserBookMarkViewModel by viewModels()
    private lateinit var viewActivity: UserMActivity

    private fun showTagDialog() {
        if (viewModel.tags.value != null) {
            TagsShowDialog.newInstance(uid).also { 
                it.callback = this
            }.show(childFragmentManager)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AdapterRefreshEvent) {
        runBlocking {
            val id = AppDataRepository.currentUser.userid
            picItemAdapter.illustFilter.hideBookmarked =
                if (uid != id) {
                    viewActivity.viewModel.hideBookmarked.value!!
                } else { 0 }
            picItemAdapter.illustFilter.hideDownloaded =
                if (uid == id) {
                    viewActivity.viewModel.hideDownloaded.value!!
                } else { false }
            picItemAdapter.notifyDataSetChanged()
        }
    }

    private lateinit var picItemAdapter: PicListAdapter
    private lateinit var binding: FragmentUserBookMarkBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserBookMarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param userid Parameter 1.
         * @param tag Parameter 2.
         * @return A new instance of fragment UserBookMarkFragment.
         */
        @JvmStatic
        fun newInstance(userid: Long, tag: String) =
            UserBookMarkFragment().apply {
                this.uid = userid
                this.TAG = tag
            }
    }
}
