/*
 * MIT License
 *
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

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.UserListAdapter
import com.perol.asdpl.pixivez.adapters.UserShowAdapter
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.repository.AppDataRepository
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.ListUserResponse
import com.perol.asdpl.pixivez.responses.SearchUserResponse
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_user_follow.*
import kotlinx.coroutines.runBlocking

class UserFollowActivity : RinkActivity() {

    private var userShowAdapter: UserShowAdapter? = null
    private var userListAdapter: UserListAdapter? = null
    private var Next_url: String? = null
    private var recyclerviewusersearch: RecyclerView? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private val retrofitRepository  = RetrofitRepository.getInstance()
    private val username: String? = null
    private var bundle: Bundle? = null
    private var userid: Long = 0
    private var illust_id: Long = 0
    private var restrict = "public"
    private var Illustdata: ListUserResponse? = null
    private var data: SearchUserResponse? = null
    private var pastdata: SearchUserResponse? = null
    internal var isfollower: Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.themeInit(this)
        setContentView(R.layout.activity_user_follow)
        setSupportActionBar(toolbar_userfollow)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        bundle = this.intent.extras
        if(bundle!!.containsKey("illust_id")) {
            illust_id = bundle!!.getLong("illust_id")
            supportActionBar!!.setTitle(R.string.bookmark)
            textView8.text =  getString(R.string.bookmark)
            initIllustData()
        }
        else{
            userid = bundle!!.getLong("user")
            isfollower = bundle!!.getBoolean("isfollower", false)
            supportActionBar!!.setTitle(R.string.following)
            textView8.text =   getString(R.string.following)
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
        spinner.setVisibility(View.GONE)
        linearLayoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
        recyclerviewusersearch = findViewById(R.id.recyclerview_usersearch)
        recyclerviewusersearch!!.layoutManager = linearLayoutManager


        val users =     if (isfollower!!) {
                            retrofitRepository.getUserFollower(userid)
                        } else
                            retrofitRepository.getUserFollowing(userid, restrict)
        users.subscribe(object : Observer<SearchUserResponse> {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onNext(searchUserResponse: SearchUserResponse) {
                        pastdata = searchUserResponse
                        data = searchUserResponse
                        Next_url = searchUserResponse.next_url
                        userShowAdapter = UserShowAdapter(R.layout.view_usershow_item)
                        userShowAdapter!!.setNewData(searchUserResponse.user_previews)
                        recyclerviewusersearch!!.adapter = userShowAdapter
                        runBlocking {
                            val user = AppDataRepository.getUser()
                            if (userid == user.userid && !isfollower!!) {
                                spinner.visibility = View.VISIBLE
                                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
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

                            } else {
                                spinner.setVisibility(View.GONE)
                            }
                        }

                        userShowAdapter!!.loadMoreModule?.setOnLoadMoreListener {
                            if (Next_url != null) {
                                retrofitRepository.getNextUser(Next_url!!)
                                    .subscribe{
                                            Next_url = it.next_url
                                            userShowAdapter!!.addData(it.user_previews)
                                            userShowAdapter!!.loadMoreModule?.loadMoreComplete()
                                        }
                            } else {
                                userShowAdapter!!.loadMoreModule?.loadMoreEnd()
                            }
                        }
                    }

                    override fun onError(e: Throwable) {

                    }

                    override fun onComplete() {

                    }
                })

    }

    private fun initIllustData() {
        spinner.setVisibility(View.GONE)
        linearLayoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
        recyclerviewusersearch = findViewById(R.id.recyclerview_usersearch)
        recyclerviewusersearch!!.layoutManager = linearLayoutManager
        retrofitRepository.getIllustBookmarkUsers(illust_id)
            .subscribe(object : Observer<ListUserResponse> {
                override fun onSubscribe(d: Disposable) {

                }
                override fun onNext(listUserResponse: ListUserResponse) {
                    Illustdata = listUserResponse
                    Next_url = listUserResponse.next_url
                    userListAdapter = UserListAdapter(R.layout.view_usershow_item)
                    recyclerviewusersearch!!.adapter = userListAdapter
                    userListAdapter!!.setNewData(listUserResponse.users)
                    userListAdapter!!.loadMoreModule?.setOnLoadMoreListener {
                        if (Next_url == null) {
                            userListAdapter!!.loadMoreModule?.loadMoreEnd()
                        }
                        else {
                            //retrofitRepository.getIllustBookmarkUsers(illust_id,
                            //    Next_url!!.substringAfter("offset=").toInt())
                            retrofitRepository.getNext<ListUserResponse>(Next_url!!)
                                .subscribe {
                                Illustdata = it
                                Next_url = it.next_url
                                userListAdapter!!.addData(it.users)
                                userListAdapter!!.loadMoreModule?.loadMoreComplete()
                            }
                        }
                    }
                }

                override fun onError(e: Throwable) {

                }

                override fun onComplete() {

                }
            })
    }

    private fun againrefresh() {
        retrofitRepository.getUserFollowing(userid, restrict)
                .subscribe(object : Observer<SearchUserResponse> {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onNext(searchUserResponse: SearchUserResponse) {
                        data = searchUserResponse
                        Next_url = searchUserResponse.next_url
                        userShowAdapter!!.setNewData(data!!.user_previews)


                    }

                    override fun onError(e: Throwable) {

                    }

                    override fun onComplete() {

                    }
                })

    }
}
