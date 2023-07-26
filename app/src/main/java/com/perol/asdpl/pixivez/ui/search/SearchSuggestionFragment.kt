package com.perol.asdpl.pixivez.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.model.Tag
import com.perol.asdpl.pixivez.databinding.FragmentSearchSuggestionsBinding

/**
 * A placeholder fragment containing a simple view.
 */
class SearchSuggestionFragment : Fragment() {

    private lateinit var binding: FragmentSearchSuggestionsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchSuggestionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private class TagsTextAdapter(layoutResId: Int) :
        BaseQuickAdapter<Tag, BaseViewHolder>(layoutResId) {
        override fun convert(holder: BaseViewHolder, item: Tag) {
            holder.setText(R.id.name, item.name)
                .setText(R.id.translated_name, item.translated_name)
        }
    }

    private val searchSuggestionViewModel: SearchSuggestionViewModel by viewModels({ requireActivity() })
    private lateinit var tagsTextAdapter: TagsTextAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tagsTextAdapter = TagsTextAdapter(R.layout.tagstext_item)
        binding.recyclerview.layoutManager = LinearLayoutManager(activity)
        binding.recyclerview.adapter = tagsTextAdapter
        tagsTextAdapter.setOnItemClickListener { adapter, view, position ->
            val tag = searchSuggestionViewModel.autoCompleteTags.value!![position]
            searchSuggestionViewModel.addHistory(tag)
            val bundle = Bundle()
            bundle.putString("keyword", tag.name)
            val intent = Intent(requireActivity(), SearchResultActivity::class.java)
            intent.putExtras(bundle)
            startActivityForResult(intent, 775)
        }
        binding.advices.visibility = View.GONE
        searchSuggestionViewModel.autoCompleteTags.observe(viewLifecycleOwner) {
            binding.advices.visibility = if (it.isNotEmpty()) View.VISIBLE else View.GONE
            tagsTextAdapter.setList(it)
        }
    }
}
