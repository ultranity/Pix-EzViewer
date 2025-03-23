package com.perol.asdpl.pixivez.core

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Pair
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.chad.brvah.viewholder.BaseViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.DMutableLiveData
import com.perol.asdpl.pixivez.base.KotlinUtil.Int
import com.perol.asdpl.pixivez.base.KotlinUtil.alsoIf
import com.perol.asdpl.pixivez.base.LBaseQuickAdapter
import com.perol.asdpl.pixivez.base.UtilFunc.compute
import com.perol.asdpl.pixivez.base.UtilFunc.flip
import com.perol.asdpl.pixivez.base.UtilFunc.forEachIndexed
import com.perol.asdpl.pixivez.base.UtilFunc.forEachNotIndexed
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import com.perol.asdpl.pixivez.view.NiceImageView
import com.perol.asdpl.pixivez.view.ResizeTransformation
import jp.wasabeef.glide.transformations.gpu.PixelationFilterTransformation
import java.util.BitSet
import java.util.WeakHashMap


/**
 * basic Adapter for image item
 */
//TODO: reuse more code
//TODO: fling optimize
abstract class PicListAdapter(
    val filter: PicsFilter,
    layoutResId: Int,
) :
    LBaseQuickAdapter<Illust, BaseViewHolder>(layoutResId, null) {

    var colorPrimary: Int = R.color.colorPrimary
    var colorPrimaryDark: Int = R.color.colorPrimaryDark
    var colorTransparent: Int = ThemeUtil.halftrans
    var badgeTextColor: Int = R.color.yellow
    private var quality = 0
    protected open val withUser = false
    protected val likeDataID = "likeLiveData".hashCode()
    protected val followDataID = "followLiveData".hashCode()

    var mData: MutableList<Illust> = arrayListOf()

    //TODO:consider RoaringBitmap
    var hidedFlag = BitSet(128) // size of mData
    var blockedFlag = BitSet(128)
    var selectedFlag = BitSet(128)
    var checkEmptyListener: ((Int) -> Unit)? = null // size of mData

    class VHStatus(
        val holder: BaseViewHolder,
        //var item:Illust,
        val position: Int,
        var visible: Boolean = false,
    )

    private val _mBoundViewHolders = WeakHashMap<BaseViewHolder, VHStatus>() //holder: visible
    private val mBoundViewHolders: MutableCollection<VHStatus> = _mBoundViewHolders.values

    //private val mBoundPosition = HashSet<Int>()
    private val mVisiblePosition = HashSet<Int>()
    fun initData(initData: MutableList<Illust>) {
        mData = initData
        resetFilterFlag()
    }

    fun resetFilterFlag(notify: Boolean = true) {
        hidedFlag.clear()
        blockedFlag.clear()
        //CoroutineScope(Dispatchers.IO).launch {
        data.clear()
        if (mData.isNotEmpty())
            addFilterData(mData)
        if (notify)
            notifyFilterChanged()
    }

    fun resetFilterTag(tag: String, add: Boolean = false, notify: Boolean = true) {
        //TODO: not finished
        val visFlag = blockedFlag.compute { or(hidedFlag) }.flip()
        var count = 0

        if (add) {
            visFlag.forEachIndexed { i, index ->
                if (mData[index].tags.any { it.name == tag }) {
                    //if (!filter.showBlocked)notifyItemRemoved(index - count) else
                    notifyItemChanged(index - count)
                    count++
                    blockedFlag.set(i)
                }
            }
        } else {
            visFlag.forEachNotIndexed { i, index ->
                if (mData[index].tags.any { it.name == tag }) {
                    filter.needBlock(mData[index]).not().alsoIf {
                        //if (!filter.showBlocked)notifyItemInserted(index + count) else
                        notifyItemChanged(index + count)
                        count++
                        blockedFlag.clear(i)
                    }
                }
            }
        }
    }

    fun resetFilterUser(UID: Int, add: Boolean = false, notify: Boolean = true) {
        val visFlag = blockedFlag.compute { or(hidedFlag) }.flip()
        var count = 0
        if (add) {
            visFlag.forEachIndexed { i, index ->
                if (mData[index].user.id == UID) {
                    if (!filter.showBlocked) notifyItemRemoved(index - count)
                    else notifyItemChanged(index - count)
                    count++
                    blockedFlag.set(i)
                }
            }
        } else {
            visFlag.forEachNotIndexed { i, index ->
                if (mData[index].user.id == UID) {
                    filter.needBlock(mData[index]).not().alsoIf {
                        if (!filter.showBlocked) notifyItemInserted(index + count)
                        else notifyItemChanged(index + count)
                        count++
                        blockedFlag.clear(i)
                    }
                }
            }
        }
        //notifyFilterChanged()
    }

    fun notifyFilterChanged() {
        if (mData.isEmpty() or mBoundViewHolders.isEmpty()) {
            notifyDataSetChanged()
        } else {
            //TODO: filtered.forEach { notifyItemChanged(it) } 导致oom
            //notifyItemRangeChanged(headerLayoutCount, filtered.last)
            mBoundViewHolders.forEach {
                notifyItemChanged(it.position)
            }
        }
    }

    /**
     * 包括header位置的position
     */
    fun getItemRealPosition(item: Illust) = getItemPosition(item) + headerLayoutCount

    /*override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return createBaseViewHolder(parent, layoutResId)
    }*/
    private fun setFullSpan(holder: RecyclerView.ViewHolder, isFullSpan: Boolean) {
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            layoutParams.isFullSpan = isFullSpan
        }
    }

    open fun setAction(CollectMode: Int) {
        if (CollectMode == 2) {
            setOnItemClickListener { adapter, view, position ->
                if (position > adapter.data.size)
                    return@setOnItemClickListener
                (adapter.data as MutableList<Illust>)[position].let { item ->
                    Works.imageDownloadAll(item)
                    setUIDownload(1, position)
                    if (!item.is_bookmarked) {
                        InteractionUtil.like(item) { setUILike(true, position) }
                    }
                    if (!item.user.is_followed) {
                        //todo: hint auto follow
                        InteractionUtil.follow(item) { setUIFollow(true, position) }
                    }
                }
            }
            setOnItemLongClickListener { adapter, view, position ->
                viewPics(adapter as PicListAdapter, view, position)
                true
            }
        } else {
            setOnItemClickListener { adapter, view, position ->
                viewPics(adapter as PicListAdapter, view, position)
            }
            setOnItemLongClickListener { adapter, view, position ->
                if (position > adapter.data.size)
                    return@setOnItemLongClickListener true
                // show detail of illust
                (adapter.data as MutableList<Illust>)[position].let { item ->
                    MaterialAlertDialogBuilder(context as Activity)
                        .setMessage(item.toDetailString())
                        .setTitle("Detail")
                        .setPositiveButton(R.string.save) { _, _ ->
                            Works.imageDownloadAll(item)
                            setUIDownload(1, position)
                        }
                        .setNeutralButton(R.string.like) { _, _ ->
                            InteractionUtil.like(item) { setUILike(true, position) }
                        }
                        .setNegativeButton(R.string.follow) { _, _ ->
                            InteractionUtil.follow(item) { setUIFollow(true, position) }
                        }
                        .show()
                }
                true
            }
        }
    }

    fun viewPics(adapter: PicListAdapter, view: View, position: Int) {
        DataHolder.setIllustList(adapter.data)
//      DataHolder.setIllustList(adapter.data.subList(
//                max(position - 30, 0),
//                min(
//                    adapter.data.size,
//                    max(position - 30, 0) + 60
//                )
//            ).toList())
        val illust = adapter.data[position]
        val options = if (PxEZApp.animationEnable) viewPicsOptions(view, illust) else null
        PictureActivity.start(context, illust.id, position, options = options)
    }

    open fun viewPicsOptions(view: View, illust: Illust): Bundle {
        //val mainimage = view.findViewById<View>(R.id.item_img)
        return ActivityOptions.makeSceneTransitionAnimation(
            context as Activity,
            Pair(view, "shared_element_container")
            //Pair(mainimage, "mainimage")
        ).toBundle()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        //setFooterView(LayoutInflater.from(context).inflate(R.layout.foot_list, null))
        setItemAnimation(AnimationType.ScaleIn)
        animationEnable = PxEZApp.animationEnable
        //recyclerView.setItemViewCacheSize(12)
        loadMoreModule.preLoadNumber = 12
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        headerWithEmptyEnable = true
        footerWithEmptyEnable = true
        //loadMoreModule.isEnableLoadMoreIfNotFullPage = false
        colorPrimary = ThemeUtil.getColorPrimary(context)
        colorPrimaryDark = ThemeUtil.getColorPrimaryDark(context)
        badgeTextColor = ThemeUtil.getColorHighlight(context)
        setAction(PxEZApp.CollectMode)
        quality = PxEZApp.instance.pre.getString("quality", "0")?.toInt() ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        _mBoundViewHolders[holder] = VHStatus(holder, position, false)
        //println("onBindViewHolder $holder ${holder.bindingAdapterPosition}")
        //mBoundPosition.add(holder.bindingAdapterPosition)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        //println("onViewAttachedToWindow $holder ${holder.bindingAdapterPosition}")
        _mBoundViewHolders[holder]?.visible = true
        mVisiblePosition.add(holder.bindingAdapterPosition)
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
        super.onViewDetachedFromWindow(holder)
        //println("onViewDetachedFromWindow $holder ${holder.bindingAdapterPosition}")
        _mBoundViewHolders[holder]?.visible = false
        mVisiblePosition.remove(holder.bindingAdapterPosition)
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
        super.onViewDetachedFromWindow(holder)
        _mBoundViewHolders.remove(holder)
        //println("onViewRecycled $holder ${holder.bindingAdapterPosition}")
        //mBoundPosition.remove(holder.bindingAdapterPosition)
    }

    //Fix setOnItemClick when using RecyclerViewPool
    protected val adapterID = "adapter".hashCode()
    override fun setOnItemClick(v: View, position: Int) {
        super.mOnItemClickListener?.onItemClick(v.getTag(adapterID) as PicListAdapter, v, position)
    }

    override fun setOnItemLongClick(v: View, position: Int): Boolean {
        return mOnItemLongClickListener?.onItemLongClick(
            v.getTag(adapterID) as PicListAdapter,
            v,
            position
        ) == true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        val likeLiveData = DMutableLiveData(lastValue = false, onlyIfChanged = true)
        likeLiveData.observeAfterSet(parent.context as LifecycleOwner) {
            setUILike(it, holder)
        }
        holder.itemView.setTag(likeDataID, likeLiveData)
        if (withUser) {
            val followLiveData = DMutableLiveData(lastValue = false, onlyIfChanged = true)
            followLiveData.observeAfterSet(parent.context as LifecycleOwner) {
                setUIFollow(it, holder)
            }
            holder.itemView.setTag(followDataID, followLiveData)
        }
        return holder
    }

    protected val illustID = "illust".hashCode()
    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: Illust) {
        (holder.itemView.getTag(likeDataID) as DMutableLiveData<Boolean>?)?.let {
            (holder.itemView.getTag(illustID) as Illust?)?.removeBinder(it)
            it.triggerValue(item.is_bookmarked)
            item.addBinder("${item.id}|${this.hashCode()}", it)
        }
        if (withUser) {
            (holder.itemView.getTag(followDataID) as DMutableLiveData<Boolean>?)?.let {
                (holder.itemView.getTag(illustID) as Illust?)?.user?.removeBinder(it)
                it.triggerValue(item.user.is_followed)
                item.user.addBinder("${item.id}-${this.hashCode()}", it)
            }
        }
        holder.itemView.setTag(illustID, item)
        holder.itemView.setTag(adapterID, this)
        val pos = holder.bindingAdapterPosition - headerLayoutCount
        //Alt1: on the flight test
        blockedFlag[pos] = filter.needBlock(item)
        val blockSanity = filter.blockSanity(item)
        if (!filter.showBlocked and (blockedFlag[pos] or blockSanity)) {
            hideItemView(holder)
            return
        }
        showItemView(holder.itemView)
        val needSmall = if (quality == 1) {
            (1.0 * item.height / item.width > 3) || (1.0 * item.width / item.height > 4)
        } else {
            item.height > 1800
        }
        val needFullSpan = !needSmall and ((1.0 * item.width / item.height) >
                ((context.resources.configuration.orientation == ORIENTATION_LANDSCAPE).Int() * 2 + 2.1))
        if (!blockedFlag[pos])
            setFullSpan(holder, needFullSpan)

        val numLayout = holder.getView<TextView>(R.id.textview_num)
        if (item.type == "ugoira") {
            numLayout.text = "GIF"
            numLayout.visibility = View.VISIBLE
        } else if (item.meta.size > 1) {
            val meta_pages_size = item.meta.size.toString()

            numLayout.text = when (item.type) {
                "illust" -> meta_pages_size
                //"ugoira" -> "GIF"
                else -> "C$meta_pages_size" // "manga"
            }
            numLayout.visibility = View.VISIBLE
        } else {
            numLayout.visibility = View.GONE
        }
        val mainImage = holder.getView<ImageView>(R.id.item_img)
        if (blockedFlag[pos]) {
            Glide.with(context)
                .load(R.drawable.ic_action_block).transition(withCrossFade())
                .placeholder(R.drawable.ai)
                .into(mainImage)
            return
        }

        // Load Images
        mainImage.setTag(R.id.tag_first, item.meta[0].medium)
        val loadUrl = if (needSmall) {
            item.meta[0].square_medium
        } else {
            item.meta[0].medium
        }
        // val isr18 = tags.contains("R-18") || tags.contains("R-18G")
        if (blockSanity) { //show blur image
            Glide.with(context)
                .load(loadUrl)
                .transition(withCrossFade())
                .placeholder(R.drawable.h)
                .transform(ResizeTransformation(8), PixelationFilterTransformation(4F))
                .into(mainImage)
            return
        }
        Glide.with(context).load(loadUrl).transition(withCrossFade())
            .placeholder(ColorDrawable(ThemeUtil.halftrans))
            .error(ContextCompat.getDrawable(context, R.drawable.ai))
            .into(object : ImageViewTarget<Drawable>(mainImage) {
                override fun setResource(resource: Drawable?) {
                    mainImage.setImageDrawable(resource)
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    if (mainImage.getTag(R.id.tag_first) === item.meta[0].medium) {
                        super.onResourceReady(resource, transition)
                    }
                }
            })
    }

    override fun convert(holder: BaseViewHolder, item: Illust, payloads: List<Any>) {
        super.convert(holder, item, payloads)
        val payload = payloads[0] as Payload
        when (payload.type) {
            "bookmarked" -> {
                setUILike(item.is_bookmarked, holder)
            }

            "followed" -> {
                setUIFollow(
                    item.user.is_followed,
                    holder.getView<NiceImageView>(R.id.imageview_user)
                )
            }
        }
    }

    private fun showItemView(itemView: View) {
        itemView.visibility = View.VISIBLE
        itemView.layoutParams.apply {
            height = LinearLayout.LayoutParams.WRAP_CONTENT
            width = LinearLayout.LayoutParams.MATCH_PARENT
        }
    }

    private fun hideItemView(holder: BaseViewHolder) {
        holder.itemView.visibility = View.GONE
        holder.itemView.layoutParams.apply { width = 0; height = 0 }
    }

    //TODO: ?????
    override fun addData(newData: Collection<Illust>) {
        throw NotImplementedError("not filtered")
    }

    fun addFilterData(newData: Collection<Illust>): Int {
        if (newData != mData)
            mData.addAll(newData)
        val newData = newData //chunked(32)
            .filterIndexed { i, illust ->
                //see Alt1: which also on the flight test
                val i = i + mData.size - newData.size
                filter.needBlock(illust).alsoIf { blockedFlag.set(i) }
                !filter.needHide(illust, checkSanity = !filter.showBlocked)
                    .alsoIf { hidedFlag.set(i) }
            }
        this.data.addAll(newData)
        checkEmptyListener?.invoke(newData.size)
        //emptyLayout?.findViewById<TextView>(R.id.text)?.text
        //setFooterView()
        if (recyclerViewOrNull?.layoutManager?.isAttachedToWindow == true) {
            notifyItemRangeInserted(
                this.data.size - newData.size + headerLayoutCount,
                newData.size
            )
            compatibilityDataSizeChanged(newData.size)
        }
        DataHolder.picPagerAdapter?.notifyDataSetChanged().also {
            DataHolder.picPagerAdapter = null
        }
        return newData.size
    }

    fun getViewByAdapterPosition(position: Int, @IdRes viewId: Int): View? {
        return getViewByPosition(position + headerLayoutCount, viewId)
    }

    open fun setUILike(status: Boolean, holder: BaseViewHolder) {}
    open fun setUIFollow(status: Boolean, holder: BaseViewHolder) {}

    open fun setUILike(status: Boolean, position: Int) {}
    open fun setUIFollow(status: Boolean, position: Int) {}
    open fun setUIDownload(status: Int, position: Int) {}

    open fun setUILike(status: Boolean, view: View) {}
    open fun setUIFollow(status: Boolean, view: View) {}
    open fun setUIDownload(status: Int, view: View) {}
}
