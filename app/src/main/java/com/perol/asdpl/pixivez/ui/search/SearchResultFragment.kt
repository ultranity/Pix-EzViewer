package com.perol.asdpl.pixivez.ui.search

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.DMutableLiveData
import com.perol.asdpl.pixivez.core.PicListFragment
import com.perol.asdpl.pixivez.core.PicListViewModel
import com.perol.asdpl.pixivez.core.arg
import com.perol.asdpl.pixivez.core.checkUpdate
import com.perol.asdpl.pixivez.core.extraArg
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import java.util.Calendar

class SearchResultFragment: PicListFragment() {

    private val keyword:String by extraArg()
    override fun onDataLoadedListener(illusts: MutableList<Illust>): MutableList<Illust>? {
        // jump to illust pid if search result empty and looks like a pid
        if (illusts.isEmpty()) {
            keyword.toLongOrNull()?.let {
                PictureActivity.start(requireContext(), it)
            }
        }
        return super.onDataLoadedListener(illusts)
    }
    override val viewModel:SearchResultViewModel by viewModels({requireActivity()})
    override fun configByTAG() {
        viewModel.sort.observeAfterSet(viewLifecycleOwner) {
            viewModel.onLoadFirst()
        }
        viewModel.query = keyword
        headerBinding.imgBtnSpinner.setText(R.string.sort_by)
        headerBinding.imgBtnSpinner.setOnClickListener {
            MaterialDialog(requireContext()).show {
                val list = listItemsSingleChoice(
                    R.array.sort, disabledIndices = intArrayOf(),
                    initialSelection = viewModel.sort.value!!
                ) { dialog, index, text ->
                    viewModel.sort.checkUpdate(index)
                    headerBinding.imgBtnSpinner.text = text
                }
            }
        }
        binding.fab.visibility = View.VISIBLE
        binding.fab.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            val arrayList = arrayOfNulls<String>(viewModel.starnumT.size)
            for (i in viewModel.starnumT.indices) {
                if (viewModel.starnumT[i] == 0) {
                    arrayList[i] = ("$keyword users入り")
                } else {
                    arrayList[i] = (keyword + " " + viewModel.starnumT[i].toString() + "users入り")
                }
            }
            builder.setTitle("users入り")
                .setItems(
                    arrayList
                ) { _, which ->

                    viewModel.query = if (viewModel.starnumT[which] == 0) {
                        "$keyword users入り"
                    } else {
                        keyword + " " + viewModel.starnumT[which].toString() + "users入り"
                    }
                    viewModel.onLoadFirst()
                    binding.recyclerview.scrollToPosition(0)
                }
            builder.create().show()
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
class SearchResultViewModel:PicListViewModel() {
    var keyword: String by arg()
    lateinit var query: String
    val starnumT = intArrayOf(50000, 30000, 20000, 10000, 5000, 1000, 500, 250, 100, 0)
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
        retrofit.getSearchIllustPreview(word, sort, search_target, null, duration)
            .subscribeNext(data, nextUrl) {
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
            Toasty.error(PxEZApp.instance, "not premium!").show()
            setPreview(
                word,
                sortT[sort.value!!],
                searchTargetT[searchTarget],
                durationT[selectDuration]
            )
        } else {
            retrofit.getSearchIllust(
                word,
                sortT[sort.value!!],
                searchTargetT[searchTarget],
                startDate.value.generateDateString(),
                endDate.value.generateDateString(),
                null
            ).subscribeNext(data, nextUrl, ::localSortByBookmarks) {
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
            retrofit.getNextIllusts(it) //getNextIllustRecommended
                .subscribeNext(dataAdded, nextUrl, ::localSortByBookmarks)
        }
    }
}
