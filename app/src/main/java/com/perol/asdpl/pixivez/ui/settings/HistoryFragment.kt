package com.perol.asdpl.pixivez.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.databinding.FragmentHistoryBinding
import com.perol.asdpl.pixivez.ui.pic.PictureActivity

class HistoryFragment : Fragment() {
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var binding: FragmentHistoryBinding
    private val historyMViewModel: HistoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyMViewModel.illustBeans.observe(requireActivity()) {
            historyAdapter.setNewInstance(it)
        }
        historyMViewModel.first()

        binding.recyclerview.layoutManager =
            GridLayoutManager(requireContext(), 2 * resources.configuration.orientation)
        historyAdapter = HistoryAdapter(R.layout.view_recommand_itemh)
        binding.recyclerview.adapter = historyAdapter
        binding.recyclerview.smoothScrollToPosition(historyAdapter.data.size)
        binding.fab.setOnClickListener {
            MaterialDialog(requireContext()).show {
                title(R.string.clearhistory)
                positiveButton {
                    historyMViewModel.fabOnClick()
                }
            }
        }
        historyAdapter.setOnItemClickListener { _, _, position ->
            PictureActivity.start(
                requireContext(),
                historyMViewModel.illustBeans.value!![position].illustid
            )
        }
        historyAdapter.setOnItemLongClickListener { _, _, i ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.history_delete_confirm_title)
                .setPositiveButton(R.string.ok) { _, _ ->
                    historyMViewModel.deleteSelect(i)
                    historyAdapter.notifyItemRemoved(i)
                }.show()
            true
        }
    }
}
