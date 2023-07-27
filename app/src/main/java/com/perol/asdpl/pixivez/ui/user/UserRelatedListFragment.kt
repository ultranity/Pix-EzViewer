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
import androidx.recyclerview.widget.GridLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.data.RetrofitRepository
import com.perol.asdpl.pixivez.data.model.ListUserResponse
import com.perol.asdpl.pixivez.data.model.SearchUserResponse
import com.perol.asdpl.pixivez.databinding.FragmentListBinding
import com.perol.asdpl.pixivez.databinding.ViewRestrictButtonBinding
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.objects.getMaxColumn
import com.perol.asdpl.pixivez.ui.FragmentActivity
import com.perol.asdpl.pixivez.view.AverageGridItemDecoration
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

class UserRelatedListFragment : Fragment() {
    companion object {
        fun start(context: Context, illustid: Long) {
            FragmentActivity.start(context, "Users","", Bundle().apply {
                putLong("illustid", illustid)
            })
        }

        fun start(context: Context, userid: Long, get_following: Boolean?=null) {
            FragmentActivity.start(context, "Users", "", Bundle().apply {
                putLong("userid", userid)
                if (get_following != null) {
                    putBoolean("get_following", get_following)
                }
            })
        }
    }

    // TODO: view model
    private lateinit var userShowAdapter: UserShowAdapter
    private lateinit var userListAdapter: UserListAdapter
    private var nextUrl: String? = null
    private val retrofitRepository = RetrofitRepository.getInstance()
    private val username: String? = null // TODO: title?
    private var illustId: Long = 0
    private var illustData: ListUserResponse? = null
    private var userid: Long = 0
    private var userData: SearchUserResponse? = null
    private var getFollowing: Boolean = false
    private var restrict = "public"
    private lateinit var binding: FragmentListBinding

    private var _bindingHeader: ViewRestrictButtonBinding? = null
    private val bindingHeader: ViewRestrictButtonBinding
        get() = requireNotNull(_bindingHeader) { "The property of binding has been destroyed." }
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
                GridLayoutManager(requireContext(), getMaxColumn(UserShowAdapter.itemWidth))
            // FlexboxLayoutManager(this, FlexDirection.ROW, FlexWrap.WRAP)
            //    .apply { justifyContent = JustifyContent.SPACE_AROUND }
            addItemDecoration(AverageGridItemDecoration(UserShowAdapter.itemWidthPx))
        }
        requireArguments().let {
            if (it.containsKey("illustid")) {
                illustId = it.getLong("illustid")
                requireActivity().actionBar?.setTitle(R.string.bookmark)
                //binding.title.setText(R.string.bookmark)
                initIllustData()
            } else {
                userid = it.getLong("userid")
                if (it.containsKey("get_following")) {
                    getFollowing = it.getBoolean("get_following", true)
                    requireActivity().actionBar?.setTitle(R.string.following)
                    //binding.title.setText(R.string.following)
                    initFollowData()
                }
                else {
                    requireActivity().actionBar?.setTitle(R.string.related)
                    initRelatedData()
                }
            }
        }
    }

    private fun initFollowData() {
        userShowAdapter = UserShowAdapter(R.layout.view_usershow_item)
        binding.recyclerview.adapter = userShowAdapter
        userShowAdapter.setOnLoadMoreListener {
            if (nextUrl == null) {
                userShowAdapter.loadMoreEnd()
            } else {
                onLoadMoreUserPreview()
            }
        }

        if (userid == AppDataRepo.currentUser.userid && getFollowing) {
            _bindingHeader = ViewRestrictButtonBinding.inflate(layoutInflater)
            userShowAdapter.addHeaderView(bindingHeader.root)
            bindingHeader.apply {
                buttonPublic.isChecked = true
                buttonAll.visibility = View.GONE
                toggleRestrict.addOnButtonCheckedListener { group, checkedId, isChecked ->
                    if (isChecked) {
                        val checkedIndex = group.indexOfChild(group.findViewById(checkedId))
                        val value = InteractionUtil.visRestrictTag(checkedIndex == 2)
                        if (restrict != value) {
                            restrict = value
                            refreshFollowData()
                        }
                    }
                }
            }
        }
        refreshFollowData()
    }

    private fun refreshFollowData() {
        (
                if (getFollowing)
                    retrofitRepository.getUserFollowing(userid, restrict)
                else
                    retrofitRepository.getUserFollower(userid)
            ).subscribe({
                userData = it
                nextUrl = it.next_url
                userShowAdapter.setNewInstance(it.user_previews)
            }, {}, {}).add()
    }

    private fun onLoadMoreUserPreview() {
        retrofitRepository.getNextSearchUser(nextUrl!!).subscribe {
            nextUrl = it.next_url
            userShowAdapter.addData(it.user_previews)
            userShowAdapter.loadMoreComplete()
        }.add()
    }

    private var disposables = CompositeDisposable()
    fun Disposable.add() {
        disposables.add(this)
    }

    private fun initIllustData() {
        userListAdapter = UserListAdapter(R.layout.view_usershow_item)
        binding.recyclerview.adapter = userListAdapter
        retrofitRepository.getIllustBookmarkUsers(illustId).subscribe({
            illustData = it
            nextUrl = it.next_url
            userListAdapter.setNewInstance(it.users)
            userListAdapter.setOnLoadMoreListener {
                if (nextUrl == null) {
                    userListAdapter.loadMoreEnd()
                } else {
                    onLoadMoreUser()
                }
            }
        }, {}, {}).add()
    }

    private fun onLoadMoreUser() {
        // retrofitRepository.getIllustBookmarkUsers(illust_id,
        //    Next_url!!.substringAfter("offset=").toInt())
        retrofitRepository.getNextUser(nextUrl!!).subscribe({
            illustData = it
            nextUrl = it.next_url
            userListAdapter.addData(it.users)
            userListAdapter.loadMoreComplete()
        }, {
            userListAdapter.loadMoreFail()
            it.printStackTrace()
        }, {}).add()
    }


    private fun initRelatedData() {
        userShowAdapter = UserShowAdapter(R.layout.view_usershow_item)
        binding.recyclerview.adapter = userShowAdapter
        retrofitRepository.getUserRelated(userid).subscribe({
            userData = it
            nextUrl = it.next_url
            userShowAdapter.setNewInstance(it.user_previews)
            userShowAdapter.setOnLoadMoreListener {
                if (nextUrl == null) {
                    userShowAdapter.loadMoreEnd()
                } else {
                    onLoadMoreUserRelated()
                }
            }
        }, {
            userShowAdapter.loadMoreFail()
            it.printStackTrace()
           }, {}).add()
    }

    private fun onLoadMoreUserRelated() {
        retrofitRepository.getNextSearchUser(nextUrl!!).subscribe({
            userData = it
            nextUrl = it.next_url
            userShowAdapter.addData(it.user_previews)
            userShowAdapter.loadMoreComplete()
        }, {
            userShowAdapter.loadMoreFail()
            it.printStackTrace()
        }, {}).add()
    }
}
