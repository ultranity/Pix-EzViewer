package com.perol.asdpl.pixivez.core

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.SimpleItemAnimator
import com.afollestad.dragselectrecyclerview.DragSelectReceiver
import com.afollestad.dragselectrecyclerview.DragSelectTouchListener
import com.afollestad.dragselectrecyclerview.Mode
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.all
import com.perol.asdpl.pixivez.base.rotate
import com.perol.asdpl.pixivez.base.setMargins
import com.perol.asdpl.pixivez.base.size
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.objects.dp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.view.NiceImageView
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
            .isChecked = selectedFlag.get(getItemRealPosition(item))
    }

    override fun convert(holder: BaseViewHolder, item: Illust, payloads: List<Any>) {
        super.convert(holder, item, payloads)
        val payload = payloads[0] as Payload
        when (payload.type) {
            "bookmarked" -> {
                setUILike(item.is_bookmarked, holder.getView<NiceImageView>(R.id.imageview_like))
            }

            "checked" -> {
                holder.itemView.findViewById<MaterialCardView>(R.id.cardview).isChecked =
                    (payload.value as Boolean)
            }
        }
    }
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
            Payload("checked", selectedFlag[index])
        )
        headerBinding.imgBtnSpinner.text = selectedHintStr()
    }

    private val receiver: DragSelectReceiver = object : DragSelectReceiver {
        override fun isSelected(index: Int): Boolean = selectedFlag[index]
        override fun isIndexSelectable(index: Int) = true
        override fun getItemCount(): Int = allData?.size ?: 0
        override fun setSelected(index: Int, selected: Boolean) {
            selectedFlag.set(index, selected)
            picListAdapter.notifyItemChanged(
                index + picListAdapter.headerLayoutCount,
                Payload("checked", selected)
            )
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
                rotate()
                downloadAllSelected()
                true
            }
        }
        FloatingActionButton(requireContext()).apply {
            binding.coordinatorlayout.addView(this)
            Glide.with(context).load(R.drawable.ic_love_mono).into(this)
            //setImageResource(R.drawable.ic_love_mono)
            setOnClickListener {
                val status = selectedNotHide().map { it.is_bookmarked }.all()
                if (status == null) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.bookmark)
                        .setMessage(selectedHintStr(true))
                        .setPositiveButton(R.string.all) { _, _ ->
                            bookmarkAllSelected(true)
                        }.setNegativeButton(R.string.flip) { _, _ ->
                            bookmarkAllSelected(null)
                        }.setNeutralButton(R.string.dislike) { _, _ ->
                            bookmarkAllSelected(false)
                        }.show()
                } else {
                    //action to inverse bookmark status
                    val target = status.not()
                    setLike(context, target)
                    //setImageResource(if (status) R.drawable.ic_heart else R.drawable.ic_love)
                    Toasty.longToast(
                        getString(if (target) R.string.bookmarked else R.string.dislike) +
                                selectedHintStr()
                    )
                    bookmarkAllSelected(target)
                    rotate()
                }
            }
            setOnLongClickListener {
                bookmarkAllSelected(true)
                rotate()
                true
            }
            setMargins(binding.fab) {
                it.marginEnd = 2 * it.marginEnd + 60.dp
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

    private fun bookmarkAllSelected(target: Boolean?) {
        selectedNotHide().forEach {
            (target ?: it.is_bookmarked.not()).let { st ->
                if (st) InteractionUtil.like(it) {
                    picListAdapter.notifyItemChanged(
                        picListAdapter.getItemRealPosition(it),
                        Payload("bookmarked")
                    )
                }
                else InteractionUtil.unlike(it) {
                    picListAdapter.notifyItemChanged(
                        picListAdapter.getItemRealPosition(it),
                        Payload("bookmarked")
                    )
                }
            }
        }
    }

    private fun downloadAllSelected() {
        Works.downloadAll(selectedNotHide())
    }

    private fun selectedNotHide(): List<Illust> =
        allData?.filterIndexed { index, _ ->
            selectedFlag[index] and !hidedFlag[index]
        } ?: listOf()

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