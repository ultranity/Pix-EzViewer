/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
 * Copyright (c) 2019 Perol_Notsfsssf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package com.perol.asdpl.pixivez.ui.user

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.postDelayed
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.KotlinUtil.observeOnce
import com.perol.asdpl.pixivez.base.LazyFragment
import com.perol.asdpl.pixivez.core.UserListFragment
import com.perol.asdpl.pixivez.data.model.UserDetail
import com.perol.asdpl.pixivez.databinding.FragmentUserInfoBinding
import com.perol.asdpl.pixivez.objects.EasyFormatter
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.argument
import com.perol.asdpl.pixivez.view.loadBGImage

/**
 * A simple [Fragment] subclass.
 * Use the [UserInfoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UserInfoFragment : LazyFragment() { // Required empty public constructor

    // TODO: Rename and change types of parameters
    private val viewModel: UserMViewModel by viewModels({ requireActivity() })
    private lateinit var userDetail: UserDetail

    private fun getChip(
        word: String,
        hint: String? = null,
        url: String? = null,
        onclickAction: ((Chip) -> Unit)? = null
    ): Chip {
        val chip = Chip(activity)
        chip.text = word
        chip.contentDescription = hint
        if (!url.isNullOrBlank()) {
            chip.setOnClickListener {
                val uri = Uri.parse(url)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
        if (onclickAction != null) {
            chip.setOnClickListener {
                onclickAction(chip)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            chip.tooltipText = hint
        } else if (!hint.isNullOrBlank()) {
            chip.setOnLongClickListener {
                chip.text = chip.contentDescription.also { chip.contentDescription = chip.text }
                it.postDelayed(2000) {
                    chip.contentDescription = chip.text.also { chip.text = chip.contentDescription }
                }
                true
            }
        }
        return chip
    }

    @SuppressLint("SetTextI18n")
    override fun loadData() {
        viewModel.userDetail.value?.let {
            userDetail = it
        }
        if (::userDetail.isInitialized.not()) {
            isLoaded = false
            viewModel.userDetail.observeOnce(viewLifecycleOwner) {
                if (::userDetail.isInitialized.not()) {
                    userDetail = it
                    loadData()
                }
            }
            return
        }
        binding.textViewUsercomment.run {
            autoLinkMask = Linkify.WEB_URLS
            text =
                if (userDetail.user.comment != "") {
                    "${userDetail.user.account}:\r\n${userDetail.user.comment}"
                } else {
                    "~~~"
                }
            binding.cardViewUsercomment.setOnLongClickListener {
                contentDescription = text
                text = EasyFormatter.DEFAULT.format(userDetail)
                it.postDelayed(5000) {
                    text = contentDescription
                }
                true
            }
        }
        loadBGImage(binding.imageviewUserBg, userDetail.profile.background_image_url)
        binding.textviewId.text = "ID:${userid}"

        binding.textviewFans.setOnClickListener {
            UserListFragment.start(requireContext(), userid, null, true)
        }
        binding.textviewFansNum.text = userDetail.profile.total_mypixiv_users.toString()

        binding.textviewFollower.setOnClickListener {
            UserListFragment.start(requireContext(), userid, false)
        }
        binding.textviewFollowingNum.text = userDetail.profile.total_follow_users.toString()
        binding.textviewFollowing.setOnClickListener {
            UserListFragment.start(requireContext(), userid, true)
        }
        //binding.textviewFollowingNum.setOnClickListener { binding.textviewFollowing.callOnClick() }

        if (!userDetail.profile.twitter_account.isNullOrBlank()) {
            binding.chipgroup.addView(
                getChip(
                    "twitter@" + userDetail.profile.twitter_account,
                    "twitter",
                    userDetail.profile.twitter_url
                ).also { it.setTextColor(ContextCompat.getColor(requireContext(), R.color.splash)) }
            )
        }
        if (userDetail.profile_publicity.pawoo) {
            binding.chipgroup.addView(
                getChip(
                    "pawoo",
                    "pawoo",
                    userDetail.profile.pawoo_url
                ).also { it.setTextColor(Color.YELLOW) }
            )
        }
        if (userDetail.profile.total_illusts > 0) {
            binding.chipgroup.addView(
                getChip("ta的插画${userDetail.profile.total_illusts}", "total_illusts") {
                    viewModel.currentTab.value = 0
                }
            )
        }
        if (userDetail.profile.total_manga > 0) {
            binding.chipgroup.addView(
                getChip("ta的漫画" + userDetail.profile.total_manga, "total_manga") {
                    viewModel.currentTab.value = 1
                }
            )
        }
        if (userDetail.profile.total_illust_bookmarks_public > 0) {
            binding.chipgroup.addView(
                getChip(
                    "ta的收藏" + userDetail.profile.total_illust_bookmarks_public,
                    "total_bookmarks"
                ) {
                    viewModel.currentTab.value = 2
                }
            )
        }
        if (userDetail.profile.webpage.isNotBlank()) {
            binding.chipgroup.addView(
                getChip(userDetail.profile.webpage, "webpage", userDetail.profile.webpage)
            )
        }
        val chips = ArrayList<Pair<String?, String>>().apply {
            add(userDetail.profile.gender to "gender")
            add(userDetail.profile.birth to "birth")
            add("${userDetail.profile.region} ${userDetail.profile.country_code}" to "country")
            add(userDetail.profile.job to "job")
            add(userDetail.workspace.tool to "tool")
            add(userDetail.workspace.tablet to "tablet")
            add(userDetail.workspace.printer to "printer")
            add(userDetail.workspace.monitor to "monitor")
            add(userDetail.workspace.chair to "chair")
        }
        chips.filter { it.first.isNullOrBlank().not() }.forEach {
            binding.chipgroup.addView(getChip(it.first!!, it.second))
        }
        if (binding.chipgroup.size <= 2) {
            binding.chipgroup.addView(
                getChip("╮(╯▽╰)╭", "2333")
            )
        }
        binding.chipgroup.addView(
            getChip(getString(R.string.related), "user_related") {
                UserListFragment.start(requireContext(), userid, null, false)
            }.also {
                it.setTextColor(ThemeUtil.getColorHighlight(requireContext()))
            }
        )
    }

    private var userid: Int by argument()

    private lateinit var binding: FragmentUserInfoBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        fun newInstance(userid: Int): Fragment {
            return UserInfoFragment().apply {
                this.userid = userid
            }
        }
    }
}
