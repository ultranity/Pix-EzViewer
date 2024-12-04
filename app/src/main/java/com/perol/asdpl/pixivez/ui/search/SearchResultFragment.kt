package com.perol.asdpl.pixivez.ui.search

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.DMutableLiveData
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.checkUpdate
import com.perol.asdpl.pixivez.core.PicListArgs
import com.perol.asdpl.pixivez.core.PicListExtraArgs
import com.perol.asdpl.pixivez.core.PicListFragment
import com.perol.asdpl.pixivez.core.PicListViewModel
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import java.util.Calendar

class SearchResultFragment : PicListFragment() {

    private val keyword: String by PicListExtraArgs()
    override fun onDataLoadedListener(illusts: MutableList<Illust>): MutableList<Illust> {
        // jump to illust pid if search result empty and looks like a pid
        if (illusts.isEmpty()) {
            keyword.toIntOrNull()?.let {
                PictureActivity.start(requireContext(), it)
            }
        }
        return super.onDataLoadedListener(illusts)
    }

    override val viewModel: SearchResultViewModel by viewModels({ requireActivity() })
    override fun configByTAG() {
        viewModel.sort.observeAfterSet(viewLifecycleOwner) {
            viewModel.onLoadFirst()
        }
        viewModel.query = keyword
        headerBinding.imgBtnR.setText(R.string.sort_by)
        headerBinding.imgBtnR.setOnClickListener {
            MaterialDialogs(requireContext()).show {
                setSingleChoiceItems(
                    R.array.sort, viewModel.sort.value
                ) { dialog, index ->
                    viewModel.sort.checkUpdate(index)
                    headerBinding.imgBtnR.text = resources.getStringArray(R.array.sort)[index]
                }
            }
        }
        binding.fab.visibility = View.VISIBLE
        binding.fab.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            val arrayList = Array<String>(viewModel.starnumT.size) { "" }
            for (i in viewModel.starnumT.indices) {
                arrayList[i] = if (viewModel.starnumT[i] == -1) "$keyword 000"
                else if (viewModel.starnumT[i] == 0) "$keyword users入り"
                else "$keyword ${viewModel.starnumT[i]}users入り"
            }
            builder.setTitle("users入り")
                .setItems(
                    arrayList
                ) { _, which ->
                    viewModel.query = arrayList[which]
                    viewModel.onLoadFirst()
                    binding.recyclerview.scrollToPosition(0)
                }
            builder.show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(word: String) =
            SearchResultFragment().apply {
                this.TAG = "Search"
                this.extraArgs = mutableMapOf("keyword" to word)
            }
    }
}

fun Calendar?.generateDateString(): String? = this?.run {
    "${get(Calendar.YEAR)}-${get(Calendar.MONTH) + 1}-${get(Calendar.DATE)}"
}

class SearchResultViewModel : PicListViewModel() {
    var keyword: String by PicListArgs()
    lateinit var query: String
    val starnumT = intArrayOf(50000, 30000, 20000, 10000, 5000, 1000, 500, 250, 100, 0, -1)
    val sortT = arrayOf("date_desc", "date_asc", "popular_desc")
    val searchTargetT =
        arrayOf("partial_match_for_tags", "exact_match_for_tags", "title_and_caption")
    val durationT = arrayOf(
        null,
        "within_last_day",
        "within_last_week",
        "within_last_month",
        "within_half_year",
        "within_year"
    )

    var isPreview = false
    val sort = DMutableLiveData(0)
    var searchTarget = 0
    private val selectDuration: Int = 0
    val startDate = MutableLiveData<Calendar?>()
    val endDate = MutableLiveData<Calendar?>()

    fun setPreview(word: String, sort: String, search_target: String?, duration: String?) {
        isRefreshing.value = true
        subscribeNext({
            retrofit.api.getSearchIllustPreview(word, sort, search_target, null, duration)
        }, data, nextUrl) {
            isRefreshing.value = false
        }
    }

    fun firstSetData(word: String) {
        isRefreshing.value = true
        //TODO: WTF?
        if ((startDate.value != null || endDate.value != null) &&
            (startDate.value != null && endDate.value != null) &&
            startDate.value!!.timeInMillis >= endDate.value!!.timeInMillis
        ) {
            startDate.value = null
            endDate.value = null
        }
        //if (!AppDataRepository.currentUser.ispro && sort.value == 2)
        if (isPreview && sort.value == 2) {
            Toasty.error(PxEZApp.instance, "not premium!")
            setPreview(
                word,
                sortT[sort.value],
                searchTargetT[searchTarget],
                durationT[selectDuration]
            )
        } else {
            subscribeNext({
                retrofit.api.getSearchIllust(
                    word,
                    sortT[sort.value],
                    searchTargetT[searchTarget],
                    startDate.value.generateDateString(),
                    endDate.value.generateDateString(),
                    null
                )
            }, data, nextUrl, ::localSortByBookmarks) {
                isRefreshing.value = false
            }
        }
    }

    override fun onLoadFirst() {
        firstSetData(query)
    }

    //TODO: UI标识
    private fun localSortByBookmarks(illusts: MutableList<Illust>): MutableList<Illust> {
        if (sort.value == 2) illusts.sortByDescending { it.total_bookmarks }
        return illusts
    }

    override fun onLoadMore() {
        nextUrl.value?.let {
            subscribeNext(
                { retrofit.getIllustNext(it) },
                dataAdded,
                nextUrl,
                ::localSortByBookmarks
            )
        }
    }
}
