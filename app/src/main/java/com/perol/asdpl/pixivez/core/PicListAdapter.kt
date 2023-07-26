package com.perol.asdpl.pixivez.core

import android.app.Activity
import android.app.ActivityOptions
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.LBaseQuickAdapter
import com.perol.asdpl.pixivez.base.last
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import java.util.BitSet
import kotlin.math.max
import kotlin.math.min

/**
 * basic Adapter for image item
 */
//TODO: reuse more code
//TODO: fling optimize
abstract class PicListAdapter(
    layoutResId: Int,
    data: MutableList<Illust>?,
    val filter: PicListFilter
) :
    LBaseQuickAdapter<Illust, BaseViewHolder>(layoutResId, data) {

    var colorPrimary: Int = R.color.colorPrimary
    var colorPrimaryDark: Int = R.color.colorPrimaryDark
    var colorTransparent: Int = ThemeUtil.halftrans
    var badgeTextColor: Int = R.color.yellow
    private var quality = 0
    var hidedFlag = BitSet(data?.size ?: 128) //TODO:consider RoaringBitmap
    var blockedFlag = BitSet(data?.size ?: 128)
    var selectedFlag = BitSet(data?.size ?: 128)
    val filtered = BitSet(data?.size ?: 128)
    fun resetFilterFlag() {
        filtered.clear()
        hidedFlag.clear()
        blockedFlag.clear()
    }

    fun notifyAllChanged() {
        notifyItemRangeChanged(headerLayoutCount, getDefItemCount() + headerLayoutCount)
    }

    fun notifyFilterChanged() {
        //TODO: filtered.forEach { notifyItemChanged(it) } 导致oom
        notifyItemRangeChanged(headerLayoutCount, filtered.last)
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
                        .create().show()
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
        val mainimage = view.findViewById<View>(R.id.item_img)
        return ActivityOptions.makeSceneTransitionAnimation(
            context as Activity,
            Pair(mainimage, "mainimage")
        ).toBundle()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        addFooterView(LayoutInflater.from(context).inflate(R.layout.foot_list, null))
        setAnimationWithDefault(AnimationType.ScaleIn)
        animationEnable = PxEZApp.animationEnable
        //recyclerView.setItemViewCacheSize(12)
        loadMoreModule.preLoadNumber = 12
        colorPrimary = ThemeUtil.getColorPrimary(context)
        colorPrimaryDark = ThemeUtil.getColorPrimaryDark(context)
        badgeTextColor = ThemeUtil.getColorHighlight(context)
        setAction(PxEZApp.CollectMode)
        quality = PxEZApp.instance.pre.getString("quality", "0")?.toInt() ?: 0
    }

    override fun convert(holder: BaseViewHolder, item: Illust) {
        val pos = holder.bindingAdapterPosition - headerLayoutCount
        if (filtered[pos]) {
            if (hidedFlag[pos]) {
                hideItemView(holder)
                return
            }
        } else {
            filtered.set(pos)
            var needHide = filter.needHide(item)
            if (!needHide) {
                if (filter.showBlocked)
                    blockedFlag[pos] = filter.needBlock(item)
                else
                    needHide = filter.needBlock(item)
            }
            if (needHide) { //(mFilterList[pos]){//
                hideItemView(holder)
                hidedFlag.set(pos)
                return
            }
        }
        showItemView(holder.itemView)
        // if (!context.resources.configuration.orientation==ORIENTATION_LANDSCAPE)
        if (!blockedFlag[pos])
            setFullSpan(holder, (1.0 * item.width / item.height > 2.1))

        val numLayout = holder.getView<TextView>(R.id.textview_num)
        if (item.type == "ugoira") {
            numLayout.text = "GIF"
            numLayout.visibility = View.VISIBLE
        } else if (item.meta_pages.isNotEmpty()) {
            val meta_pages_size = item.meta_pages.size.toString()

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
        if (!filter.R18on && item.x_restrict == 1) {
            Glide.with(context)
                .load(R.drawable.h).transition(withCrossFade())
                .placeholder(R.drawable.h)
                .into(mainImage)
            return
        }

        // Load Images
        mainImage.setTag(R.id.tag_first, item.image_urls.medium)
        val needSmall = if (quality == 1) {
            (1.0 * item.height / item.width > 3) || (item.width / item.height > 4)
        } else {
            item.height > 1800
        }
        val loadUrl = if (needSmall) {
            item.image_urls.square_medium
        } else {
            item.image_urls.medium
        }
        Glide.with(context).load(loadUrl).transition(withCrossFade())
            .placeholder(ColorDrawable(ThemeUtil.halftrans))
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .error(ContextCompat.getDrawable(context, R.drawable.ai))
            .into(object : ImageViewTarget<Drawable>(mainImage) {
                override fun setResource(resource: Drawable?) {
                    mainImage.setImageDrawable(resource)
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    if (mainImage.getTag(R.id.tag_first) === item.image_urls.medium) {
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
        super.addData(newData)
        DataHolder.pictureAdapter?.notifyDataSetChanged().also {
            DataHolder.pictureAdapter = null
        }
    }

    fun getViewByAdapterPosition(position: Int, @IdRes viewId: Int): View? {
        return getViewByPosition(position + headerLayoutCount, viewId)
    }

    open fun setUILike(status: Boolean, position: Int){}
    open fun setUIFollow(status: Boolean, position: Int){}
    open fun setUIDownload(status: Int, position: Int){}

    open fun setUILike(status: Boolean, view: View){}
    open fun setUIFollow(status: Boolean, view: View){}
    open fun setUIDownload(status: Int, view: View){}
}
