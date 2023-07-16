package com.perol.asdpl.pixivez.dialog

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.TagsShowAdapter
import com.perol.asdpl.pixivez.databinding.ViewTagsShowBinding
import com.perol.asdpl.pixivez.objects.InteractionUtil.visRestrictTag
import com.perol.asdpl.pixivez.objects.argument
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.viewmodel.UserBookMarkViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

// UserBookMarkFragment
class TagsShowDialog : BaseVBDialogFragment<ViewTagsShowBinding>() {
    companion object {
        fun newInstance(uid: Long): TagsShowDialog = TagsShowDialog().apply{
            this.uid = uid
        }
    }
    private var uid:Long by argument()
    val viewModel: UserBookMarkViewModel by viewModels(ownerProducer = { requireParentFragment() })
    var callback: Callback? = null

    interface Callback {
        fun onClick(string: String, public: String)
    }

    override fun onCreateDialogBinding(builder: MaterialAlertDialogBuilder) {
        var nextUrl = viewModel.tags.value!!.next_url
        val tagsShowAdapter = TagsShowAdapter(R.layout.view_tags_show_item, viewModel.tags.value!!.bookmark_tags)
        tagsShowAdapter.setOnItemClickListener { adapter, view, position ->
            callback!!.onClick(
                tagsShowAdapter.data[position].name,
                visRestrictTag(binding.tablayout.selectedTabPosition != 0)
            )
            this.dismiss()
        }
        binding.recyclerviewTags.adapter = tagsShowAdapter
        binding.recyclerviewTags.layoutManager = GridLayoutManager(context, 1)
        binding.all.setOnClickListener {
            callback!!.onClick(
                "",
                visRestrictTag(binding.tablayout.selectedTabPosition != 0)
            )
            this.dismiss()
        }
        binding.tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {}

            override fun onTabUnselected(p0: TabLayout.Tab?) {}

            override fun onTabSelected(p0: TabLayout.Tab?) {
                if (p0 != null) {
                    val pub = visRestrictTag(p0.position != 0)
                    RetrofitRepository.getInstance().getIllustBookmarkTags(viewModel.id, pub)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io()).subscribe({
                            nextUrl = it.next_url
                            if (it.bookmark_tags.isNullOrEmpty()) {
                                callback!!.onClick("", pub)
                                this@TagsShowDialog.dismiss()
                            }
                            tagsShowAdapter.setList(it.bookmark_tags)
                        }, {}, {}).add()
                }
            }
        })
        tagsShowAdapter.setOnLoadMoreListener {
            if (!nextUrl.isNullOrBlank()) {
                RetrofitRepository.getInstance().getNextTags(nextUrl!!)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                        {
                            nextUrl = it.next_url
                            tagsShowAdapter.addData(it.bookmark_tags)
                            tagsShowAdapter.loadMoreComplete()
                        }, { tagsShowAdapter.loadMoreFail() }, {}).add()
            }
            else {
                tagsShowAdapter.loadMoreEnd()
            }
        }
    }
}
