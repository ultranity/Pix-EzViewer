package com.perol.asdpl.pixivez.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.PicListAdapter
import com.perol.asdpl.pixivez.databinding.DialogPicListFilterBinding
import com.perol.asdpl.pixivez.databinding.FragmentIllustListBinding
import com.perol.asdpl.pixivez.databinding.HeaderBookmarkBinding
import com.perol.asdpl.pixivez.objects.KotlinUtil.plus
import com.perol.asdpl.pixivez.objects.KotlinUtil.times
import com.perol.asdpl.pixivez.viewmodel.ADAPTER_VERSION
import com.perol.asdpl.pixivez.viewmodel.BlockViewModel
import com.perol.asdpl.pixivez.viewmodel.FilterViewModel
import com.perol.asdpl.pixivez.viewmodel.PicListViewModel
import com.perol.asdpl.pixivez.viewmodel.RESTRICT_TYPE
import com.perol.asdpl.pixivez.viewmodel.checkUpdate
import kotlinx.coroutines.runBlocking

fun Boolean.toInt() = if (this) 1 else 0
open class PicListFragment : Fragment() {
    companion object {
        private const val ARG_PARAM1 = "mode"
        private const val ARG_PARAM2 = "tabPosition"

        @JvmStatic
        fun newInstance(mode: String, tabPosition: Int) =
            PicListFragment().apply {
                TAG = mode
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, mode)
                    putInt(ARG_PARAM2, tabPosition)
                }
            }
    }

    var TAG = "PicListFragment"
    private var tabPosition = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            TAG = it.getString(ARG_PARAM1)!!
            tabPosition = it.getInt(ARG_PARAM2)
        }
        viewModel.setonLoadFirstRx(TAG)
    }

    var isLoaded = false

    private var _binding: FragmentIllustListBinding? = null
    private var _headerBinding: HeaderBookmarkBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    protected val binding get() = _binding!!
    private val headerBinding get() = _headerBinding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIllustListBinding.inflate(inflater, container, false)
        _headerBinding = HeaderBookmarkBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _headerBinding = null
    }

    private lateinit var picListAdapter: PicListAdapter
    val viewModel: PicListViewModel by viewModels()
    private val filterModel: FilterViewModel by viewModels()
    fun initViewModel() {
        viewModel.data.observe(viewLifecycleOwner) {
            if (it != null) {
                picListAdapter.setList(it)
            } else {
                picListAdapter.loadMoreFail()
            }
            isLoaded = true
            binding.swiperefreshLayout.isRefreshing = false
        }
        viewModel.dataAdded.observe(viewLifecycleOwner) {
            if (it != null) {
                picListAdapter.addData(it)
            } else {
                picListAdapter.loadMoreModule.loadMoreFail()
            }
        }
        viewModel.nextUrl.observe(viewLifecycleOwner) {
            if (it != null) {
                picListAdapter.loadMoreComplete()
            } else {
                picListAdapter.loadMoreEnd()
            }
        }
        viewModel.restrict.observe(viewLifecycleOwner){
            headerBinding.imagebuttonShowtags.text =
                resources.getStringArray(R.array.restrict_type)[viewModel.restrict.value!!.ordinal]
            viewModel.onLoadFirst()
            binding.swiperefreshLayout.isRefreshing = true
        }
        filterModel.listFilter.blockTags = runBlocking{ BlockViewModel.getAllTags() }
        filterModel.applyConfig()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        configAdapter(false)
        headerBinding.imagebuttonConfig.setOnClickListener {
            val dialog = DialogPicListFilterBinding.inflate(layoutInflater)
            val layoutManager = binding.recyclerview.layoutManager as StaggeredGridLayoutManager
            dialog.apply {
                sliderSpan.value =
                    layoutManager.spanCount.toFloat() //filterModel.spanNum.value!!.toFloat()
                buttonBookmarked.isChecked = filterModel.listFilter.showBookmarked
                buttonNotBookmarked.isChecked = filterModel.listFilter.showNotBookmarked
                buttonDownloaded.isChecked = filterModel.listFilter.showDownloaded
                buttonNotDownloaded.isChecked = filterModel.listFilter.showNotDownloaded
                buttonFollowed.isChecked = filterModel.listFilter.showFollowed
                buttonNotFollowed.isChecked = filterModel.listFilter.showNotFollowed
                listOf(
                    buttonHideUserImg,
                    buttonShowUserImg
                )[filterModel.adapterVersion.value!!.ordinal % 2].isChecked = true
                listOf(
                    buttonHideSave,
                    buttonShowSave
                )[filterModel.adapterVersion.value!!.ordinal / 2].isChecked = true
            }
            MaterialDialog(requireContext()).show {
                customView(view = dialog.root, scrollable = true)
                positiveButton {
                    filterModel.listFilter.apply {
                        showBookmarked = dialog.buttonBookmarked.isChecked
                        showNotBookmarked = dialog.buttonNotBookmarked.isChecked
                        showDownloaded = dialog.buttonDownloaded.isChecked
                        showNotDownloaded = dialog.buttonNotDownloaded.isChecked
                        showFollowed = dialog.buttonFollowed.isChecked
                        showNotFollowed = dialog.buttonNotFollowed.isChecked
                    }
                    filterModel.applyConfig()
                    val span = dialog.sliderSpan.value.toInt()
                    layoutManager.spanCount = if (span == 0) filterModel.spanNum.value!! else span
                    val adapterVersion = ADAPTER_VERSION.values()[
                            dialog.buttonShowSave.isChecked * 2 + dialog.buttonShowUserImg.isChecked]
                    if (filterModel.adapterVersion.checkUpdate(adapterVersion)) {
                        val data = picListAdapter.data
                        configAdapter()
                        picListAdapter.setNewInstance(data)
                    }
                    else {
                        picListAdapter.notifyDataSetChanged()
                    }
                }
                negativeButton { }
            }
        }
        headerBinding.imagebuttonShowtags.setOnClickListener {
            MaterialDialog(requireContext()).show {
                val list = listItemsSingleChoice(R.array.restrict_type, disabledIndices= intArrayOf(),
                    initialSelection = viewModel.restrict.value!!.ordinal){ dialog, index, text ->
                    viewModel.restrict.checkUpdate(RESTRICT_TYPE.values()[index])
                }
            }
        }
        filterModel.spanNum.value = 2 * requireContext().resources.configuration.orientation
        binding.recyclerview.layoutManager = StaggeredGridLayoutManager(
            filterModel.spanNum.value!!,
            StaggeredGridLayoutManager.VERTICAL
        )
        binding.swiperefreshLayout.setOnRefreshListener {
            viewModel.onLoadFirst()
        }
        picListAdapter.setOnLoadMoreListener {
            viewModel.onLoadMore()
        }
    }

    private fun configAdapter(renew:Boolean = true) {
        if(renew) {
            picListAdapter.removeAllHeaderView()
        }
        else{
            if (::picListAdapter.isInitialized){
                return
            }
        }
        picListAdapter = filterModel.getAdapter()
        picListAdapter.apply{
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            headerWithEmptyEnable = true
            footerWithEmptyEnable = true
            addHeaderView(headerBinding.root)
        }
        binding.recyclerview.adapter = picListAdapter
    }
}
