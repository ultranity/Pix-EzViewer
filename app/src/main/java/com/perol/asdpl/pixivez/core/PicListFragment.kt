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

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.checkUpdate
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.databinding.FragmentListFabBinding
import com.perol.asdpl.pixivez.databinding.HeaderFilterBinding
import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.ToastQ
import com.perol.asdpl.pixivez.objects.argument
import com.perol.asdpl.pixivez.objects.argumentNullable
import com.perol.asdpl.pixivez.services.Event
import com.perol.asdpl.pixivez.services.FlowEventBus
import com.perol.asdpl.pixivez.ui.FragmentActivity
import com.perol.asdpl.pixivez.ui.home.trend.CalendarViewModel
import com.perol.asdpl.pixivez.ui.settings.BlockViewModel
import com.perol.asdpl.pixivez.ui.user.TagsShowDialog
import com.perol.asdpl.pixivez.view.BounceEdgeEffectFactory
import kotlinx.coroutines.launch

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
        filterModel.TAG = TAG
    }

    var isLoaded = false

    private var _binding: FragmentListFabBinding? = null
    private var _headerBinding: HeaderFilterBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    protected val binding get() = _binding!!
    protected val headerBinding get() = _headerBinding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListFabBinding.inflate(inflater, container, false)
        _headerBinding = HeaderFilterBinding.inflate(inflater)
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
        CrashHandler.instance.d("PicListFragment", "$TAG $tabPosition resume $isLoaded")
        if (!isLoaded) {
            isLoaded = true
            viewModel.onLoadFirst()
            CrashHandler.instance.d("PicListFragment", "$TAG $tabPosition data reload")
        } else {
            //DataHolder.modifiedIllusts.forEach{
            //    picListAdapter.notifyItemChanged(picListAdapter.getItemRealPosition(it))
            //}
            //picListAdapter.notifyBoundViewChanged()
        }
    }

    open fun onDataLoadedListener(illusts: MutableList<Illust>): MutableList<Illust> {
        return illusts
    }

    protected open val onDataAddedListener: (() -> Unit) = { updateHintText() }

    open lateinit var picListAdapter: PicListAdapter
    protected open fun ownerProducer(): ViewModelStoreOwner {
        return when (TAG) {
            TAG_TYPE.Rank.name -> {
                requireParentFragment()//requireActivity()
            }

            else -> this
        }
    }

    protected open val viewModel: PicListViewModel by viewModels()
    protected val filterModel: FilterViewModel by viewModels(::ownerProducer)
    private var emptyLoadNum: Int = 0
    private var manualLoadNum: Int = 1

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //viewModel.filterModel = filterModel
        filterModel.init(TAG)
        filterModel.spanNum = 2 * requireContext().resources.configuration.orientation
        binding.recyclerview.layoutManager = StaggeredGridLayoutManager(
            filterModel.spanNum,
            StaggeredGridLayoutManager.VERTICAL
        )
        configAdapter(false)
        picListAdapter.checkEmptyListener = {
            if (picListAdapter.data.isEmpty()) picListAdapter.showEmptyView(false)
            if (it == 0) {
                emptyLoadNum++
                //stop autoload as a warning if filter risky!
                if ((picListAdapter.mData.isNotEmpty()) and (emptyLoadNum >= manualLoadNum)) {
                    picListAdapter.isAutoLoadMore = false
                    picListAdapter.setOnManualLoadMoreListener {
                        emptyLoadNum = 0
                        manualLoadNum *= 2
                        picListAdapter.isAutoLoadMore = true
                        picListAdapter.setOnManualLoadMoreListener(null)
                    }
                }
            }
            updateHintText()
        }
        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            binding.swipeRefreshLayout.isRefreshing = it
        }
        viewModel.data.observe(viewLifecycleOwner) {
            if (it != null) {
                picListAdapter.initData(onDataLoadedListener(it))
                //TODO: IllustCacheRepo.register(this, picListAdapter.mData)
                binding.recyclerview.edgeEffectFactory = BounceEdgeEffectFactory()
            } else {
                if (picListAdapter.mData.isEmpty()) //show error only if first load
                    picListAdapter.showEmptyView(true)
            }
            isLoaded = true
            viewModel.isRefreshing.value = false
        }
        viewModel.dataAdded.observe(viewLifecycleOwner) {
            if (it != null) {
                picListAdapter.addFilterData(it)
                onDataAddedListener.invoke()
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

        filterModel.filter.blockTags = BlockViewModel.getBlockTagString()
        lifecycleScope.launch {
            //check change
            FlowEventBus.observe<Event.BlockTagsChanged>(viewLifecycleOwner) {
                filterModel.filter.blockTags = it.blockTags
                it.diff?.apply { picListAdapter.resetFilterTag(this, it.add) }
                    ?: picListAdapter.resetFilterFlag()
            }

            FlowEventBus.observe<Event.BlockUsersChanged>(viewLifecycleOwner) {
                filterModel.filter.blockUsers = it.blockUsers
                it.diff?.apply { picListAdapter.resetFilterUser(this, it.add) }
                    ?: picListAdapter.resetFilterFlag()
            }
        }
        headerBinding.imgBtnConfig.setOnLongClickListener {
            picListAdapter.isAutoLoadMore = picListAdapter.isAutoLoadMore.not()
            ToastQ.post(if (picListAdapter.isAutoLoadMore) R.string.all_stop else R.string.loading)
            true
        }
        headerBinding.imgBtnConfig.setOnClickListener {
            //TODO: support other layoutManager
            val layoutManager = binding.recyclerview.layoutManager as StaggeredGridLayoutManager
            showFilterDialog(
                requireContext(),
                this,
                filterModel,
                layoutInflater,
                layoutManager,
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
        //TODO: check 
        // binding.recyclerview.addItemDecoration(GridItemDecoration())
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.onLoadFirst()
        }
        picListAdapter.setOnLoadMoreListener {
            viewModel.onLoadMore()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateHintText() {
        headerBinding.imgBtnConfig.text = "${picListAdapter.data.size}/${picListAdapter.mData.size}"
    }

    open fun configByTAG() = when (TAG) {
        TAG_TYPE.UserBookmark.name -> {
            headerBinding.imgBtnR.setText(R.string.publics)
            headerBinding.imgBtnR.setOnClickListener {
                val id = extraArgs!!["userid"] as Int
                TagsShowDialog.newInstance(id).also {
                    it.callback =
                        TagsShowDialog.Callback { tag, public ->
                            extraArgs!!["tag"] = tag
                            extraArgs!!["pub"] = public
                            headerBinding.imgBtnR.setText(if (public == "public") R.string.publics else R.string.privates)
                            viewModel.onLoadFirst()
                        }
                }.show(childFragmentManager)
            }
        }

        TAG_TYPE.Rank.name -> {
            val shareModel: CalendarViewModel by viewModels(::ownerProducer)
            shareModel.picDateShare.value?.also { extraArgs!!["pickDate"] = it }
            shareModel.picDateShare.observe(viewLifecycleOwner) {
                viewModel.onLoadFirst()
            }
            //TODO: RecycledViewPool 导致 list 间 item click/longclick事件 错误配置
            //binding.recyclerview.setRecycledViewPool(shareModel.pool)
            headerBinding.imgBtnR.apply {
                setText(R.string.choose_date)
                setIconResource(R.drawable.ic_calendar)
                setTextColor(
                    AppCompatResources.getColorStateList(
                        requireContext(),
                        com.google.android.material.R.color.mtrl_tabs_icon_color_selector_colored
                    )
                )
            }
            headerBinding.imgBtnR.setOnClickListener {
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
            val restrictTypes = resources.getStringArray(R.array.restrict_type)
            viewModel.restrict.observe(viewLifecycleOwner) {
                if (viewModel.restrict.currentVersion > 0)
                    viewModel.onLoadFirst()
                headerBinding.imgBtnR.text = restrictTypes[viewModel.restrict.value.ordinal]
            }
            headerBinding.imgBtnR.setOnClickListener {
                MaterialDialogs(requireContext()).show {
                    setSingleChoiceItems(restrictTypes, viewModel.restrict.value.ordinal)
                    { dialog, index ->
                        viewModel.restrict.checkUpdate(RESTRICT_TYPE.entries[index])
                        headerBinding.imgBtnR.text = restrictTypes[index]
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
        //TODO: decouple header view
        picListAdapter.addHeaderView(headerBinding.root)
        //binding.recyclerview.swapAdapter(picListAdapter, true)
        binding.recyclerview.adapter = picListAdapter
    }
}
