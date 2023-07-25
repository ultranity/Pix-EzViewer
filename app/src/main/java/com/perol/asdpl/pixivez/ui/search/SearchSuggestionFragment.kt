package com.perol.asdpl.pixivez.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.model.Tag
import com.perol.asdpl.pixivez.databinding.FragmentSearchRBinding

/**
 * A placeholder fragment containing a simple view.
 */
class SearchSuggestionFragment : Fragment() {

    private lateinit var binding: FragmentSearchRBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchRBinding.inflate(inflater, container, false)
        return binding.root
    }

    private class TagsTextAdapter(layoutResId: Int) :
        BaseQuickAdapter<Tag, BaseViewHolder>(layoutResId) {
        override fun convert(holder: BaseViewHolder, item: Tag) {
            holder.setText(R.id.name, item.name)
                .setText(R.id.translated_name, item.translated_name)
        }
    }

    private lateinit var tagsTextViewModel: TagsTextViewModel
    private lateinit var tagsTextAdapter: TagsTextAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tagsTextAdapter = TagsTextAdapter(R.layout.tagstext_item)
        binding.recyclerview.layoutManager = LinearLayoutManager(activity)
        binding.recyclerview.adapter = tagsTextAdapter
        tagsTextAdapter.setOnItemClickListener { adapter, view, position ->
            val tag = tags[position]
            tagsTextViewModel.addHistory(tag)
            val bundle = Bundle()
            bundle.putString("keyword", tag.name)
            val intent = Intent(requireActivity(), SearchResultActivity::class.java)
            intent.putExtras(bundle)
            startActivityForResult(intent, 775)
        }
        tagsTextViewModel = ViewModelProvider(requireActivity())[TagsTextViewModel::class.java]
        tagsTextViewModel.autoCompleteTags.observe(viewLifecycleOwner) {
            tagsTextAdapter.setList(it)
            tags.clear()
            tags.addAll(it)
        }
    }

    val tags = ArrayList<Tag>()
}
