package com.perol.asdpl.pixivez.ui.user

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseVBDialogFragment
import com.perol.asdpl.pixivez.databinding.ViewTagsShowBinding
import com.perol.asdpl.pixivez.objects.InteractionUtil.visRestrictTag
import com.perol.asdpl.pixivez.objects.argument
import com.perol.asdpl.pixivez.data.model.BookMarkTagsResponse
import com.perol.asdpl.pixivez.objects.argumentNullable

// UserBookMarkFragment
class TagsShowDialog : BaseVBDialogFragment<ViewTagsShowBinding>() {
    companion object {
        fun newInstance(uid: Long, tags: BookMarkTagsResponse, index:Int=0) = TagsShowDialog().apply {
            this.uid = uid
            this.tags = tags.bookmark_tags
            this.nextUrl = tags.next_url
            this.index = index
        }
    }

    private var nextUrl: String? by argumentNullable()
    private var tags: MutableList<BookMarkTagsResponse.BookmarkTagsBean> by argument()
    private var uid: Long by argument()
    private var index: Int by argument()
    val viewModel: BookMarkTagViewModel by viewModels(ownerProducer = { requireParentFragment() })
    var callback: Callback? = null

    fun interface Callback {
        fun onClick(tag: String, public: String)
    }

    override fun onCreateDialogBinding(builder: MaterialAlertDialogBuilder) {
        viewModel.nextUrl.value = nextUrl
        viewModel.tags.value = tags
        val tagsShowAdapter =
            TagsShowAdapter(R.layout.view_tags_show_item, viewModel.tags.value!!)
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
            callback!!.onClick("",
                visRestrictTag(binding.tablayout.selectedTabPosition != 0)
            )
            this.dismiss()
        }
        if (index!=0) binding.tablayout.selectTab(binding.tablayout.getTabAt(index))
        binding.tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {}

            override fun onTabUnselected(p0: TabLayout.Tab?) {}

            override fun onTabSelected(p0: TabLayout.Tab?) {
                if (p0 != null) {
                    val pub = visRestrictTag(p0.position != 0)
                    viewModel.first(uid, pub)
                }
            }
        })
        viewModel.tags.observe(this){
            if (it.isNullOrEmpty()) {
                callback!!.onClick("", viewModel.pub)
                this@TagsShowDialog.dismiss()
            }
            tagsShowAdapter.setNewInstance(it)
        }
        viewModel.tagsAdded.observe(this) {
            if (it != null) {
                tagsShowAdapter.addData(it)
            } else {
                tagsShowAdapter.loadMoreFail()
            }
        }
        viewModel.nextUrl.observe(this) {
            if (it == null) {
                tagsShowAdapter.loadMoreEnd()
            } else {
                tagsShowAdapter.loadMoreComplete()
            }
        }
        tagsShowAdapter.setOnLoadMoreListener {
            if (!viewModel.nextUrl.value.isNullOrBlank()) {
                viewModel.onLoadMore()
            } else {
                tagsShowAdapter.loadMoreEnd()
            }
        }
    }
}
