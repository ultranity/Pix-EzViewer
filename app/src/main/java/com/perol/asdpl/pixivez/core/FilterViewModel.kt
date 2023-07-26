/*
 * MIT License
 *
 * Copyright (c) 2022 ultranity
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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.google.android.material.button.MaterialButton
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.databinding.DialogPicListFilterBinding
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.objects.KotlinUtil.plus
import com.perol.asdpl.pixivez.objects.KotlinUtil.times
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlin.reflect.KMutableProperty0

enum class ADAPTER_TYPE {
    PIC_LIKE,
    PIC_USER_LIKE,
    PIC_BTN,
    PIC_USER_BTN,
}

data class PicListFilter(
    var R18on: Boolean = false,
    var blockTags: List<String>? = null,
    var blockUser: List<Long>? = null,
    var max_sanity: Int = 8,

    var minLike: Int = 0,
    var minViewed: Int = 0,
    var minLikeRate: Float = 0f,

    var showBookmarked: Boolean = true,
    var showNotBookmarked: Boolean = true,
    var showDownloaded: Boolean = true,
    var showNotDownloaded: Boolean = true,

    var showIllust: Boolean = true,
    var showManga: Boolean = true,
    var showUgoira: Boolean = true,

    var showFollowed: Boolean = true,
    var showNotFollowed: Boolean = true,

    var showAINone: Boolean = true,
    var showAIHalf: Boolean = true,
    var showAIFull: Boolean = true,

    var showPrivate: Boolean = true,
    var showPublic: Boolean = true,

    var startTime: Long = 0,
    var endTime: Long = Long.MAX_VALUE,

    var sortTime: Boolean = false,
    var sortLike: Boolean = false,
    var sortLikeRate: Boolean = false,
) {
    fun needHide(item: Illust): Boolean =
        (!showBookmarked && item.is_bookmarked) || (!showNotBookmarked && !item.is_bookmarked) ||
        (!showFollowed && item.user.is_followed) || (!showNotFollowed && !item.user.is_followed) ||
        (!showDownloaded && FileUtil.isDownloaded(item)) || (!showNotDownloaded && !FileUtil.isDownloaded(item))||
        (!showAINone && item.illust_ai_type==0) || (!showAIHalf && item.illust_ai_type==1) ||(!showAIFull && item.illust_ai_type==2) ||
        (!showIllust && (item.type == "illust")) || (!showManga && (item.type == "manga")) || (!showUgoira && (item.type == "ugoira"))
        //|| (minLike > item.total_bookmarks) || (minViewed > item.total_view) || (minLikeRate*item.total_view> item.total_bookmarks)

    fun needBlock(item: Illust): Boolean {
        if (blockTags.isNullOrEmpty() and blockUser.isNullOrEmpty()) {
            return false
        }
        val tags = item.tags.map{ it.name }
        if (tags.isNotEmpty()) {
            // if (blockTags.intersect(tags).isNotEmpty())
            for (i in blockTags!!) {
                if (tags.contains(i)) {
                    return true
                }
            }
        }
        return blockUser!!.contains(item.user.id)
    }

    fun applyTo(filter: IllustFilter){
        filter.let{
            it.R18on = R18on
            it.hideBookmarked = !showBookmarked + (!showNotBookmarked)*2
            it.hideDownloaded = !showDownloaded
            it.sortCoM = if (!showManga) 1 else (if (showIllust) 0 else 2)
            it.blockTags = blockTags
        }
    }

}
class FilterViewModel : ViewModel() {
    var TAG = "FilterViewModel"
    val spanNum = MutableLiveData(2)
    val filter = IllustFilter()
    val listFilter = PicListFilter()
    var adapterType = MutableLiveData(ADAPTER_TYPE.PIC_USER_LIKE)
    var modeCollect = false
    init {
        listFilter.R18on = PxEZApp.instance.pre.getBoolean("r18on", false)
    }
    fun applyConfig() = listFilter.applyTo(filter)
    fun getAdapter() = if (modeCollect){
        DownPicListAdapter(R.layout.view_ranking_item_s, null, filter)
    } else when (adapterType.value) {
        ADAPTER_TYPE.PIC_BTN -> PicListBtnAdapter(R.layout.view_recommand_item, null, filter)
        ADAPTER_TYPE.PIC_USER_BTN -> PicListBtnUserAdapter(R.layout.view_ranking_item, null, filter)
        ADAPTER_TYPE.PIC_LIKE -> PicListXAdapter(R.layout.view_recommand_item_s, null, filter)
        ADAPTER_TYPE.PIC_USER_LIKE -> PicListXUserAdapter(
            R.layout.view_ranking_item_s,
            null,
            filter
        )

        else -> PicListXUserAdapter(R.layout.view_ranking_item_s, null, filter)
    }
}

private val propsMap = hashSetOf<Pair<MaterialButton, KMutableProperty0<Boolean>>>()
private fun pairBtnFilter(button: MaterialButton, props: KMutableProperty0<Boolean>) {
    propsMap.add(button to props)
    button.isChecked = props.get()
}

fun showFilterDialog(
    context: Context,
    filterModel: FilterViewModel,
    picListAdapter: PicListAdapter,
    layoutInflater: LayoutInflater,
    layoutManager: StaggeredGridLayoutManager,
    configAdapter: () -> Unit
): MaterialDialog {
    val dialog = DialogPicListFilterBinding.inflate(layoutInflater)
    dialog.apply {
        if (!PxEZApp.instance.pre.getBoolean("init_download_filter", false))
            toggleDownload.setOnClickListener {
                showFilterDownloadDialog(context)
            }
        sliderSpan.value =
            layoutManager.spanCount.toFloat() //filterModel.spanNum.value!!.toFloat()
        val filter = filterModel.listFilter

        pairBtnFilter(showBookmarked, filter::showBookmarked)
        pairBtnFilter(showNotBookmarked, filter::showNotBookmarked)

        pairBtnFilter(showDownloaded, filter::showDownloaded)
        pairBtnFilter(showNotDownloaded, filter::showNotDownloaded)

        pairBtnFilter(showFollowed, filter::showFollowed)
        pairBtnFilter(showNotFollowed, filter::showNotFollowed)

        pairBtnFilter(showIllust, filter::showIllust)
        pairBtnFilter(showManga, filter::showManga)
        pairBtnFilter(showUgoira, filter::showUgoira)

        pairBtnFilter(showAINone, filter::showAINone)
        pairBtnFilter(showAIHalf, filter::showAIHalf)
        pairBtnFilter(showAIFull, filter::showAIFull)

        pairBtnFilter(showPrivate, filter::showPrivate)
        pairBtnFilter(showPublic, filter::showPublic)
        listOf(
            showHideUserImg,
            showShowUserImg
        )[filterModel.adapterType.value!!.ordinal % 2].isChecked = true
        listOf(
            showHideSave,
            showShowSave
        )[filterModel.adapterType.value!!.ordinal / 2].isChecked = true
    }
    return MaterialDialog(context).show {
        customView(view = dialog.root, scrollable = true)
        positiveButton {
            propsMap.forEach {
                it.second.set(it.first.isChecked)
            }
            filterModel.applyConfig()
            picListAdapter.notifyFilterChanged()
            picListAdapter.filtered.clear()
            val span = dialog.sliderSpan.value.toInt()
            layoutManager.spanCount = if (span == 0) filterModel.spanNum.value!! else span
            val adapterVersion = ADAPTER_TYPE.values()[
                dialog.showShowSave.isChecked * 2 + dialog.showShowUserImg.isChecked]
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

fun showFilterDownloadDialog(context: Context) = MaterialDialog(context).show {
    PxEZApp.instance.pre.edit {
        putBoolean("init_download_filter", true)
    }
    title(R.string.hide_downloaded)
    message(R.string.hide_downloaded_detail) {
        html()
    }
    positiveButton(R.string.I_know) { }
    neutralButton(R.string.download) {
        val uri = Uri.parse(context.getString(R.string.plink))
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }
}