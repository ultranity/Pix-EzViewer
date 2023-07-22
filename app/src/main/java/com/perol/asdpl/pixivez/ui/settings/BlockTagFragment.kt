package com.perol.asdpl.pixivez.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.google.android.material.chip.Chip
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.databinding.FragmentBlockTagBinding
import com.perol.asdpl.pixivez.objects.AdapterRefreshEvent
import com.perol.asdpl.pixivez.data.entity.BlockTagEntity
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus

class BlockTagFragment : Fragment() {
    private val viewModel = BlockViewModel

    @SuppressLint("SetTextI18n")
    private fun getChip(blockTagEntity: BlockTagEntity): Chip {
        val chip = Chip(requireContext())
        chip.text = "${blockTagEntity.name} ${blockTagEntity.translateName}"

        chip.setOnLongClickListener {
            runBlocking {
                viewModel.deleteSingleTag(blockTagEntity)
                getTagList()
            }
            EventBus.getDefault().post(AdapterRefreshEvent())
            true
        }
        return chip
    }

    private lateinit var binding: FragmentBlockTagBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentBlockTagBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getTagList()
    }

    private fun getTagList() {
        val it = runBlocking {
            viewModel.fetchAllTags()
        }
        binding.chipgroup.removeAllViews()
        var chip = Chip(requireContext())
        chip.text = requireContext().getText(R.string.hold_to_delete)
        binding.chipgroup.addView(chip)
        it.forEach { v ->
            binding.chipgroup.addView(getChip(v))
        }
        chip = Chip(requireContext())
        chip.text = "+"
        chip.setOnClickListener {
            MaterialDialog(requireContext()).show {
                title(R.string.block_tag)
                val inputitem = input { dialog, text ->
                    if (text.isBlank()) return@input
                    runBlocking {
                        viewModel.insertBlockTag(
                            BlockTagEntity(
                                text.toString(),
                                text.toString()
                            )
                        )
                        getTagList()
                    }

                    EventBus.getDefault().post(AdapterRefreshEvent())
                }
                positiveButton()
                negativeButton()
            }
        }
        binding.chipgroup.addView(chip)
    }
}
