package com.perol.asdpl.pixivez.ui.settings

import android.app.ActivityOptions
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.databinding.FragmentHistoryBinding
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import com.perol.asdpl.pixivez.ui.user.UserMActivity

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

        historyMViewModel.history.observe(requireActivity()) {
            historyAdapter.setNewInstance(it)
        }
        historyMViewModel.first()

        binding.recyclerview.layoutManager =
            GridLayoutManager(requireContext(), 2 * resources.configuration.orientation)
        historyAdapter = HistoryAdapter()
        binding.recyclerview.adapter = historyAdapter
        binding.recyclerview.smoothScrollToPosition(historyAdapter.data.size)
        binding.fab.setOnClickListener {
            MaterialDialogs(requireContext()).show {
                setTitle(R.string.clearhistory)
                confirmButton { _, _ ->
                    historyMViewModel.clearHistory()
                }
            }
        }
        historyAdapter.setOnItemClickListener { _, view, position ->
            val item = historyMViewModel.history.value!![position]
            val options = if (PxEZApp.animationEnable) {
                ActivityOptions.makeSceneTransitionAnimation(
                    requireActivity(),
                    Pair(view, "shared_element_container")
                ).toBundle()
            } else null
            if (item.isUser) UserMActivity.start(requireContext(), item.id, options)
            else PictureActivity.start(requireContext(), item.id, options = options)
        }
        historyAdapter.setOnItemLongClickListener { _, _, i ->
            MaterialDialogs(requireContext()).show {
                setTitle(R.string.confirm_title)
                confirmButton { _, _ ->
                    historyMViewModel.deleteSelect(i) {
                        historyAdapter.notifyItemRemoved(i)
                    }
                }
            }
            true
        }
    }
}
