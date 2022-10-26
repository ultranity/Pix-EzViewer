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
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

class UserFollowActivity : RinkActivity() {
    companion object {
        fun start(context: Context, id: Long, getFollower: Boolean) {
            val intent = Intent(context, UserFollowActivity::class.java)
            intent.putExtra("user", id)
            intent.putExtra("getFollower", getFollower)
            context.startActivity(intent)
        }
    }

    // TODO: view model
    private var userShowAdapter: UserShowAdapter? = null
    private var userListAdapter: UserListAdapter? = null
    private var Next_url: String? = null
    private val retrofitRepository = RetrofitRepository.getInstance()
    private val username: String? = null // TODO: title?
    private var bundle: Bundle? = null
    private var userid: Long = 0
    private var illust_id: Long = 0
    private var restrict = "public"
    private var illustData: ListUserResponse? = null
    private var data: SearchUserResponse? = null
    private var pastdata: SearchUserResponse? = null
    internal var getFollower: Boolean? = false
    private lateinit var binding: ActivityUserFollowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarUserfollow)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.spinner.visibility = View.GONE
        binding.recyclerviewUsersearch.apply{
            layoutManager =
                GridLayoutManager(this@UserFollowActivity, getMaxColumn(UserShowAdapter.itemWidth))
            // FlexboxLayoutManager(this, FlexDirection.ROW, FlexWrap.WRAP)
            //    .apply { justifyContent = JustifyContent.SPACE_AROUND }
            addItemDecoration(AverageGridItemDecoration(UserShowAdapter.itemWidthPx))
        }
        bundle = this.intent.extras
        if (bundle!!.containsKey("illust_id")) {
            illust_id = bundle!!.getLong("illust_id")
            supportActionBar!!.setTitle(R.string.bookmark)
            binding.textView8.text = getString(R.string.bookmark)
            initIllustData()
        }
        else {
            userid = bundle!!.getLong("user")
            getFollower = bundle!!.getBoolean("getFollower", false)
            supportActionBar!!.setTitle(R.string.following)
            binding.textView8.text = getString(R.string.following)
            initFollowData()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initFollowData() {
        val getUsers = if (getFollower!!) {
            retrofitRepository.getUserFollower(userid)
        }
        else {
            retrofitRepository.getUserFollowing(userid, restrict)
        }
        getUsers.subscribe(object : Observer<SearchUserResponse> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(searchUserResponse: SearchUserResponse) {
                pastdata = searchUserResponse
                data = searchUserResponse
                Next_url = searchUserResponse.next_url
                userShowAdapter = UserShowAdapter(R.layout.view_usershow_item)
                userShowAdapter!!.setNewInstance(searchUserResponse.user_previews)
                binding.recyclerviewUsersearch.adapter = userShowAdapter
                userShowAdapter!!.loadMoreModule.setOnLoadMoreListener {
                    if (Next_url != null) {
                        retrofitRepository.getNextUser(Next_url!!).subscribe {
                            Next_url = it.next_url
                            userShowAdapter!!.addData(it.user_previews)
                            userShowAdapter!!.loadMoreModule.loadMoreComplete()
                        }.add()
                    }
                    else {
                        userShowAdapter!!.loadMoreModule.loadMoreEnd()
                    }
                }
                val user = AppDataRepository.currentUser
                if (userid == user.userid && !getFollower!!) {
                    binding.spinner.visibility = View.VISIBLE
                    binding.spinner.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>,
                                view: View,
                                position: Int,
                                id: Long
                            ) {
                                when (position) {
                                    0 -> {
                                        restrict = "public"
                                        againrefresh()
                                    }
                                    1 -> {
                                        restrict = "private"
                                        againrefresh()
                                    }
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {
                            }
                        }
                }
                else {
                    binding.spinner.visibility = View.GONE
                }
            }

            override fun onError(e: Throwable) {}
            override fun onComplete() {}
        })
    }

    var disposables = CompositeDisposable()
    fun Disposable.add() {
        disposables.add(this)
    }

    override fun finish() {
        disposables.clear()
        super.finish()
    }

    private fun initIllustData() {
        retrofitRepository.getIllustBookmarkUsers(illust_id).subscribe({
            illustData = it
            Next_url = it.next_url
            userListAdapter = UserListAdapter(R.layout.view_usershow_item)
            binding.recyclerviewUsersearch.adapter = userListAdapter
            userListAdapter!!.setNewInstance(it.users)
            userListAdapter!!.loadMoreModule.setOnLoadMoreListener {
                if (Next_url == null) {
                    userListAdapter!!.loadMoreModule.loadMoreEnd()
                }
                else {
                    // retrofitRepository.getIllustBookmarkUsers(illust_id,
                    //    Next_url!!.substringAfter("offset=").toInt())
                    retrofitRepository.getNext<ListUserResponse>(Next_url!!).subscribe({
                        illustData = it
                        Next_url = it.next_url
                        userListAdapter!!.addData(it.users)
                        userListAdapter!!.loadMoreModule.loadMoreComplete()
                    }, {
                        userListAdapter!!.loadMoreModule.loadMoreFail()
                        it.printStackTrace()
                    }, {}).add()
                }
            }
        }, {}, {}).add()
    }

    private fun againrefresh() {
        retrofitRepository.getUserFollowing(userid, restrict)
            .subscribe(object : Observer<SearchUserResponse> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(searchUserResponse: SearchUserResponse) {
                    data = searchUserResponse
                    Next_url = searchUserResponse.next_url
                    userShowAdapter!!.setNewInstance(data!!.user_previews)
                }

                override fun onError(e: Throwable) {}
                override fun onComplete() {}
            })
    }
}
