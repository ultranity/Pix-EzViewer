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
import androidx.recyclerview.widget.RecyclerView
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

        val layoutManager = GridLayoutManager(requireContext(), 2 * resources.configuration.orientation)
        binding.recyclerview.layoutManager = layoutManager
        historyAdapter = HistoryAdapter()
        binding.recyclerview.adapter = historyAdapter
        binding.recyclerview.smoothScrollToPosition(historyAdapter.data.size)

        // Pull the next page in as the end of the list comes into view, so the full history stays
        // reachable while only the rows actually scrolled to are ever held in memory. The
        // ViewModel ignores requests while one is in flight or once the table is exhausted.
        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                if (layoutManager.findLastVisibleItemPosition() >=
                    layoutManager.itemCount - HistoryViewModel.PREFETCH_DISTANCE
                ) {
                    historyMViewModel.loadMore()
                }
            }
        })

        // Observe on viewLifecycleOwner rather than the Activity. onViewCreated runs again on
        // every view recreation (tab switch, rotation, back-navigation), and an Activity-scoped
        // observer is never removed until the Activity dies - so each pass leaked another
        // observer still holding the previous adapter, its ViewHolders and their ImageViews.
        // The retained bitmaps starve Glide until it stops decoding and only the placeholder
        // renders, i.e. previews "stop showing" after viewing a lot of images. Registering after
        // historyAdapter is assigned also avoids the lateinit crash when LiveData delivers a
        // retained value synchronously (rotating while on this screen).
        historyMViewModel.history.observe(viewLifecycleOwner) {
            historyAdapter.setNewInstance(it)
        }
        historyMViewModel.first()
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
