package com.perol.asdpl.pixivez.core

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Pair
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.chad.brvah.viewholder.BaseViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.LBaseQuickAdapter
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import java.util.BitSet
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.min


/**
 * basic Adapter for image item
 */
//TODO: reuse more code
//TODO: fling optimize
abstract class PicListAdapter(
    layoutResId: Int,
    val filter: PicsFilter
) :
    LBaseQuickAdapter<Illust, BaseViewHolder>(layoutResId, null) {

    var colorPrimary: Int = R.color.colorPrimary
    var colorPrimaryDark: Int = R.color.colorPrimaryDark
    var colorTransparent: Int = ThemeUtil.halftrans
    var badgeTextColor: Int = R.color.yellow
    private var quality = 0

    lateinit var mData: MutableList<Illust>

    //TODO:consider RoaringBitmap
    var blockedFlag = BitSet(128)
    var selectedFlag = BitSet(128)
    val filtered = BitSet(128) // size of mData
    private val _mBoundViewHolders = WeakHashMap<BaseViewHolder, Boolean>() //holder: visible
    private val mBoundViewHolders: Set<BaseViewHolder> =
        Collections.newSetFromMap(_mBoundViewHolders)

    //private val mBoundPosition = HashSet<Int>()
    private val mVisiblePosition = HashSet<Int>()
    fun initData(initData: MutableList<Illust>) {
        mData = initData
        resetFilterFlag()
        notifyFilterChanged()
        isUseEmpty = true
    }

    fun resetFilterFlag() {
        if (::mData.isInitialized) {
            filtered.clear()
            blockedFlag.clear()
            //CoroutineScope(Dispatchers.IO).launch {

            data.clear()
            addFilterData(mData)
        }
        //}
    }

    //fun notifyAllChanged() {
    //    notifyItemRangeChanged(headerLayoutCount, getDefItemCount() + headerLayoutCount)
    //}

    fun notifyFilterChanged() {
        //TODO: filtered.forEach { notifyItemChanged(it) } 导致oom
        //notifyItemRangeChanged(headerLayoutCount, filtered.last)
        mBoundViewHolders.forEach {
            notifyItemChanged(it.bindingAdapterPosition + headerLayoutCount)
        }
    }

    /**
     * 包括header位置的position
     */
    fun getItemRealPosition(item: Illust) = getItemPosition(item) + headerLayoutCount

    /*override fun onConreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
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
                        .setMessage(InteractionUtil.toDetailString(item))
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
        DataHolder.setIllustList(
            adapter.data.subList(
                max(position - 30, 0),
                min(
                    adapter.data.size,
                    max(position - 30, 0) + 60
                )
            )
        )
        val illust = adapter.data[position]
        val options = if (PxEZApp.animationEnable) viewPicsOptions(view, illust) else null
        PictureActivity.start(context, illust.id, position, 30, options)
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
        setEmptyView(R.layout.empty_list)
        setItemAnimation(AnimationType.ScaleIn)
        animationEnable = PxEZApp.animationEnable
        //recyclerView.setItemViewCacheSize(12)
        loadMoreModule.preLoadNumber = 12
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        isUseEmpty = false
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
        _mBoundViewHolders[holder] = false
        //println("onBindViewHolder $holder ${holder.bindingAdapterPosition}")
        //mBoundPosition.add(holder.bindingAdapterPosition)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        //println("onViewAttachedToWindow $holder ${holder.bindingAdapterPosition}")
        _mBoundViewHolders[holder] = true
        mVisiblePosition.add(holder.bindingAdapterPosition)
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
        super.onViewDetachedFromWindow(holder)
        //println("onViewDetachedFromWindow $holder ${holder.bindingAdapterPosition}")
        _mBoundViewHolders[holder] = false
        mVisiblePosition.remove(holder.bindingAdapterPosition)
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
        super.onViewDetachedFromWindow(holder)
        _mBoundViewHolders.remove(holder)
        //println("onViewRecycled $holder ${holder.bindingAdapterPosition}")
        //mBoundPosition.remove(holder.bindingAdapterPosition)
    }

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: Illust) {
        val pos = holder.bindingAdapterPosition - headerLayoutCount
        blockedFlag[pos] = filter.needBlock(item)
        if (filter.showBlocked and blockedFlag[pos]) {
            hideItemView(holder)
            return
        }
        showItemView(holder.itemView)
        // if (!context.resources.configuration.orientation==ORIENTATION_LANDSCAPE)
        if (!blockedFlag[pos])
            setFullSpan(holder, (1.0 * item.width / item.height > 2.1))

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
        // val isr18 = tags.contains("R-18") || tags.contains("R-18G")
        if (filter.blockSanity(item)) {
            Glide.with(context)
                .load(R.drawable.h).transition(withCrossFade())
                .placeholder(R.drawable.h)
                .into(mainImage)
            return
        }

        // Load Images
        mainImage.setTag(R.id.tag_first, item.meta[0].medium)
        val needSmall = if (quality == 1) {
            (1.0 * item.height / item.width > 3) || (item.width / item.height > 4)
        } else {
            item.height > 1800
        }
        val loadUrl = if (needSmall) {
            item.meta[0].square_medium
        } else {
            item.meta[0].medium
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

    private fun showItemView(itemView: View) {
        itemView.visibility = View.VISIBLE
        itemView.layoutParams.apply {
            height = LinearLayout.LayoutParams.WRAP_CONTENT
            width = LinearLayout.LayoutParams.MATCH_PARENT
        }
    }

    private fun hideItemView(holder: BaseViewHolder) {
        holder.itemView.visibility = View.GONE
        holder.itemView.layoutParams.apply {
            height = 0
            width = 0
        }
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
                filtered.set(i)
                !filter.needHide(illust)
            }
        this.data.addAll(newData)
        //emptyLayout?.findViewById<TextView>(R.id.text)?.text
        //setFooterView()
        if (recyclerView.layoutManager?.isAttachedToWindow == true) {
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

    open fun setUILike(status: Boolean, position: Int) {}
    open fun setUIFollow(status: Boolean, position: Int) {}
    open fun setUIDownload(status: Int, position: Int) {}

    open fun setUILike(status: Boolean, view: View) {}
    open fun setUIFollow(status: Boolean, view: View) {}
    open fun setUIDownload(status: Int, view: View) {}
}
