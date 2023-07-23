package com.perol.asdpl.pixivez.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.afollestad.dragselectrecyclerview.DragSelectReceiver
import com.afollestad.dragselectrecyclerview.DragSelectTouchListener
import com.afollestad.dragselectrecyclerview.Mode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.databinding.DialogPicListFilterBinding
import com.perol.asdpl.pixivez.databinding.FragmentListDownBinding
import com.perol.asdpl.pixivez.databinding.HeaderBookmarkBinding
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.objects.KotlinUtil.plus
import com.perol.asdpl.pixivez.objects.KotlinUtil.times
import com.perol.asdpl.pixivez.ui.settings.BlockViewModel
import kotlinx.coroutines.runBlocking

class DownPicListAdapter(
    layoutResId: Int,
    data: List<Illust>?,
    val filter: IllustFilter
):
    PicListAdapter(layoutResId, data?.toMutableList(), filter){
    override fun setAction(CollectMode: Int) { }

    override fun convert(holder: BaseViewHolder, item: Illust) {
        super.convert(holder, item)
        holder.itemView.findViewById<MaterialCardView>(R.id.cardview).isCheckable = true
    }
    override fun convert(holder: BaseViewHolder, item: Illust, payloads: List<Any>) {
        super.convert(holder, item, payloads)
        holder.itemView.findViewById<MaterialCardView>(R.id.cardview).
            isChecked = (payloads[0] as Boolean)
    }
}

class SelectDownloadFragment : Fragment() {
    private var _binding: FragmentListDownBinding? = null
    private var _headerBinding: HeaderBookmarkBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    protected val binding get() = _binding!!
    protected val headerBinding get() = _headerBinding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListDownBinding.inflate(inflater, container, false)
        _headerBinding = HeaderBookmarkBinding.inflate(inflater)
        return binding.root
    }
    protected lateinit var picListAdapter: DownPicListAdapter
    protected val filterModel: FilterViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filterModel.listFilter.blockTags = runBlocking { BlockViewModel.getAllTags() }
        filterModel.spanNum.value = 2 * requireContext().resources.configuration.orientation
        filterModel.applyConfig()
        configAdapter(false)
        headerBinding.imgBtnConfig.setOnClickListener {
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
                buttonAINone.isChecked = filterModel.listFilter.showAINone
                buttonAIHalf.isChecked = filterModel.listFilter.showAIHalf
                buttonAIFull.isChecked = filterModel.listFilter.showAIFull
                listOf(
                    buttonHideUserImg,
                    buttonShowUserImg
                )[filterModel.adapterType.value!!.ordinal % 2].isChecked = true
                listOf(
                    buttonHideSave,
                    buttonShowSave
                )[filterModel.adapterType.value!!.ordinal / 2].isChecked = true
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
                        showAINone = dialog.buttonAINone.isChecked
                        showAIHalf = dialog.buttonAIHalf.isChecked
                        showAIFull = dialog.buttonAIFull.isChecked
                    }
                    filterModel.applyConfig()
                    val span = dialog.sliderSpan.value.toInt()
                    layoutManager.spanCount = if (span == 0) filterModel.spanNum.value!! else span
                    val adapterVersion = ADAPTER_TYPE.values()[
                        dialog.buttonShowSave.isChecked * 2 + dialog.buttonShowUserImg.isChecked]
                    if (filterModel.adapterType.checkUpdate(adapterVersion)) {
                        val data = picListAdapter.data
                        configAdapter()
                        picListAdapter.setNewInstance(data)
                    } else {
                        //TODO: check //picListAdapter.notifyDataSetChanged()
                    }
                }
                negativeButton { }
            }
        }
        configByTAG()
        binding.recyclerview.layoutManager = StaggeredGridLayoutManager(
            filterModel.spanNum.value!!,
            StaggeredGridLayoutManager.VERTICAL
        )
        //TODO: check
        // binding.recyclerview.addItemDecoration(GridItemDecoration())
        picListAdapter.setList(allData)
    }
    private fun configAdapter(renew: Boolean = true) {
        if (renew) {
            picListAdapter.removeAllHeaderView()
        } else {
            if (::picListAdapter.isInitialized) {
                return
            }
        }
        picListAdapter = DownPicListAdapter(R.layout.view_ranking_item_s, null, filterModel.filter)
        picListAdapter.apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            headerWithEmptyEnable = true
            footerWithEmptyEnable = true
            addHeaderView(headerBinding.root)
        }
        binding.recyclerview.adapter = picListAdapter
    }
    var TAG = "Collect"
    val selectedIndices = HashSet<Int>()
    val allData = DataHolder.tmpList
    fun toggleSelected(index: Int) {
        if (selectedIndices.contains(index)) {
            selectedIndices.remove(index)
            picListAdapter.notifyItemChanged(index+1, false)
        } else {
            selectedIndices.add(index)
            picListAdapter.notifyItemChanged(index+1, true)
        }
    }
    private val receiver: DragSelectReceiver = object : DragSelectReceiver {
        override fun isSelected(index: Int): Boolean = selectedIndices.contains(index)
        override fun isIndexSelectable(index: Int) = true
        override fun getItemCount(): Int = allData?.size?:0
        override fun setSelected(index: Int, selected: Boolean) {
            // do something to mark this index as selected/unselected
            if(selected){ // && !selectedIndices.contains(index)) {
                selectedIndices.add(index)
            } else { // if(!selected) {
                selectedIndices.remove(index)
            }
            picListAdapter.notifyItemChanged(index+1, selected)
        }

    }
    @SuppressLint("SetTextI18n")
    fun configByTAG() {
        val touchListener =  DragSelectTouchListener.create(requireContext(), receiver) {
            // Configure the auto-scroll hotspot
            // hotspotHeight = resources.getDimensionPixelSize(R.dimen.default_56dp)
            // hotspotOffsetTop = 0 // default
            // hotspotOffsetBottom = 0 // default

            // Listen for auto scroll start/end
            // autoScrollListener = { isScrolling -> }

            // Or instead of the above...
            // disableAutoScroll()

            // The drag selection mode, RANGE is the default
            mode = Mode.RANGE
        }
        picListAdapter.setOnItemClickListener{ adapter, view, position ->
            //picListAdapter.viewPics(picListAdapter, view, position)
            toggleSelected(position)
        }
        picListAdapter.setOnItemLongClickListener{adapter, view, position ->
            touchListener.setIsActive(true, position)
        }
        binding.recyclerview.addOnItemTouchListener(touchListener) // important!!
        // true for active = true, 0 is the initial selected index

        val itemAnimator: ItemAnimator? = binding.recyclerview.itemAnimator
        if (itemAnimator != null) {
            itemAnimator.changeDuration = 0
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
        binding.fab.apply {
            setOnClickListener {
                println(selectedIndices)
                binding.fab.showContextMenu()
            }
            setOnLongClickListener {
                isExtended = !isExtended
                if (isExtended) text = "${selectedIndices.size}/${allData!!.size}"
                true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _headerBinding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        DataHolder.tmpList = null
    }
    companion object {
        @JvmStatic
        fun newInstance() = SelectDownloadFragment()
    }
}
