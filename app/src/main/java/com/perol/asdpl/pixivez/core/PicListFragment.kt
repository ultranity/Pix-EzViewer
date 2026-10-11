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
import androidx.core.view.children
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
import com.perol.asdpl.pixivez.networks.observeNetworkRecovery
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
import com.perol.asdpl.pixivez.view.SafeStaggeredGridLayoutManager
import kotlinx.coroutines.launch

open class PicListFragment : Fragment() {
    private enum class ListState {
        Hidden,
        Loading,
        Failed,
        Empty,
    }

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
    private var listStateView: View? = null
    private var loadAttempted = false

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
        listStateView = binding.root.findViewById(R.id.list_state)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _headerBinding = null
        listStateView = null
    }

    override fun onPause() {
        // 停掉惯性滑动:ViewFlinger 由 Choreographer 驱动,不随 tab 切走而停。
        // 否则切走的 tab 其 RecyclerView 仍在逐帧 scroll,而切回时数据加载回调会全量
        // 重置 adapter,两者在主线程相邻帧交错使 SGLM 的 mSpans 为 null -> recycleFromStart NPE。
        _binding?.recyclerview?.stopScroll()
        super.onPause()
    }

    override fun onResume() {
        isLoaded = viewModel.data.value != null
        super.onResume()
        CrashHandler.instance.d("PicListFragment", "$TAG $tabPosition resume $isLoaded")
        if (!isLoaded) {
            isLoaded = true
            requestFirstLoad()
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

    private fun requestFirstLoad() {
        if (viewModel.isRefreshing.value) return
        loadAttempted = true
        if (picListAdapter.mData.isEmpty()) {
            showListState(ListState.Loading)
        }
        viewModel.onLoadFirst()
    }

    private fun showListState(state: ListState) {
        val stateView = listStateView ?: return
        val loading = stateView.findViewById<View>(R.id.list_state_loading)
        val message = stateView.findViewById<android.widget.TextView>(R.id.list_state_message)
        val retry = stateView.findViewById<View>(R.id.list_state_retry)
        when (state) {
            ListState.Hidden -> {
                stateView.visibility = View.GONE
            }

            ListState.Loading -> {
                stateView.visibility = View.VISIBLE
                loading.visibility = View.VISIBLE
                message.setText(R.string.loading)
                retry.visibility = View.GONE
            }

            ListState.Failed -> {
                stateView.visibility = View.VISIBLE
                loading.visibility = View.GONE
                message.setText(R.string.list_load_failed)
                retry.visibility = View.VISIBLE
                retry.isEnabled = true
            }

            ListState.Empty -> {
                stateView.visibility = View.VISIBLE
                loading.visibility = View.GONE
                message.setText(R.string.list_empty)
                retry.visibility = View.VISIBLE
                retry.isEnabled = true
            }
        }
    }

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
        binding.recyclerview.layoutManager = SafeStaggeredGridLayoutManager(
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
            if (it && picListAdapter.mData.isEmpty()) {
                showListState(ListState.Loading)
            }
        }
        binding.root.findViewById<View>(R.id.list_state_retry).setOnClickListener {
            requestFirstLoad()
        }
        viewModel.data.observe(viewLifecycleOwner) {
            if (it != null) {
                val loaded = onDataLoadedListener(it)
                picListAdapter.initData(loaded)
                //TODO: IllustCacheRepo.register(this, picListAdapter.mData)
                if (loaded.isNotEmpty()) {
                    binding.recyclerview.edgeEffectFactory = BounceEdgeEffectFactory()
                    showListState(ListState.Hidden)
                } else {
                    showListState(ListState.Empty)
                }
            } else {
                if (picListAdapter.mData.isEmpty()) //show error only if first load
                    showListState(ListState.Failed)
                else
                    showListState(ListState.Hidden)
            }
            isLoaded = true
            viewModel.isRefreshing.value = false
        }
        viewModel.dataAdded.observe(viewLifecycleOwner) {
            if (it != null) {
                picListAdapter.addFilterData(it)
                if (picListAdapter.mData.isNotEmpty()) showListState(ListState.Hidden)
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
            requestFirstLoad()
        }
        picListAdapter.setOnLoadMoreListener {
            viewModel.onLoadMore()
        }
        if (loadAttempted && viewModel.data.value == null && !viewModel.isRefreshing.value
            && picListAdapter.mData.isEmpty()
        ) {
            showListState(ListState.Failed)
        }
        observeNetworkRecovery(viewLifecycleOwner, requireContext()) {
            if (isLoaded && viewModel.data.value == null && picListAdapter.mData.isEmpty()
                && !viewModel.isRefreshing.value
            ) {
                requestFirstLoad()
            }
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
                            requestFirstLoad()
                        }
                }.show(childFragmentManager)
            }
        }

        TAG_TYPE.Rank.name -> {
            val shareModel: CalendarViewModel by viewModels(::ownerProducer)
            shareModel.picDateShare.value?.also { extraArgs!!["pickDate"] = it }
            shareModel.picDateShare.observe(viewLifecycleOwner) {
                requestFirstLoad()
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
                    requestFirstLoad()
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
        var headers: List<View>? = null
        if (renew) {
            headers = picListAdapter.headerLayout?.children?.toList()
            picListAdapter.removeAllHeaderView()
        } else {
            if (::picListAdapter.isInitialized) {
                binding.recyclerview.adapter = picListAdapter
                return
            }
        }
        picListAdapter = filterModel.getAdapter()
        //TODO: decouple header view
        headers?.forEach { picListAdapter.addHeaderView(it) }
            ?: picListAdapter.addHeaderView(headerBinding.root)
        //binding.recyclerview.swapAdapter(picListAdapter, true)
        binding.recyclerview.adapter = picListAdapter
    }
}
