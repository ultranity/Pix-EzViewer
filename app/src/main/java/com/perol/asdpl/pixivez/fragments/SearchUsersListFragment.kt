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
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.adapters.UserShowAdapter
import com.perol.asdpl.pixivez.databinding.FragmentUserslistBinding
import com.perol.asdpl.pixivez.objects.LazyFragment
import com.perol.asdpl.pixivez.objects.ScreenUtil.getMaxColumn
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.viewmodel.UserViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "keyword"


/**
 * A simple [Fragment] subclass.
 * Use the [SearchUsersListFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SearchUsersListFragment : LazyFragment() {
    override fun loadData() {
        userViewModel.getSearchUser(keyword!!)
    }

    override fun onResume() {
        isLoaded = userShowAdapter.data.isNotEmpty()
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        userShowAdapter = UserShowAdapter(R.layout.view_usershow_item)
        binding.recyclerviewUser.adapter = userShowAdapter
        binding.recyclerviewUser.layoutManager =
            GridLayoutManager(requireContext(), getMaxColumn(400))
        //FlexboxLayoutManager(requireContext(), FlexDirection.ROW, FlexWrap.WRAP)
        //    .apply { justifyContent = JustifyContent.SPACE_AROUND }
        userShowAdapter.loadMoreModule.setOnLoadMoreListener {
            if (userViewModel.nextUrl.value != null)
                userViewModel.getNextUsers(userViewModel.nextUrl.value!!)

        }
        userShowAdapter.setOnItemClickListener { adapter, view, position ->
            val options = if (PxEZApp.animationEnable) {
                val userImage = view.findViewById<View>(R.id.imageview_usershow)
                 ActivityOptions.makeSceneTransitionAnimation(
                    requireActivity(),
                    Pair.create(userImage, "userimage")
                ).toBundle()
            } else null
            UserMActivity.start(requireContext(), userShowAdapter.data[position].user, options)
        }
    }

    private var keyword: String? = null
    private lateinit var userShowAdapter: UserShowAdapter
    private lateinit var userViewModel: UserViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            keyword = it.getString(ARG_PARAM1)
        }
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
    }

    private lateinit var binding: FragmentUserslistBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        binding = FragmentUserslistBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initViewModel() {
        userViewModel.users.observe(viewLifecycleOwner) {
            if (it != null) {
                userShowAdapter.setNewInstance(it.user_previews)
            } else {
                userShowAdapter.loadMoreModule.loadMoreFail()
            }
        }
        userViewModel.nextUrl.observe(viewLifecycleOwner) {
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
         * @param keyword Parameter 1.
         * @return A new instance of fragment SearchUsersListFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(keyword: String) =
            SearchUsersListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, keyword)
                }
            }
    }
}
