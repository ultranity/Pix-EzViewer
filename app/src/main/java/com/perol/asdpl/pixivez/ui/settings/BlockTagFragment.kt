package com.perol.asdpl.pixivez.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.getInputField
import com.perol.asdpl.pixivez.base.setInput
import com.perol.asdpl.pixivez.data.entity.BlockTagEntity
import com.perol.asdpl.pixivez.databinding.FragmentBlockTagBinding
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import kotlinx.coroutines.runBlocking

class BlockTagFragment : Fragment() {
    private val viewModel = BlockViewModel

    @SuppressLint("SetTextI18n")
    private fun getChip(blockTagEntity: BlockTagEntity): Chip {
        val chip = Chip(requireContext())
        chip.text = if (blockTagEntity.name == blockTagEntity.translateName ||
            blockTagEntity.translateName.isBlank()
        ) blockTagEntity.name
        else "${blockTagEntity.name}|${blockTagEntity.translateName}"

        chip.setOnLongClickListener {
            MaterialDialogs(requireContext()).show {
                setTitle("Delete Tag")
                setMessage("Are you sure to delete ${blockTagEntity.name}?")
                confirmButton { dialog, text ->
                    runBlocking {
                        viewModel.deleteBlockTag(blockTagEntity)
                    }
                    getTagList()
                }
                cancelButton()
            }
            true
        }
        return chip
    }

    @SuppressLint("SetTextI18n")
    private fun getChip(uid: Int): Chip {
        val chip = Chip(requireContext())
        chip.text = uid.toString()

        chip.setOnLongClickListener {
            MaterialDialogs(requireContext()).show {
                setTitle("Delete UID")
                setMessage("Are you sure to delete $uid?")
                confirmButton { dialog, text ->
                    runBlocking {
                        viewModel.deleteBlockUser(uid)
                    }
                    getUserList()
                }
                cancelButton()
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
        getUserList()
    }

    private fun createChipForAdd(
        chipGroup: ChipGroup,
        title: String,
        config: TextInputLayout.() -> Unit,
        action: suspend (String) -> Unit
    ) {
        val chip = Chip(requireContext())
        chip.text = "+"
        chip.setOnClickListener {
            MaterialDialogs(requireContext()).show {
                setTitle(title)
                setInput(true, config)
                confirmButton { dialog, text ->
                    val text = getInputField(dialog).text
                    if (text.isNullOrBlank().not())
                        runBlocking { action(text.toString()) }
                }
                cancelButton()
            }
        }
        chipGroup.addView(chip)
    }

    private lateinit var blockTag: MutableList<BlockTagEntity>
    private var foldedChipIndex: Int = 3

    @SuppressLint("SetTextI18n")
    private fun getExpandChip(chipGroup: ChipGroup): Chip {
        val chip = Chip(requireContext())
        chip.text = "more"
        chip.setChipIconResource(R.drawable.ic_menu_more)
        chip.setChipIconTintResource(ThemeUtil.getTextColorPrimaryResID(requireContext()))
        chip.iconEndPadding = 0F
        chip.setOnClickListener {
            chipGroup.removeViewAt(foldedChipIndex)
            blockTag.drop(foldedChipIndex - 1).forEachIndexed { index, s ->
                val chip = getChip(s)
                chipGroup.addView(chip, index + foldedChipIndex)
            }
            chipGroup.addView(getFoldChip(chipGroup))
        }
        return chip
    }

    private fun getFoldChip(chipGroup: ChipGroup): Chip {
        val chip = Chip(requireContext())
        chip.setChipIconResource(R.drawable.ic_action_fold)
        chip.setChipIconTintResource(ThemeUtil.getTextColorPrimaryResID(requireContext()))
        chip.setOnClickListener {
            (foldedChipIndex.until(chipGroup.size)).forEach { _ ->
                chipGroup.removeViewAt(foldedChipIndex)
            }
            chipGroup.addView(getExpandChip(chipGroup))
        }
        return chip
    }

    private fun getTagList() {
        blockTag = runBlocking {
            viewModel.fetchAllTags()
        }
        binding.blockTags.removeAllViews()
        createChipForAdd(binding.blockTags, getString(R.string.block_tag),
            config = { hint = getString(R.string.block_tag) },
            action = {
                viewModel.insertBlockTag(BlockTagEntity(it, it))
                getTagList()
            })
        blockTag.take(foldedChipIndex - 1).forEach { binding.blockTags.addView(getChip(it)) }
        if (blockTag.size >= foldedChipIndex)
            binding.blockTags.addView(getExpandChip(binding.blockTags))
    }

    private fun getUserList() {
        val it = runBlocking {
            viewModel.fetchAllUIDs()
        }
        createChipForAdd(binding.blockUsers,
            getString(R.string.block_user),
            config = {
                hint = "UID"
                editText!!.inputType = InputType.TYPE_CLASS_NUMBER
            }
        ) {
            viewModel.insertBlockUser(it.toInt())
            getUserList()
        }
        binding.blockUsers.removeAllViews()
        it.forEach { v ->
            val chip = getChip(v)
            chip.setOnClickListener {
                UserMActivity.start(requireContext(), v)
            }
            binding.blockUsers.addView(chip)
        }
    }
}
