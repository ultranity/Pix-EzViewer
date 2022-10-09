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


import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.adapters.UserShowAdapter
import com.perol.asdpl.pixivez.databinding.FragmentUserslistBinding
import com.perol.asdpl.pixivez.objects.LazyFragment
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.viewmodel.UserViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"


/**
 * A simple [Fragment] subclass.
 * Use the [UsersListFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class UsersListFragment : LazyFragment() {
    override fun loadData() {
        userViewModel.getSearchUser(param1!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userShowAdapter = UserShowAdapter(R.layout.view_usershow_item)
        binding.recyclerviewUser.adapter = userShowAdapter
        binding.recyclerviewUser.layoutManager = LinearLayoutManager(activity)
        userShowAdapter.loadMoreModule.setOnLoadMoreListener {
            if (userViewModel.nextUrl.value != null)
                userViewModel.getNextUsers(userViewModel.nextUrl.value!!)

        }
        userShowAdapter.setOnItemClickListener { adapter, view, position ->
            val intent = Intent(requireActivity().applicationContext, UserMActivity::class.java)
            intent.putExtra("data", userShowAdapter.data[position].user.id)

            if (PxEZApp.animationEnable) {
                val userImage = view.findViewById<View>(R.id.imageview_usershow)
                val options = ActivityOptions.makeSceneTransitionAnimation(
                    requireActivity(),
                    Pair.create(userImage, "UserImage")
                )
                startActivity(intent, options.toBundle())
            } else
                startActivity(intent)
        }
    }

    private var param1: String? = null
    lateinit var userShowAdapter: UserShowAdapter
    lateinit var userViewModel: UserViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)

        }
        lazyLoad()
    }

    private lateinit var binding: FragmentUserslistBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment

		binding = FragmentUserslistBinding.inflate(inflater, container, false)
		return binding.root
    }

    fun lazyLoad() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)


        userViewModel.users.observe(this) {
            if (it != null) {
                userShowAdapter.addData(it.user_previews)
            } else {
                userShowAdapter.loadMoreModule.loadMoreFail()
            }
        }

        userViewModel.nextUrl.observe(this) {
            if (it != null) {
                userShowAdapter.loadMoreModule.loadMoreComplete()
            } else {
                userShowAdapter.loadMoreModule.loadMoreEnd()
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment UsersListFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
                UsersListFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                    }
                }
    }
}
