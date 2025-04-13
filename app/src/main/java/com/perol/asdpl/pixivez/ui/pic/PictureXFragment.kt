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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.graphics.Color
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chrynan.parcelable.core.getParcelable
import com.chrynan.parcelable.core.putParcelable
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseFragment
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.UtilFunc.firstCommonTags
import com.perol.asdpl.pixivez.data.HistoryDatabase
import com.perol.asdpl.pixivez.data.entity.HistoryEntity
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.databinding.FragmentPictureXBinding
import com.perol.asdpl.pixivez.objects.IllustCacheRepo
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.screenWidthDp
import com.perol.asdpl.pixivez.services.Event
import com.perol.asdpl.pixivez.services.FlowEventBus
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.FragmentActivity
import com.perol.asdpl.pixivez.ui.settings.BlockViewModel
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import com.perol.asdpl.pixivez.view.BounceEdgeEffectFactory
import com.perol.asdpl.pixivez.view.loadUserImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates


/**
 * A simple [Fragment] subclass for single pic detail.
 */
class PictureXFragment : BaseFragment() {

    private var illustid by Delegates.notNull<Int>()
    private var illustobj: Illust? = null

    private val pre = PxEZApp.instance.pre
    private val autoLoadRelated by lazy { pre.getBoolean("AutoLoadRelatedIllust", true) }
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
        pictureXAdapter?.imageViewGif?.visibility = View.INVISIBLE
        _binding = null
        pictureXAdapter?.setOnLoadListener { }
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
    @SuppressLint("SetTextI18n")
    private fun checkBlockThenLoad(illust: Illust) {
        illustBlocked = BlockViewModel.getBlockUIDs().contains(illust.user.id).also {
            binding.blocktagTextview.text = illust.user.id.toString()
            binding.blocktagInfo.text = illust.user.name
        } || run {
            val blockTags = BlockViewModel.getBlockTagString()
            firstCommonTags(blockTags, illust.tags)?.also {
                binding.blocktagTextview.text = it.vis()
                binding.blocktagInfo.text = illust.title
            } != null
        }
        if (illustBlocked) {
            binding.blockView.visibility = View.VISIBLE
            binding.recyclerview.visibility = View.GONE
            binding.blockView.bringToFront()
        } else {
            binding.blockView.visibility = View.GONE
            binding.recyclerview.visibility = View.VISIBLE
            val historyDatabase = HistoryDatabase.getInstance(PxEZApp.instance)
            CoroutineScope(Dispatchers.IO).launch {
                val ee = historyDatabase.viewHistoryDao().getEntity(illust.id)
                if (ee != null) {
                    historyDatabase.viewHistoryDao().increment(ee)
                } else
                    historyDatabase.viewHistoryDao().insert(
                        HistoryEntity(illust.id, illust.title, illust.meta[0].square_medium)
                    )
            }
        }
        loadIllust(illust)
    }

