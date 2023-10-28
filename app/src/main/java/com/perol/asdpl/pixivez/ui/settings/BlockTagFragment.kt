package com.perol.asdpl.pixivez.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.getInputField
import com.perol.asdpl.pixivez.base.setInput
import com.perol.asdpl.pixivez.data.entity.BlockTagEntity
import com.perol.asdpl.pixivez.databinding.FragmentBlockTagBinding
import kotlinx.coroutines.runBlocking

class BlockTagFragment : Fragment() {
    private val viewModel = BlockViewModel

    @SuppressLint("SetTextI18n")
    private fun getChip(blockTagEntity: BlockTagEntity): Chip {
        val chip = Chip(requireContext())
        chip.text = "${blockTagEntity.name} ${blockTagEntity.translateName}"

        chip.setOnLongClickListener {
            runBlocking {
                viewModel.deleteBlockTag(blockTagEntity)
                getTagList()
            }
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
            MaterialDialogs(requireContext()).show {
                setTitle(R.string.block_tag)
                setInput(true) {
                    setHint(R.string.block_tag)
                }
                confirmButton() { dialog, text ->
                    if (getInputField(dialog).text.isNullOrBlank().not()) runBlocking {
                        viewModel.insertBlockTag(
                            BlockTagEntity(
                                text.toString(),
                                text.toString()
                            )
                        )
                        getTagList()
                    }
                }
                cancelButton()
            }
        }
        binding.chipgroup.addView(chip)
    }
}
