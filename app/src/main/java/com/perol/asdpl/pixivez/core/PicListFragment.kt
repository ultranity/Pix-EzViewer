/*
 * MIT License
 *
 * Copyright (c) 2023 Ultranity
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

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.databinding.FragmentListFabBinding
import com.perol.asdpl.pixivez.databinding.HeaderBookmarkBinding
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.argument
import com.perol.asdpl.pixivez.objects.argumentNullable
import com.perol.asdpl.pixivez.services.Event
import com.perol.asdpl.pixivez.services.FlowEventBus
import com.perol.asdpl.pixivez.ui.FragmentActivity
import com.perol.asdpl.pixivez.ui.home.trend.CalendarViewModel
import com.perol.asdpl.pixivez.ui.settings.BlockViewModel
import com.perol.asdpl.pixivez.ui.user.TagsShowDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

open class PicListFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance(
            mode: String,
            tabPosition: Int = 0,
            extraArgs: MutableMap<String, Any?>? = null
        ) = PicListFragment().apply {
            this.TAG = mode
            this.tabPosition = tabPosition
            this.extraArgs = extraArgs
        }
    }

    protected open var TAG: String by argument("PicListFragment")
    private var tabPosition: Int by argument(0)
    var extraArgs: MutableMap<String, Any?>? by argumentNullable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setonLoadFirstRx(TAG, extraArgs)
    }

    var isLoaded = false

    private var _binding: FragmentListFabBinding? = null
    private var _headerBinding: HeaderBookmarkBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    protected val binding get() = _binding!!
    protected val headerBinding get() = _headerBinding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListFabBinding.inflate(inflater, container, false)
        _headerBinding = HeaderBookmarkBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _headerBinding = null
    }

    override fun onResume() {
        isLoaded = viewModel.data.value != null
        super.onResume()
        Log.d("PicListFragment", "$TAG $tabPosition resume $isLoaded")
        if (!isLoaded) {
            isLoaded = true
            viewModel.onLoadFirst()
            Log.d("PicListFragment", "$TAG $tabPosition resume data reload")
        }
    }

    open fun onDataLoadedListener(illusts: MutableList<Illust>): MutableList<Illust>? {
        return illusts
    }

    protected var onDataAddedListener: (() -> Unit)? = null

    protected open lateinit var picListAdapter: PicListAdapter
    protected open fun ownerProducer(): ViewModelStoreOwner {
        return when (TAG) {
            TAG_TYPE.Rank.name -> {
                requireActivity()
            }
            else -> {
                this
            }
        }
    }

    protected open val viewModel: PicListViewModel by viewModels()
    protected val filterModel: FilterViewModel by viewModels(::ownerProducer)
    //protected open lateinit var viewModel: PicListViewModel //by viewModels()
    //protected open lateinit var filterModel: FilterViewModel //by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configAdapter(false)
        viewModel.filterModel = filterModel
        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            binding.swipeRefreshLayout.isRefreshing = it
        }
        viewModel.data.observe(viewLifecycleOwner) {
            if (it != null) {
                picListAdapter.setNewInstance(onDataLoadedListener(it))
            } else {
                picListAdapter.loadMoreFail()
            }
            isLoaded = true
            viewModel.isRefreshing.value = false
        }
        viewModel.dataAdded.observe(viewLifecycleOwner) {
            if (it != null) {
                picListAdapter.addData(it)
                onDataAddedListener?.invoke()
            } else {
                picListAdapter.loadMoreFail()
            }
        }
        viewModel.nextUrl.observe(viewLifecycleOwner) {
            if (it != null) {
                picListAdapter.loadMoreComplete()
            } else {
                picListAdapter.loadMoreEnd()
            }
        }

        filterModel.filter.blockTags =
            (runBlocking { BlockViewModel.getAllTags() })
        lifecycleScope.launch {
            //check change
            FlowEventBus.observe<Event.BlockTagsChanged>(viewLifecycleOwner) {
                filterModel.filter.blockTags = it.blockTags
                //refresh current
                picListAdapter.resetFilterFlag()
                picListAdapter.notifyFilterChanged()
            }
        }
        filterModel.spanNum.value = 2 * requireContext().resources.configuration.orientation
        filterModel.applyConfig()
        headerBinding.imgBtnConfig.setOnClickListener {
            //TODO: support other layoutManager
            val layoutManager = binding.recyclerview.layoutManager as StaggeredGridLayoutManager
            showFilterDialog(
                requireContext(),
                filterModel,
                picListAdapter,
                layoutInflater,
                layoutManager,
                ::configAdapter
            ).apply {
                if (TAG != TAG_TYPE.Collect.name)
                    neutralButton(R.string.download) {
                        DataHolder.dataListRef = viewModel.data.value
                        DataHolder.nextUrlRef = viewModel.nextUrl.value
                        FragmentActivity.start(requireContext(), TAG_TYPE.Collect.name)
                    }
            }
        }
        configByTAG()
        binding.recyclerview.layoutManager = StaggeredGridLayoutManager(
            filterModel.spanNum.value!!,
            StaggeredGridLayoutManager.VERTICAL
        )
        //TODO: check 
        // binding.recyclerview.addItemDecoration(GridItemDecoration())
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.onLoadFirst()
        }
        picListAdapter.setOnLoadMoreListener {
            viewModel.onLoadMore()
        }
    }

    open fun configByTAG() = when (TAG) {
        TAG_TYPE.UserBookmark.name -> {
            headerBinding.imgBtnSpinner.setText(R.string.publics)
            headerBinding.imgBtnSpinner.setOnClickListener {
                val id = extraArgs!!.get("userid") as Long
                viewModel.tags?.also {
                    TagsShowDialog.newInstance(id, it).also {
                        it.callback =
                            TagsShowDialog.Callback { tag, public ->
                                extraArgs!!["tag"] = tag
                                extraArgs!!["pub"] = public
                                viewModel.onLoadFirst()
                            }
                    }.show(childFragmentManager)
                } ?: {
                    //TODO: viewModel.getTags(id)
                }
            }
        }

        TAG_TYPE.Rank.name -> {
            val shareModel: CalendarViewModel by viewModels(::ownerProducer)
            shareModel.picDateShare.value?.also { extraArgs!!["pickDate"] = it }
            shareModel.picDateShare.observe(viewLifecycleOwner) {
                viewModel.onLoadFirst()
            }
            binding.recyclerview.setRecycledViewPool(shareModel.pool)
            headerBinding.imgBtnSpinner.text = "时间"
            headerBinding.imgBtnSpinner.setIconResource(R.drawable.ic_calendar)
            headerBinding.imgBtnSpinner.setOnClickListener {
                val dateNow = shareModel.getDateStr()
                shareModel.apply {
                    DatePickerDialog(
                        requireContext(),
                        { p0, year, month, day ->
                            ymd.setDate(year, month, day)
                            if (ymd.toStr() == dateNow) {
                                picDateShare.checkUpdate(null)
                            } else {
                                picDateShare.checkUpdate(ymd.toStr())
                            }
                            extraArgs!!["pickDate"] = picDateShare.value
                        },
                        ymd.year,
                        ymd.month,
                        ymd.day
                    ).also { it.datePicker.maxDate = System.currentTimeMillis() }
                        .show()
                }
            }
        }

        else -> {
            viewModel.restrict.observe(viewLifecycleOwner) {
                // fix: default value will be observed
                if (headerBinding.imgBtnSpinner.text != "TAG") {
                    viewModel.onLoadFirst()
                }
                headerBinding.imgBtnSpinner.text =
                    resources.getStringArray(R.array.restrict_type)[viewModel.restrict.value!!.ordinal]
            }
            headerBinding.imgBtnSpinner.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    val list = listItemsSingleChoice(
                        R.array.restrict_type, disabledIndices = intArrayOf(),
                        initialSelection = viewModel.restrict.value!!.ordinal
                    ) { dialog, index, text ->
                        viewModel.restrict.checkUpdate(RESTRICT_TYPE.values()[index])
                        headerBinding.imgBtnSpinner.text = text
                    }
                }
            }
        }
    }

    open fun configAdapter(renew: Boolean = true) {
        if (renew) {
            picListAdapter.removeAllHeaderView()
        } else {
            if (::picListAdapter.isInitialized) {
                binding.recyclerview.adapter = picListAdapter
                return
            }
        }
        picListAdapter = filterModel.getAdapter()
        picListAdapter.apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            headerWithEmptyEnable = true
            footerWithEmptyEnable = true
            addHeaderView(headerBinding.root)
        }
        binding.recyclerview.adapter = picListAdapter
    }
}