    @SuppressLint("SetTextI18n")
    private fun initViewModel() {
        pictureXViewModel.illustDetail.observe(viewLifecycleOwner) {
            binding.progressView.visibility = View.GONE
            if (it != null) {
                page_size = it.meta.size
                // stop loading here if blocked
                checkBlockThenLoad(it)
            } else {
                // step back
                if (parentFragmentManager.backStackEntryCount <= 1) {
                    activity?.finish()
                } else {
                    parentFragmentManager.popBackStack()
                }
            }
        }

        FlowEventBus.observe<Event.BlockTagsChanged>(viewLifecycleOwner) {
            pictureXViewModel.illustDetail.value?.let {
                checkBlockThenLoad(it)
            }
        }

        fun blockFilter(xes: MutableList<Illust>): List<Illust> = xes.filterNot { illust ->
            BlockViewModel.getBlockUIDs().contains(illust.user.id) || run {
                val blockTags = BlockViewModel.getBlockTagString()
                firstCommonTags(blockTags, illust.tags) != null
            }
        }

        pictureXViewModel.related.observe(viewLifecycleOwner) {
            if (it != null) {
                pictureXAdapter!!.relatedPictureAdapter.setNewInstance(blockFilter(it).toMutableList())
                pictureXAdapter!!.relatedPictureAdapter.setOnLoadMoreListener {
                    pictureXViewModel.onLoadMoreRelated()
                }
            } else {
                pictureXAdapter!!.relatedPictureAdapter.loadMoreFail()
            }
        }
        pictureXViewModel.relatedAdded.observe(viewLifecycleOwner) {
            if (it != null) {
                pictureXAdapter!!.relatedPictureAdapter.addData(blockFilter(it))
            } else {
                pictureXAdapter!!.relatedPictureAdapter.loadMoreFail()
            }
        }
        pictureXViewModel.nextRelated.observe(viewLifecycleOwner) {
            if (it != null) {
                pictureXAdapter!!.relatedPictureAdapter.loadMoreComplete()
            } else {
                pictureXAdapter!!.relatedPictureAdapter.loadMoreEnd()
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
        pictureXViewModel.downloadUgoiraZipSuccess.observe(viewLifecycleOwner) {
            pictureXAdapter?.setProgressComplete(it)
        }
    }

    private var illustLoaded = false
    private var illustBlocked = false
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
            setOnLoadListener {
                // activity?.supportStartPostponedEnterTransition()
                //TODO: why need scrollToPosition?
                if (!hasMoved) {
                    binding.recyclerview.scrollToPosition(0)
                    (binding.recyclerview.layoutManager as LinearLayoutManager?)
                        ?.scrollToPositionWithOffset(0, 0)
                }
                if (autoLoadRelated)
                    pictureXViewModel.getRelated()
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
        binding.recyclerview.edgeEffectFactory = BounceEdgeEffectFactory(0.3F)
        pictureXAdapter!!.setInstance(it)
        if (screenWidthDp() > 840) { //double pannel in wide screen
            binding.recyclerview.layoutManager = GridLayoutManager(
                requireContext(), 2,
                //RecyclerView.HORIZONTAL, false
            ).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun isSpanIndexCacheEnabled(): Boolean = true
                    override fun isSpanGroupIndexCacheEnabled(): Boolean = true
                    override fun getSpanSize(position: Int): Int {
                        if (pictureXAdapter!!.getItemViewType(position) ==
                            PictureXAdapter.ITEM_TYPE.ITEM_TYPE_RELATED.ordinal
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

        // 数据绑定
        it.addBinder("F${it.id}|${this.hashCode()}", pictureXViewModel.likeIllust)
        it.user.addBinder("U${it.id}-${this.hashCode()}", pictureXViewModel.followUser)
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
            val illust = pictureXViewModel.illustDetail.value!!
            MaterialDialogs(requireContext()).show {
                setMessage(illust.toDetailString())
                setTitle("Detail")
                setPositiveButton(R.string.setting) { _, _ ->
                    FragmentActivity.start(requireContext(), "Block")
                }
                setNeutralButton("Just Show IT") { _, _ ->
                    binding.blockView.visibility = View.GONE
                    binding.recyclerview.visibility = View.VISIBLE
                }
                cancelButton()
            }
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().let {
            illustid = it.getInt(ARG_ILLUSTID)
            it.getParcelable<Illust>(ARG_ILLUSTOBJ)?.let {
                assert(illustid == it.id) { "id not match when start PictureXFragment" }
                illustobj = IllustCacheRepo.update(it.id, it)
            }
        }
        pictureXViewModel = ViewModelProvider(this)[PictureXViewModel::class.java]
    }

    private var _binding: FragmentPictureXBinding? = null
    private val binding: FragmentPictureXBinding
        get() = requireNotNull(_binding) { "The property of binding has been destroyed." }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        if (_binding == null) {
            _binding = FragmentPictureXBinding.inflate(inflater, container, false)
        }
        page_size = illustobj?.meta?.size ?: 1
        return binding.root
    }

    var page_size = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initView()
    }

    companion object {
        private const val ARG_ILLUSTID = "illustid"
        private const val ARG_ILLUSTOBJ = "illustobj"

        @JvmStatic
        fun newInstance(id: Int?, illust: Illust?) =
            PictureXFragment().apply {
                arguments = Bundle().apply {
                    if (illust != null) {
                        // putParcelable 可以保证在IllustCacheRepo 失效后仍能恢复进程
                        putParcelable(ARG_ILLUSTOBJ, illust)
                        putInt(ARG_ILLUSTID, illust.id)
                    } else {
                        putInt(ARG_ILLUSTID, id!!)
                    }
                }
            }
    }
}
