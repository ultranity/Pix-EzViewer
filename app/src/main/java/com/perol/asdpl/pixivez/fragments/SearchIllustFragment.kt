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

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.adapters.PicListAdapter
import com.perol.asdpl.pixivez.adapters.PicListBtnAdapter
import com.perol.asdpl.pixivez.adapters.PicListBtnUserAdapter
import com.perol.asdpl.pixivez.databinding.FragmentSearchIllustBinding
import com.perol.asdpl.pixivez.dialog.SearchSectionDialog
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.repository.AppDataRepository
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.viewmodel.IllustfragmentViewModel
import kotlinx.coroutines.runBlocking

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "word"

/**

 * Use the [SearchIllustFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SearchIllustFragment : BaseFragment(), AdapterView.OnItemSelectedListener {
    override fun loadData() {
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (view != null) {
            selectSort = position
            if (position == 2) {
                runBlocking {
                    val user = AppDataRepository.currentUser
                    if (!user.ispro) {
                        viewModel.isPreview = true
                        Toasty.error(PxEZApp.instance, "not premium!").show()
                        viewModel.setPreview(param1!!, sort[position], null, null)
                    }
                    else {
                        viewModel.isPreview = false
                        viewModel.sort.value = position
                        viewModel.firstSetData(param1!!)
                    }
                }
            }
            else {
                viewModel.isPreview = false
                viewModel.sort.value = position
                viewModel.firstSetData(param1!!)
            }
        }
    }

    private lateinit var filter: IllustFilter
    private var exitTime = 0L
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        val searchtext = requireActivity().findViewById<TextView>(R.id.searchtext)
        filter = IllustFilter(isR18on, blockTags)
        searchIllustAdapter =
            if (PxEZApp.instance.pre.getBoolean("show_user_img_searchr", true)) {
                PicListBtnUserAdapter(
                    R.layout.view_ranking_item,
                    null,
                    filter
                )
                // singleLine = false
            }
            else {
                PicListBtnAdapter(
                    R.layout.view_recommand_item,
                    null,
                    filter
                )
            }
        searchIllustAdapter.apply {
            val searchResultHeaderView = LayoutInflater.from(requireContext()).inflate(
                R.layout.header_search_result,
                null
            )
            searchResultHeaderView.findViewById<Spinner>(R.id.spinner_result).onItemSelectedListener =
                this@SearchIllustFragment
            setHeaderView(searchResultHeaderView)
        }
        searchtext.text = param1
        binding.recyclerview.apply {
            adapter = searchIllustAdapter
            layoutManager =
                StaggeredGridLayoutManager(2 * context.resources.configuration.orientation, StaggeredGridLayoutManager.VERTICAL)
        }
        binding.fab.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            val arrayList = arrayOfNulls<String>(starnum.size)
            for (i in starnum.indices) {
                if (starnum[i] == 0) {
                    arrayList[i] = ("$param1 users入り")
                }
                else {
                    arrayList[i] = (param1 + " " + starnum[i].toString() + "users入り")
                }
            }
            builder.setTitle("users入り")
                .setItems(
                    arrayList
                ) { _, which ->

                    val query = if (starnum[which] == 0) {
                        "$param1 users入り"
                    }
                    else {
                        param1 + " " + starnum[which].toString() + "users入り"
                    }
                    viewModel.firstSetData(query)
                    binding.recyclerview.scrollToPosition(0)
                }
            builder.create().show()
        }
        val imageButton = requireActivity().findViewById<ImageButton>(R.id.imagebutton_section)
        imageButton.setOnClickListener {
            SearchSectionDialog().apply {
                arguments = Bundle().apply {
                    putString("word", param1!!)
                }
            }.show(childFragmentManager)
        }
        searchIllustAdapter.loadMoreModule.setOnLoadMoreListener {
            viewModel.onLoadMoreListen()
        }
        binding.swiperefreshLayout.setOnRefreshListener {
            runBlocking {
                val user = AppDataRepository.currentUser
                if (!user.ispro && selectSort == 2) {
                    Toasty.error(PxEZApp.instance, "not premium!").show()
                    viewModel.setPreview(
                        param1!!,
                        sort[selectSort],
                        search_target[selectTarget],
                        duration[selectDuration]
                    )
                }
                else {
                    viewModel.firstSetData(
                        param1!!
                    )
                }
            }
        }
        requireActivity().findViewById<TabLayout>(R.id.tablayout_searchresult)?.getTabAt(0)
            ?.view?.setOnClickListener {
                if ((System.currentTimeMillis() - exitTime) > 3000) {
                    exitTime = System.currentTimeMillis()
                }
                else {
                    binding.recyclerview.smoothScrollToPosition(0)
                }
            }
    }

    private val starnum = intArrayOf(50000, 30000, 20000, 10000, 5000, 1000, 500, 250, 100, 0)
    private var param1: String? = null
    private lateinit var searchIllustAdapter: PicListAdapter
    var sort = arrayOf("date_desc", "date_asc", "popular_desc")
    private var search_target =
        arrayOf("partial_match_for_tags", "exact_match_for_tags", "title_and_caption")

    var duration = arrayOf(
        null,
        "within_last_day",
        "within_last_week",
        "within_last_month",
        "within_half_year",
        "within_year"
    )
    private var selectSort: Int = 0
    private var selectTarget: Int = 0
    private var selectDuration: Int = 0
    private lateinit var viewModel: IllustfragmentViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
        }
        viewModel = ViewModelProvider(this)[IllustfragmentViewModel::class.java]
    }

    private lateinit var binding: FragmentSearchIllustBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchIllustBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initViewModel() {
        viewModel.illusts.observe(viewLifecycleOwner) {
            if (it != null) {
                updateillust(it)
            }
            else {
                searchIllustAdapter.loadMoreFail()
            }
        }
        viewModel.addIllusts.observe(viewLifecycleOwner) {
            if (it != null) {
                searchIllustAdapter.addData(it)
            }
            else {
                searchIllustAdapter.loadMoreFail()
            }
        }
        viewModel.nextUrl.observe(viewLifecycleOwner) {
            if (it == null) {
                searchIllustAdapter.loadMoreEnd()
            }
            else {
                searchIllustAdapter.loadMoreComplete()
            }
        }
        viewModel.bookmarkID.observe(viewLifecycleOwner) {
            changeToBlue(it)
        }
        viewModel.isRefresh.observe(viewLifecycleOwner) {
            binding.swiperefreshLayout.isRefreshing = it
        }
        viewModel.hideBookmarked.observe(viewLifecycleOwner) {
            if (it != null) {
                PxEZApp.instance.pre.edit().putInt(
                    "hide_bookmark_item_in_search2",
                    it
                ).apply()
                filter.hideBookmarked = it
            }
        }
    }

    private var position: Int? = null
    private fun changeToBlue(it: Long?) {
        if (it != null) {
            val item = searchIllustAdapter.getViewByPosition(
                position!!,
                R.id.linearlayout_isbookmark
            ) as LinearLayout
            item.setBackgroundColor(Color.YELLOW)
            Toasty.success(requireActivity(), "收藏成功", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateillust(it: List<Illust>?) {
        if (it != null) {
            searchIllustAdapter.setList(it)
            if (it.isEmpty()) {
                param1!!.toLongOrNull()?.let {
                    val bundle = Bundle()
                    val arrayList = LongArray(1)
                    arrayList[0] = it
                    bundle.putLongArray("illustidlist", arrayList)
                    bundle.putLong("illustid", it)
                    val intent = Intent(requireActivity(), PictureActivity::class.java)
                    intent.putExtras(bundle)
                    startActivity(intent)
                }
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @return A new instance of fragment IllustFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
            SearchIllustFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}
