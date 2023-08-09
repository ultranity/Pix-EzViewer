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

package com.perol.asdpl.pixivez.ui.pic

import android.app.Activity
import android.app.ActivityOptions
import android.graphics.Color
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chrynan.parcelable.core.getParcelable
import com.chrynan.parcelable.core.putParcelable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseFragment
import com.perol.asdpl.pixivez.base.UtilFunc.firstCommonTags
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.databinding.FragmentPictureXBinding
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.objects.screenWidthDp
import com.perol.asdpl.pixivez.services.Event
import com.perol.asdpl.pixivez.services.FlowEventBus
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.FragmentActivity
import com.perol.asdpl.pixivez.ui.settings.BlockViewModel
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import com.perol.asdpl.pixivez.view.BounceEdgeEffectFactory
import com.perol.asdpl.pixivez.view.loadUserImage
import kotlin.properties.Delegates


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
        } else {
            pictureXViewModel.firstGet(illustid)
        }
    }

    override fun onDestroy() {
        pictureXAdapter?.setListener { }
        pictureXAdapter?.setViewCommentListen { }
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

    /**
     * Block view and return true if need block.
     */
    private fun checkBlock(illust: Illust): Boolean {
        blockTags = BlockViewModel.getBlockTagString()
        //tags.forEach { if (blockTags.contains(it.name)) needBlock = true }
        firstCommonTags(blockTags.toHashSet(), illust.tags)?.let {
            //needBlock = true
            binding.blocktagTextview.text = it.vis()
            binding.blocktagInfo.text = illust.title
            binding.blockView.visibility = View.VISIBLE
            binding.blockView.bringToFront()
            return true
        }
        //var needBlock = false
        binding.blockView.visibility = View.GONE
        binding.recyclerview.visibility = View.VISIBLE
        return false
    }

    private fun initViewModel() {
        pictureXViewModel.illustDetail.observe(viewLifecycleOwner) {
            binding.progressView.visibility = View.GONE
            if (it != null) {
                page_size = if (it.meta_pages.isNotEmpty()) it.meta_pages.size else 1
                // stop loading here if blocked
                checkBlock(it)
                loadIllust(it)
            } else {
                if (parentFragmentManager.backStackEntryCount <= 1) {
                    activity?.finish()
                } else {
                    parentFragmentManager.popBackStack()
                }
            }
        }
        pictureXViewModel.related.observe(viewLifecycleOwner) {
            val relatedPictureAdapter = pictureXAdapter!!.relatedPictureAdapter
            if (it != null) {
                relatedPictureAdapter.setNewInstance(it)
                relatedPictureAdapter.setOnLoadMoreListener {
                    pictureXViewModel.onLoadMoreRelated()
                }
            } else {
                relatedPictureAdapter.loadMoreFail()
            }
        }
        pictureXViewModel.relatedAdded.observe(viewLifecycleOwner) {
            val relatedPictureAdapter = pictureXAdapter!!.relatedPictureAdapter
            if (it != null) {
                relatedPictureAdapter.addData(it)
            } else {
                relatedPictureAdapter.loadMoreFail()
            }
        }
        pictureXViewModel.nextRelated.observe(viewLifecycleOwner) {
            val relatedPictureAdapter = pictureXAdapter!!.relatedPictureAdapter
            if (it != null) {
                relatedPictureAdapter.loadMoreComplete()
            } else {
                relatedPictureAdapter.loadMoreEnd()
            }
        }
        pictureXViewModel.likeIllust.observe(viewLifecycleOwner) {
            if (it) {
                Glide.with(this).load(R.drawable.ic_love).into(binding.fab)
            } else {
                //fixed: WTF? Glide加载的 ic_action_heart 会变成别的图标，似乎与 res id值=0x7f08009a有关
                Glide.with(this).load(R.drawable.ic_heart).into(binding.fab)
                //binding.fab.setImageResource(R.drawable.ic_action_heart)
            }
        }
        pictureXViewModel.followUser.observe(viewLifecycleOwner) {
            binding.imageviewUserPicX.setBorderColor(
                if (it) {
                    ThemeUtil.getColorHighlight(requireContext())
                } else {
                    ThemeUtil.getColorPrimary(requireContext())
                }
            )
            pictureXAdapter?.setUserDataIsFollowed(it)
        }
        pictureXViewModel.progress.observe(viewLifecycleOwner) {
            pictureXAdapter?.setProgress(it)
        }
        pictureXViewModel.downloadGifSuccess.observe(viewLifecycleOwner) {
            pictureXAdapter?.setProgressComplete(it)
        }

        FlowEventBus.observe<Event.BlockTagsChanged>(viewLifecycleOwner) {
            pictureXViewModel.illustDetail.value?.let {
                checkBlock(it)
            }
        }
    }

    private var illustLoaded = false
    private fun loadIllust(it: Illust) {
        if (illustLoaded) return
        illustLoaded = true
        binding.apply {
            loadUserImage(binding.imageviewUserPicX, it.user.profile_image_urls.medium)
            textViewTitle.text = it.title
            textViewUserName.text = it.user.name
            textViewIllustCreateDate.text = it.create_date
        }
        pictureXAdapter = PictureXAdapter(pictureXViewModel, requireContext()).apply {
            setListener {
                // activity?.supportStartPostponedEnterTransition()
                if (!hasMoved) {
                    binding.recyclerview.scrollToPosition(0)
                    (binding.recyclerview.layoutManager as LinearLayoutManager?)
                        ?.scrollToPositionWithOffset(0, 0)
                }
                pictureXViewModel.getRelated(illustid)
            }
            setViewCommentListen {
                CommentDialog.newInstance(illustid)
                    .show(childFragmentManager)
            }
            setUserPicLongClick {
                pictureXViewModel.likeUser()
            }
        }

        binding.recyclerview.adapter = pictureXAdapter
        binding.recyclerview.edgeEffectFactory = BounceEdgeEffectFactory(0.5F)
        pictureXAdapter!!.setInstance(it)
        if (screenWidthDp() > 840) { //double pannel in wide screen
            binding.recyclerview.layoutManager =
                GridLayoutManager(
                    requireContext(), 2,
                    //RecyclerView.HORIZONTAL, false
                ).apply {
                    spanSizeLookup = object : SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            if (pictureXAdapter!!.getItemViewType(position) ==
                                PictureXAdapter.ITEM_TYPE.ITEM_TYPE_RELATIVE.ordinal
                            )
                                return 2
                            return 1
                        }
                    }
                }
            //TODO: padding when only single image
        }
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
                    Pair(binding.imageviewUserPicX, "shared_element_container")//"userimage")
                ).toBundle()
            } else null
            UserMActivity.start(requireContext(), it.user, options)
        }
        binding.fab.show()
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
            tagsBookMarkDialog.show(childFragmentManager, TagsBookMarkDialog.TAG)
            true
        }
        binding.imageButton.setOnClickListener {
            binding.recyclerview.scrollToPosition(page_size)
            binding.constraintLayoutFold.visibility = View.INVISIBLE
        }
        binding.recyclerview.layoutManager = LinearLayoutManager(requireActivity())
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
                    if (findFirstVisibleItemPosition() <= page_size && findLastVisibleItemPosition() >= page_size - 1) {
                        binding.constraintLayoutFold.visibility = View.INVISIBLE
                    } else if (findFirstVisibleItemPosition() > page_size || findLastVisibleItemPosition() < page_size) {
                        binding.constraintLayoutFold.visibility = View.VISIBLE
                    }
                }
            }
        })

        binding.jumpButton.setOnClickListener {
            FragmentActivity.start(requireContext(), "Block")
        }
        binding.jumpButton.setOnLongClickListener {
            MaterialAlertDialogBuilder(context as Activity)
                .setMessage(InteractionUtil.toDetailString(pictureXViewModel.illustDetail.value!!))
                .setTitle("Detail")
                .setPositiveButton(R.string.setting) { _, _ ->
                    FragmentActivity.start(requireContext(), "Block")
                }
                .setNeutralButton("Just Show IT") { _, _ ->
                    binding.blockView.visibility = View.GONE
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().let {
            illustid = it.getLong(ARG_ILLUSTID)
            illustobj = it.getParcelable<Illust>(ARG_ILLUSTOBJ)
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
        page_size = illustobj?.meta_pages?.size ?: 1
        return binding.root
    }

    var page_size = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initView()
    }

    companion object {
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private const val ARG_ILLUSTID = "illustid"
        private const val ARG_ILLUSTOBJ = "illustobj"

        @JvmStatic
        fun newInstance(id: Long?, illust: Illust?) =
            PictureXFragment().apply {
                arguments = Bundle().apply {
                    if (illust != null) {
                        putParcelable(ARG_ILLUSTOBJ, illust)
                        putLong(ARG_ILLUSTID, illust.id)
                    } else {
                        putLong(ARG_ILLUSTID, id!!)
                    }
                }
            }
    }
}
