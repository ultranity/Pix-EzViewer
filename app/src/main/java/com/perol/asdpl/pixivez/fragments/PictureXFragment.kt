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

package com.perol.asdpl.pixivez.fragments

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.bumptech.glide.Glide
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.BlockActivity
import com.perol.asdpl.pixivez.activity.UserFollowActivity
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.adapters.PictureXAdapter
import com.perol.asdpl.pixivez.databinding.FragmentPictureXBinding
import com.perol.asdpl.pixivez.ui.loadUserImage
import com.perol.asdpl.pixivez.dialog.CommentDialog
import com.perol.asdpl.pixivez.dialog.TagsBookMarkDialog
import com.perol.asdpl.pixivez.objects.AdapterRefreshEvent
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.objects.firstCommon
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.viewmodel.BlockViewModel
import com.perol.asdpl.pixivez.viewmodel.PictureXViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.properties.Delegates

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_ILLUSTID = "param1"
private const val ARG_ILLUSTOBJ = "param2"

/**
 * A simple [Fragment] subclass for pic detail.
 * Use the [PictureXFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class PictureXFragment : BaseFragment() {

    private var illustid by Delegates.notNull<Long>()
    private var illustobj: Illust? = null
    private lateinit var pictureXViewModel: PictureXViewModel
    override fun loadData() {
//        val item = activity?.intent?.extras
//        val illust = item?.getParcelable<Illust>(param1.toString())
        if (illustobj != null) {
            pictureXViewModel.firstGet(illustobj!!)
        }
        else {
            pictureXViewModel.firstGet(illustid)
        }
    }

    override fun onDestroy() {
        pictureXAdapter?.setListener { }
        pictureXAdapter?.setViewCommentListen { }
        pictureXAdapter?.setBookmarkedUserListen { }
        pictureXAdapter?.setUserPicLongClick { }
        super.onDestroy()
    }

    override fun onResume() {
        isLoaded = pictureXViewModel.illustDetail.value != null
        super.onResume()
        pictureXAdapter?.imageViewGif?.startPlay()
    }

    override fun onPause() {
        pictureXAdapter?.imageViewGif?.pausePlay()
        super.onPause()
    }

    private var pictureXAdapter: PictureXAdapter? = null

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AdapterRefreshEvent) {
        lifecycleScope.launch {
            pictureXViewModel.illustDetail.value?.let {
                checkBlock(it)
            }
        }
    }

    /**
     * Block view and return true if need block.
     */
    private fun checkBlock(illust: Illust): Boolean {
        blockTags = BlockViewModel.getBlockTagString()
        //tags.forEach { if (blockTags.contains(it.name)) needBlock = true }
        firstCommon(blockTags.toHashSet(), illust.tags.map {it.name})?.let {
            //needBlock = true
            binding.blocktagTextview.text = it
            binding.blocktagInfo.text = illust.title
            binding.blockView.visibility = View.VISIBLE
            binding.blockView.bringToFront()
            binding.jumpButton.setOnClickListener {
                startActivity(Intent(requireActivity(), BlockActivity::class.java))
            }
            binding.jumpButton.setOnLongClickListener {
                MaterialAlertDialogBuilder(context as Activity)
                    .setMessage(InteractionUtil.toDetailString(illust))
                    .setTitle("Detail")
                    .setPositiveButton(R.string.setting) { _, _ ->
                        startActivity(Intent(requireActivity(), BlockActivity::class.java))
                    }
                    .setNeutralButton("Just Show IT") { _, _ ->
                        binding.blockView.visibility = View.GONE
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .create().show()
                true
            }
            return true
        }
        //var needBlock = false
        binding.blockView.visibility = View.GONE
        return false
    }

    private fun initViewModel() {
        pictureXViewModel.illustDetail.observe(viewLifecycleOwner) { it ->
            binding.progressView.visibility = View.GONE
            if (it != null) {
                checkBlock(it)
                //TODO: whether stop loading here
                //binding.illust = it
                binding.apply {
                    loadUserImage(binding.imageviewUserPicX, it.user.profile_image_urls.medium)
                    textViewTitle.text = it.title
                    textViewUserName.text = it.user.name
                    textViewIllustCreateDate.text = it.create_date
                }

                position = if (it.meta_pages.isNotEmpty()) it.meta_pages.size else 1
                pictureXAdapter =
                    PictureXAdapter(pictureXViewModel, it, requireContext()).also {
                        it.setListener {
                            //                        activity?.supportStartPostponedEnterTransition()
                            if (!hasMoved) {
                                binding.recyclerview.scrollToPosition(0)
                                (binding.recyclerview.layoutManager as LinearLayoutManager?)?.scrollToPositionWithOffset(
                                    0,
                                    0
                                )
                            }
                            pictureXViewModel.getRelated(illustid)
                        }
                        it.setViewCommentListen {
                            val commentDialog =
                                CommentDialog.newInstance(illustid)
                            commentDialog.show(childFragmentManager)
                        }
                        it.setBookmarkedUserListen {
                            UserFollowActivity.start(requireContext(), illustid)
                        }
                        it.setUserPicLongClick {
                            pictureXViewModel.likeUser()
                        }
                    }

                binding.recyclerview.adapter = pictureXAdapter
                if (it.user.is_followed) {
                    binding.imageviewUserPicX.setBorderColor(Color.YELLOW)
                }
                // else
                //    binding.imageviewUserPicX.setBorderColor(ContextCompat.getColor(requireContext(), colorPrimary))
                binding.imageviewUserPicX.setOnLongClickListener {
                    pictureXViewModel.likeUser()
                    true
                }
                binding.imageviewUserPicX.setOnClickListener { _ ->
                    val options = if (PxEZApp.animationEnable) {
                        ActivityOptions.makeSceneTransitionAnimation(
                            context as Activity,
                            Pair(binding.imageviewUserPicX, "userimage")
                        ).toBundle()
                    } else null
                    UserMActivity.start(requireContext(), it.user, options)
                }
                binding.fab.show()
            }
            else {
                if (parentFragmentManager.backStackEntryCount <= 1) {
                    activity?.finish()
                }
                else {
                    parentFragmentManager.popBackStack()
                }
            }
        }
        pictureXViewModel.relatedPics.observe(viewLifecycleOwner) {
            if (it != null) {
                pictureXAdapter?.setRelatedPics(it)
            }
        }
        pictureXViewModel.likeIllust.observe(viewLifecycleOwner) {
            if (it) {
                Glide.with(this).load(R.drawable.heart_red).into(binding.fab)
            }
            else {
                Glide.with(this).load(R.drawable.ic_action_heart).into(binding.fab)
            }
        }
        pictureXViewModel.followUser.observe(viewLifecycleOwner) {
            binding.imageviewUserPicX.setBorderColor(
                if (it) {
                    Color.YELLOW
                }
                else {
                    ThemeUtil.getColorPrimary(requireContext())
                }
            )
            pictureXAdapter?.setUserPicColor(it)
        }
        pictureXViewModel.progress.observe(viewLifecycleOwner) {
            pictureXAdapter?.setProgress(it)
        }
        pictureXViewModel.downloadGifSuccess.observe(viewLifecycleOwner) {
            pictureXAdapter?.setProgressComplete(it)
        }
        //TODO: is it necessary?
        //pictureXAdapter?.notifyDataSetChanged()
    }

    var hasMoved = false
    private fun initView() {
        binding.fab.setOnClickListener {
            pictureXViewModel.fabClick()
        }
        binding.fab.setOnLongClickListener {
            if (pictureXViewModel.illustDetail.value!!.is_bookmarked) {
                return@setOnLongClickListener true
            }
            Toasty.info(
                requireActivity(),
                resources.getString(R.string.fetchtags),
                Toast.LENGTH_SHORT
            ).show()
            val tagsBookMarkDialog = TagsBookMarkDialog()
            tagsBookMarkDialog.show(childFragmentManager, TagsBookMarkDialog::class.java.name)
            true
        }
        binding.imageButton.setOnClickListener {
            binding.recyclerview.scrollToPosition(position)
            binding.constraintLayoutFold.visibility = View.INVISIBLE
        }
        binding.recyclerview.layoutManager =  LinearLayoutManager(requireActivity())
        binding.recyclerview.setHasFixedSize(true)
        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                hasMoved = true
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                (binding.recyclerview.layoutManager as LinearLayoutManager).run {
                    /*Log.d("test", "onScrolled: "+
                            findFirstCompletelyVisibleItemPosition().toString()+" "+
                            findLastCompletelyVisibleItemPosition().toString()+" "+
                            findFirstVisibleItemPosition().toString() +" "+
                            findLastVisibleItemPosition().toString()+" "+position)*/
                    if (findFirstVisibleItemPosition() <= position && findLastVisibleItemPosition() >= position - 1) {
                        binding.constraintLayoutFold.visibility = View.INVISIBLE
                    }
                    else if (findFirstVisibleItemPosition() > position || findLastVisibleItemPosition() < position) {
                        binding.constraintLayoutFold.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            illustid = it.getLong(ARG_ILLUSTID)
            illustobj = it.getParcelable(ARG_ILLUSTOBJ)
        }
        pictureXViewModel = ViewModelProvider(this)[PictureXViewModel::class.java]
    }

    lateinit var binding: FragmentPictureXBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        if (!this::binding.isInitialized) {
            binding = FragmentPictureXBinding.inflate(inflater, container, false)
        }
        position = illustobj?.meta_pages?.size ?: 1
        return binding.root
    }

    var position = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initView()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param id Parameter 1.
         * @param illust Parameter 2.
         * @return A new instance of fragment PictureXFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(id: Long?, illust: Illust?) =
            PictureXFragment().apply {
                arguments = Bundle().apply {
                    if (illust != null) {
                        putParcelable(ARG_ILLUSTOBJ, illust)
                        putLong(ARG_ILLUSTID, illust.id)
                    }
                    else {
                        putLong(ARG_ILLUSTID, id!!)
                    }
/*                    putParcelable("illust", illust)*/
                }
            }
    }
}
