package com.perol.asdpl.pixivez.core

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.SimpleItemAnimator
import com.afollestad.dragselectrecyclerview.DragSelectReceiver
import com.afollestad.dragselectrecyclerview.DragSelectTouchListener
import com.afollestad.dragselectrecyclerview.Mode
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.Works
import org.roaringbitmap.RoaringBitmap
import java.util.BitSet


class DownPicListAdapter(
    layoutResId: Int,
    data: MutableList<Illust>?,
    val filter: IllustFilter
) :
    PicListXAdapter(layoutResId, data, filter) {

    val toggle
        get() = !selectedFlag.isEmpty

    override fun setAction(CollectMode: Int) {}

    override fun convert(holder: BaseViewHolder, item: Illust) {
        super.convert(holder, item)
        holder.itemView.findViewById<MaterialCardView>(R.id.cardview)
            .isChecked = selectedFlag.get(getItemPosition(item))
    }

    override fun convert(holder: BaseViewHolder, item: Illust, payloads: List<Any>) {
        super.convert(holder, item, payloads)
        holder.itemView.findViewById<MaterialCardView>(R.id.cardview).isChecked =
            (payloads[0] as Boolean)
    }
}

private val BitSet.size: Int
    get() = cardinality()
val RoaringBitmap.size: Int
    get() = cardinality

fun RoaringBitmap.set(x: Int, status: Boolean) {
    if (status)
        add(x)
    else
        remove(x)
}

class SelectDownloadFragment : PicListFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            //val illusts = savedInstanceState.getParcelableArray("illusts")
            //DataHolder.tmpList = illusts?.toList() as MutableList<Illust>
            val hide = savedInstanceState.getByteArray("hided")
            val select = savedInstanceState.getByteArray("selected")
            hidedFlag = BitSet.valueOf(hide)
            selectedFlag = BitSet.valueOf(select)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        filterModel.modeCollect = true
        super.onViewCreated(view, savedInstanceState)
        //picListAdapter.setList(allData)
        binding.swipeRefreshLayout.isEnabled = false
    }

    override var TAG = "Collect"
    lateinit var hidedFlag: BitSet // = RoaringBitmap()
    lateinit var selectedFlag: BitSet // = RoaringBitmap()
    val selectedButHided: BitSet
        get() = (selectedFlag.clone() as BitSet).apply { and(hidedFlag) }
    val allData by lazy { DataHolder.tmpList }
    fun toggleSelected(index: Int) {
        selectedFlag.flip(index)
        picListAdapter.notifyItemChanged(
            index + picListAdapter.headerLayoutCount,
            selectedFlag[index]
        )
        headerBinding.imgBtnSpinner.text = selectedHintStr()
    }

    private val receiver: DragSelectReceiver = object : DragSelectReceiver {
        override fun isSelected(index: Int): Boolean = selectedFlag[index]
        override fun isIndexSelectable(index: Int) = true
        override fun getItemCount(): Int = allData?.size ?: 0
        override fun setSelected(index: Int, selected: Boolean) {
            selectedFlag.set(index, selected)
            picListAdapter.notifyItemChanged(index + picListAdapter.headerLayoutCount, selected)
            headerBinding.imgBtnSpinner.text = selectedHintStr()
        }
    }

    override fun configAdapter(renew: Boolean) {
        super.configAdapter(renew)
        if (::selectedFlag.isInitialized) {
            picListAdapter.selectedFlag = selectedFlag
            picListAdapter.hidedFlag = hidedFlag
        } else {
            selectedFlag = picListAdapter.selectedFlag
            hidedFlag = picListAdapter.hidedFlag
        }
        val touchListener = DragSelectTouchListener.create(requireContext(), receiver) {
            // hotspotHeight = resources.getDimensionPixelSize(R.dimen.default_56dp)
            // hotspotOffsetTop = 0 // default
            // hotspotOffsetBottom = 0 // default
            mode = Mode.RANGE
        }
        picListAdapter.setOnItemClickListener { adapter, view, position ->
            if (selectedFlag.isEmpty.not())
                toggleSelected(position)
            else
                picListAdapter.viewPics(picListAdapter, view, position)
        }
        picListAdapter.setOnItemLongClickListener { adapter, view, position ->
            touchListener.setIsActive(true, position)
        }
        binding.recyclerview.addOnItemTouchListener(touchListener)

        val itemAnimator: ItemAnimator? = binding.recyclerview.itemAnimator
        if (itemAnimator != null) {
            itemAnimator.changeDuration = 0
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
    }

    override fun configByTAG() {
        binding.fab.apply {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_action_download)
            setOnClickListener {
                //println(selectedIndices)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.download)
                    .setMessage(selectedHintStr(true))
                    .setPositiveButton(R.string.ok) { _, _ ->
                        downloadAllSelected()
                    }.show()
            }
            setOnLongClickListener {
                //print("${selectedIndices.size}/${allData!!.size}")
                Toasty.longToast(
                    getString(R.string.download) +
                            selectedHintStr()
                )
                downloadAllSelected()
                true
            }
        }
        headerBinding.imgBtnSpinner.setIconResource(R.drawable.ic_action_rank)
        headerBinding.imgBtnSpinner.text = selectedHintStr()
        headerBinding.imgBtnSpinner.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.action_select)
                .setPositiveButton(R.string.all) { _, _ ->
                    selectedFlag.set(0, allData!!.size)
                    picListAdapter.notifyFilterChanged()
                }
                .setNegativeButton(R.string.all_cancel) { _, _ ->
                    selectedFlag.clear()
                    picListAdapter.notifyFilterChanged()
                }
                .setNeutralButton(R.string.select_reverse) { _, _ ->
                    selectedFlag.flip(0, allData!!.size)
                    picListAdapter.notifyFilterChanged()
                }
                .setOnDismissListener {
                    headerBinding.imgBtnSpinner.text = selectedHintStr()
                }.show()
        }
    }

    private fun downloadAllSelected() {
        allData?.let {
            Works.downloadAll(it.filterIndexed { index, _ ->
                selectedFlag[index] and !hidedFlag[index]
            })
        }
    }

    private fun selectedHintStr(debug: Boolean = false): String {
        val skiped = selectedButHided.size
        return "${selectedFlag.size - skiped}/${allData!!.size}" +
                if (debug) "(${skiped} skipped)" else ""
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putByteArray("hided", hidedFlag.toByteArray())
        outState.putByteArray("selected", selectedFlag.toByteArray())
        //outState.putParcelableArray("illusts", DataHolder.tmpList?.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
        //DataHolder.tmpList = null
    }
}
