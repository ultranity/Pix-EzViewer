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

package com.perol.asdpl.pixivez.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.recyclerview.widget.GridLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.UserListAdapter
import com.perol.asdpl.pixivez.adapters.UserShowAdapter
import com.perol.asdpl.pixivez.databinding.ActivityUserFollowBinding
import com.perol.asdpl.pixivez.objects.ScreenUtil.getMaxColumn
import com.perol.asdpl.pixivez.repository.AppDataRepository
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.ListUserResponse
import com.perol.asdpl.pixivez.responses.SearchUserResponse
import com.perol.asdpl.pixivez.ui.AverageGridItemDecoration
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

class UserFollowActivity : RinkActivity() {
    companion object {
        fun start(context: Context, illustid: Long) {
            val intent = Intent(context, UserFollowActivity::class.java)
            intent.putExtra("illustid", illustid)
            context.startActivity(intent)
        }
        fun start(context: Context, userid: Long, get_following: Boolean) {
            val intent = Intent(context, UserFollowActivity::class.java)
            intent.putExtra("userid", userid)
            intent.putExtra("get_following", get_following)
            context.startActivity(intent)
        }
    }

    // TODO: view model
    private var userShowAdapter: UserShowAdapter? = null
    private var userListAdapter: UserListAdapter? = null
    private var nextUrl: String? = null
    private val retrofitRepository = RetrofitRepository.getInstance()
    private val username: String? = null // TODO: title?
    private var illustId: Long = 0
    private var illustData: ListUserResponse? = null
    private var userid: Long = 0
    private var userData: SearchUserResponse? = null
    private var getFollowing: Boolean = false
    private var restrict = "public"
    private lateinit var binding: ActivityUserFollowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarUserfollow)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        binding.recyclerviewUsersearch.apply {
            layoutManager =
                GridLayoutManager(this@UserFollowActivity, getMaxColumn(UserShowAdapter.itemWidth))
            // FlexboxLayoutManager(this, FlexDirection.ROW, FlexWrap.WRAP)
            //    .apply { justifyContent = JustifyContent.SPACE_AROUND }
            addItemDecoration(AverageGridItemDecoration(UserShowAdapter.itemWidthPx))
        }
        intent.extras?.let {
            if (it.containsKey("illustid")) {
                illustId = it.getLong("illustid")
                supportActionBar!!.setTitle(R.string.bookmark)
                //binding.title.text = getString(R.string.bookmark)
                initIllustData()
            }
            else {
                userid = it.getLong("userid")
                getFollowing = it.getBoolean("get_following", true)
                supportActionBar!!.setTitle(R.string.following)
                //binding.title.text = getString(R.string.following)
                initFollowData()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initFollowData() {
        userShowAdapter = UserShowAdapter(R.layout.view_usershow_item)
        binding.recyclerviewUsersearch.adapter = userShowAdapter
        userShowAdapter!!.setOnLoadMoreListener {
            if (nextUrl == null) {
                userShowAdapter!!.loadMoreEnd()
            } else {
                onLoadMoreUserPreview()
            }
        }

        if (userid == AppDataRepository.currentUser.userid && getFollowing) {
            binding.spinner.visibility = View.VISIBLE
            binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    when (position) {
                        0 -> {
                            restrict = "public"
                            refreshFollowData()
                        }
                        1 -> {
                            restrict = "private"
                            refreshFollowData()
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
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
                userShowAdapter!!.setNewInstance(it.user_previews)
            }, {}, {}).add()
    }

    private fun onLoadMoreUserPreview() {
        retrofitRepository.getNextUser(nextUrl!!).subscribe {
            nextUrl = it.next_url
            userShowAdapter!!.addData(it.user_previews)
            userShowAdapter!!.loadMoreComplete()
        }.add()
    }

    private var disposables = CompositeDisposable()
    fun Disposable.add() {
        disposables.add(this)
    }

    override fun finish() {
        disposables.clear()
        super.finish()
    }

    private fun initIllustData() {
        userListAdapter = UserListAdapter(R.layout.view_usershow_item)
        binding.recyclerviewUsersearch.adapter = userListAdapter
        retrofitRepository.getIllustBookmarkUsers(illustId).subscribe({
            illustData = it
            nextUrl = it.next_url
            userListAdapter!!.setNewInstance(it.users)
            userListAdapter!!.setOnLoadMoreListener {
                if (nextUrl == null) {
                    userListAdapter!!.loadMoreEnd()
                } else {
                    onLoadMoreUser()
                }
            }
        }, {}, {}).add()
    }

    private fun onLoadMoreUser() {
        // retrofitRepository.getIllustBookmarkUsers(illust_id,
        //    Next_url!!.substringAfter("offset=").toInt())
        retrofitRepository.getNext<ListUserResponse>(nextUrl!!).subscribe({
            illustData = it
            nextUrl = it.next_url
            userListAdapter!!.addData(it.users)
            userListAdapter!!.loadMoreComplete()
        }, {
            userListAdapter!!.loadMoreFail()
            it.printStackTrace()
        }, {}).add()
    }
}
