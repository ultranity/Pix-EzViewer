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
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface.BUTTON_NEUTRAL
import android.content.Intent
import android.content.pm.ResolveInfo
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.KotlinUtil.launchCatching
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.data.entity.BlockTagEntity
import com.perol.asdpl.pixivez.data.model.AIType
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.data.model.Tag
import com.perol.asdpl.pixivez.databinding.ViewPicturexDetailBinding
import com.perol.asdpl.pixivez.databinding.ViewPicturexSurfaceGifBinding
import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.InteractionUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.ToastQ
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.objects.screenHeightPx
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.ui.search.SearchActivity
import com.perol.asdpl.pixivez.ui.settings.BlockViewModel
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import com.perol.asdpl.pixivez.ui.user.UsersFragment
import com.perol.asdpl.pixivez.view.AnimationView
import com.perol.asdpl.pixivez.view.loadUserImage
import com.zhy.view.flowlayout.FlowLayout
import com.zhy.view.flowlayout.TagAdapter
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.properties.Delegates

// support double panel in wide screen device
// if? save zip ugoira by default
class PictureXAdapter(
    private val pictureXViewModel: PictureXViewModel,
    private val mContext: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var data: Illust
    fun setInstance(illust: Illust) {
        data = illust
        initConfig()
    }

    private var quality by Delegates.notNull<Int>()
    private lateinit var imageUrls: List<String>
    private lateinit var imageThumbnail: String
    val pre = PxEZApp.instance.pre
    private lateinit var mListen: () -> Unit
    private lateinit var mViewCommentListen: () -> Unit
    private lateinit var mUserPicLongClick: () -> Unit
    fun setUserPicLongClick(listener: () -> Unit) {
        this.mUserPicLongClick = listener
    }

    fun setListener(listener: () -> Unit) {
        this.mListen = listener
    }

    fun setViewCommentListen(listener: () -> Unit) {
        this.mViewCommentListen = listener
    }


    private fun initConfig() {
        quality = pre.getString("quality", "0")?.toInt() ?: 0
        imageUrls = when (quality) {
            0 -> data.meta.map { it.medium }
            2 -> data.meta.map { it.original }
            else -> data.meta.map { it.large }
        }
        val needSmall = if (quality == 1) {
            (data.height / data.width > 3) || (data.width / data.height >= 3)
        } else {
            data.height > 1800
        }
        imageThumbnail = if (needSmall) {
            data.meta[0].square_medium
        } else {
            data.meta[0].medium
        }
    }

    class PictureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class SurfaceGifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class DetailViewHolder(
        var binding: ViewPicturexDetailBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun updateWithPage(
            mContext: Context,
            illust: Illust,
            mViewCommentListen: () -> Unit,
            mUserPicLongClick: () -> Unit
        ) {
            //binding.illust = illust
            binding.apply {
                loadUserImage(binding.imageviewUserPicX, illust.user.profile_image_urls.medium)

                textViewTitle.text = illust.title
                textViewUserName.text = illust.user.name
                textViewIllustCreateDate.text = illust.create_date

                textviewIllustId.text = illust.id.toString()
                pixelWxH.text = "${illust.width}X${illust.height}"
                textViewTotalView.text = illust.total_view.toString()
                bookmarkedUserNum.text = illust.total_bookmarks.toString()
                san.text = illust.sanity_level.toString()
                if (illust.sanity_level > 5) {
                    san.setTextColor(Color.RED)
                }
                if (illust.illust_ai_type > 0) {
                    AIText.visibility = View.VISIBLE
                    AILevel.text = AIType.entries[illust.illust_ai_type].toString()
                    if (illust.illust_ai_type == 2)
                        AILevel.setTextColor(Color.RED)
                }
            }
            // captionTextView.autoLinkMask = Linkify.WEB_URLS
            val colorPrimary = ThemeUtil.getColorPrimary(mContext)
            val colorPrimaryDark = ThemeUtil.getColorPrimaryDark(mContext)
            val badgeTextColor = ThemeUtil.getColorHighlight(mContext)
            if (illust.user.is_followed) {
                binding.imageviewUserPicX.setBorderColor(badgeTextColor)
            } else {
                binding.imageviewUserPicX.setBorderColor(colorPrimary)
            }
            binding.imageviewUserPicX.setOnLongClickListener {
                mUserPicLongClick.invoke()
                true
            }
            binding.imageviewUserPicX.setOnClickListener {
                val options = if (PxEZApp.animationEnable) {
                    ActivityOptions.makeSceneTransitionAnimation(
                        mContext as Activity,
                        Pair(binding.imageviewUserPicX, "shared_element_container")//"userimage")
                    ).toBundle()
                } else null
                UserMActivity.start(mContext, illust.user, options)
            }
            binding.textviewCaption.text = Html.fromHtml(illust.caption.ifBlank { "~" })
            Linkify.addLinks(binding.textviewCaption, Linkify.WEB_URLS)
            CrashHandler.instance.d("url", binding.textviewCaption.urls.toString())
            binding.textviewCaption.movementMethod = LinkMovementMethod.getInstance()

            binding.btnViewRelated.setOnClickListener {
                pictureXViewModel.getRelated()
            }
            //TODO: get real comment count
            // binding.textviewViewComment.text = "${binding.textviewViewComment.text}(${illust.total_comments})"
            binding.btnViewComment.setOnClickListener {
                mViewCommentListen.invoke()
            }
            binding.textviewBookmarked.setOnClickListener {
                UsersFragment.start(mContext, illust.id)
            }
            binding.bookmarkedUserNum.setOnClickListener {
                binding.textviewBookmarked.callOnClick()
            }
            // google translate app btn click listener
            val intent = Intent().setType("text/plain")
            var componentPackageName = ""
            var componentName = ""
            var isGoogleTranslateEnabled = false
            // check google translate
            for (resolveInfo: ResolveInfo in
            mContext.packageManager.queryIntentActivities(intent, 0)) {
                try {
                    // emui null point exception
                    if (resolveInfo.activityInfo.packageName.contains("com.google.android.apps.translate")) {
                        isGoogleTranslateEnabled = true
                        componentPackageName = resolveInfo.activityInfo.packageName
                        componentName = resolveInfo.activityInfo.name
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (!isGoogleTranslateEnabled || illust.caption.isBlank()) {
                binding.btnTranslate.visibility = View.GONE
            } else {
                binding.btnTranslate.setOnClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        intent.action = Intent.ACTION_PROCESS_TEXT
                        intent.putExtra(
                            Intent.EXTRA_PROCESS_TEXT,
                            binding.textviewCaption.text.toString()
                        )
                    } else {
                        intent.action = Intent.ACTION_SEND
                        intent.putExtra(Intent.EXTRA_TEXT, binding.textviewCaption.text.toString())
                    }
                    intent.component = ComponentName(
                        componentPackageName,
                        componentName
                    )
                    mContext.startActivity(intent)
                }
            }

            binding.tagFlowlayout.adapter = object : TagAdapter<Tag>(illust.tags) {
                @SuppressLint("SetTextI18n")
                override fun getView(parent: FlowLayout, position: Int, t: Tag): View {
                    val tv = LayoutInflater.from(mContext)
                        .inflate(R.layout.picture_tag, parent, false)
                    val name = tv.findViewById<TextView>(R.id.name)
                    val translateName = tv.findViewById<TextView>(R.id.translated_name)
                    name.text = "#${t.name} "
                    if (!t.translated_name.isNullOrBlank()) {
                        translateName.visibility = View.VISIBLE
                        translateName.text = t.translated_name
                    }
                    if (t.name == "R-18" || t.name == "R-18G") {
                        name.setTextColor(Color.RED)
                    }
                    tv.setOnClickListener {
                        SearchActivity.start(mContext, t.name) //illust.tags[position]
                    }
                    tv.setOnLongClickListener {
                        showBlockTagDialog(mContext, t)
                        true
                    }
                    return tv
                }
            }
            binding.imagebuttonShare.setOnClickListener {
                val textIntent = Intent(Intent.ACTION_SEND)
                textIntent.type = "text/plain"
                textIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "https://www.pixiv.net/artworks/${illust.id}"
                )
                mContext.startActivity(
                    Intent.createChooser(
                        textIntent,
                        mContext.getString(R.string.share)
                    )
                )
            }
            if (FileUtil.isDownloaded(illust)) {
                binding.imagebuttonDownload.drawable.setTint(badgeTextColor)
            }
            if (illust.type == "ugoira") {
                // gif
                binding.imagebuttonDownload.setOnClickListener {
                    //TODO: separate logic
                    gifPlay.callOnClick()
                    showGIFDialog(illust)
                }
            } else {
                binding.imagebuttonDownload.setOnClickListener {
                    binding.imagebuttonDownload.drawable.setTint(colorPrimaryDark)
                    Works.imageDownloadAll(illust)
                }
            }
            binding.imagebuttonDownload.setOnLongClickListener {
                // show detail of illust
                val detailstring = InteractionUtil.toDetailString(illust, false)
                MaterialDialogs(mContext).show {
                    setMessage(detailstring)
                    setTitle("Detail")
                    confirmButton()
                }
                true
            }
        }

        private fun showBlockTagDialog(mContext: Context, t: Tag) {
            MaterialDialogs(mContext).show {
                setTitle(R.string.add_to_block_tag_list)
                confirmButton { _, _ ->
                    runBlocking {
                        BlockViewModel.insertBlockTag(
                            BlockTagEntity(
                                name = t.name,
                                translateName = t.translated_name ?: ""
                            )
                        )
                    }
                }
                cancelButton()
            }
        }
    }

    class RelatedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recyclerview = itemView.findViewById<RecyclerView>(R.id.recyclerview_related)!!
        fun updateWithPage(s: SquareMediumAdapter, mContext: Context) {
            recyclerview.layoutManager =
                GridLayoutManager(mContext, 1 + 2 * mContext.resources.configuration.orientation)
            recyclerview.adapter = s
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            ITEM_TYPE.ITEM_TYPE_PICTURE.ordinal -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_picturex_item, parent, false)
                return PictureViewHolder(view)
            }

            ITEM_TYPE.ITEM_TYPE_GIF.ordinal -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_picturex_surface_gif, parent, false)
                return SurfaceGifViewHolder(view)
            }

            ITEM_TYPE.ITEM_TYPE_BLANK.ordinal -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_picturex_blank, parent, false)
                return BlankViewHolder(view)
            }

            ITEM_TYPE.ITEM_TYPE_RELATIVE.ordinal -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_picturex_relative, parent, false)
                return RelatedHolder(view)
            }

            else -> {
                val binding = ViewPicturexDetailBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return DetailViewHolder(binding)
            }
        }
    }

    class BlankViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            imageUrls.size -> ITEM_TYPE.ITEM_TYPE_DETAIL.ordinal
            imageUrls.size + 1 -> ITEM_TYPE.ITEM_TYPE_RELATIVE.ordinal
            imageUrls.size + 2 -> ITEM_TYPE.ITEM_TYPE_BLANK.ordinal

            else -> {
                if (data.type != "ugoira") {
                    ITEM_TYPE.ITEM_TYPE_PICTURE.ordinal
                } else {
                    ITEM_TYPE.ITEM_TYPE_GIF.ordinal
                }
            }
        }
    }

    enum class ITEM_TYPE {
        ITEM_TYPE_PICTURE,
        ITEM_TYPE_BLANK,
        ITEM_TYPE_DETAIL,
        ITEM_TYPE_RELATIVE,
        ITEM_TYPE_GIF
    }

    override fun getItemCount() = imageUrls.size + 3

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val position = holder.bindingAdapterPosition
        when (holder) {
            is PictureViewHolder -> setPicViewHolder(holder, position, mContext)

            is SurfaceGifViewHolder -> setGifViewHolder(holder, position)

            is DetailViewHolder -> holder.updateWithPage(
                mContext, data,
                mViewCommentListen,
                mUserPicLongClick
            )

            is RelatedHolder -> {
//            relatedtureAdapter.openLoadAnimation(BaseQuickAdapter.SCALEIN)
                holder.updateWithPage(relatedPictureAdapter, mContext)
            }
        }
    }

    private fun setPicViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        mContext: Context
    ) {
        val mainImage = holder.itemView.findViewById<ImageView>(R.id.imageview_pic)
        if (mContext.resources.configuration.orientation == ORIENTATION_LANDSCAPE) {
            mainImage.maxHeight = screenHeightPx()
            //mainImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
            //TODO: check wh ratio and span size
        }
        Glide.with(mContext).load(imageUrls[position])
            .placeholder(if (position % 2 == 1) R.color.transparent else R.color.halftrans)
            .run {
                if (position == 0) {
                    val minPercentage = min(1024f / data.width, 1024f / data.height)
                    thumbnail(
                        Glide.with(mContext).load(imageThumbnail)
                            .override(
                                (minPercentage * data.width).roundToInt(),
                                (minPercentage * data.height).roundToInt()
                            )
                            .centerCrop()
                            .listener(object : RequestListener<Drawable> {

                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                mListen.invoke()
                                (mContext as FragmentActivity).supportStartPostponedEnterTransition()
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                mListen.invoke()
                                (mContext as FragmentActivity).supportStartPostponedEnterTransition()
                                return false
                            }
                            })
                    )
                } else this
            }
            .transition(withCrossFade())
            .into(mainImage)
        // show detail of illust
        mainImage.setOnLongClickListener {
            MaterialDialogs(mContext).show {
                setTitle(R.string.saveselectpic1)
                setMessage(InteractionUtil.toDetailString(data))
                confirmButton { _, _ ->
                    ToastQ.post(R.string.join_download_queue)
                    Works.imgD(data, position)
                }
                cancelButton()
                if (data.meta.size > 1) {
                    setNeutralButton(R.string.multichoicesave) { _, _ ->
                        val mSelectedItems = ArrayList<Int>() // Where we track the selected items
                        val showList = data.meta.indices.map { it.toString() }.toTypedArray()
                        //all elements initialized to false
                        val boolList = BooleanArray(showList.size)
                        MaterialDialogs(mContext).show {
                            setTitle(R.string.choice)
                            setMultiChoiceItems(showList, boolList) { _, which, isChecked ->
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    mSelectedItems.add(which)
                                } else if (mSelectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    mSelectedItems.remove(Integer.valueOf(which))
                                }
                            }
                            confirmButton { _, _ ->
                                ToastQ.post(R.string.join_download_queue)
                                mSelectedItems.map {
                                    Works.imgD(data, it)
                                }
                            }
                            cancelButton()
                            setNeutralButton(R.string.all) { dialog, _ -> } // see below
                        }.apply {
                            getButton(BUTTON_NEUTRAL).setOnClickListener {
                                mSelectedItems.clear()
                                for (i in boolList.indices) {
                                    boolList[i] = true
                                    listView.setItemChecked(i, true)
                                    mSelectedItems.add(i)
                                }
                            }
                        }
                    }
                }
            }
            true
        }
        mainImage.setOnClickListener {
            ZoomFragment.start(mContext, position, pictureXViewModel.illustDetail.value!!)
        }
        if (position == 0 && PxEZApp.animationEnable) {
            mainImage.transitionName = "mainimage"
        }
        // (mContext as FragmentActivity).supportStartPostponedEnterTransition()
    }


    fun setUserDataIsFollowed(it: Boolean) {
        data.user.is_followed = it
        notifyItemChanged(imageUrls.size)
    }

    // ---- GIF ----
    private fun setGifViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val binding = ViewPicturexSurfaceGifBinding.bind(holder.itemView)
        gifProgressBar = binding.progressbarGif
        gifPlay = binding.imageviewPlay

        imageViewGif = binding.imageviewGif
        val s = (data.height.toFloat() / data.width.toFloat())
        holder.itemView.post {
            val finalHeight = s * holder.itemView.width.toFloat()
            binding.container.apply {
                layoutParams = layoutParams.apply {
                    width = FrameLayout.LayoutParams.MATCH_PARENT
                    height = finalHeight.toInt()
                }
            }
            imageViewGif!!.layoutParams = imageViewGif!!.layoutParams.apply {
                width = FrameLayout.LayoutParams.MATCH_PARENT
                height = finalHeight.toInt()
            }
        }
        Glide.with(mContext).load(imageUrls[position])
            .placeholder(if (position % 2 == 1) R.color.transparent else R.color.halftrans)
            .transition(withCrossFade()).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    mListen.invoke()
                    if (position == 0) {
                        (mContext as FragmentActivity).supportStartPostponedEnterTransition()
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    mListen.invoke()
                    if (position == 0) {
                        (mContext as FragmentActivity).supportStartPostponedEnterTransition()
                    }
                    return false
                }
            })
            .into(binding.preview)
        previewImageView = binding.preview
        imageViewGif!!.setOnLongClickListener {
            if (gifProgressBar?.visibility != View.VISIBLE) {
                showGIFDialog(data)
            }
            true
        }
        gifPlay.setOnClickListener {
            if (gifPlay.visibility != View.GONE) {
                gifPlay.visibility = View.GONE
                Toasty.info(PxEZApp.instance, "Ugoira loading...")
                pictureXViewModel.viewModelScope.launchCatching({
                    pictureXViewModel.loadGif(data.id)
                }, {
                    when (max(quality, Works.qualityDownload)) {
                        0 -> pictureXViewModel.downloadZip(
                            it.zip_urls.medium
                        )

                        else -> pictureXViewModel.downloadZip(
                            it.zip_urls.medium.replace("600x600", "1920x1080")
                        )
                        //2 -> pictureXViewModel.downloadUgoira(data, size)
                    }
                }, {
                    CrashHandler.instance.d("gif ", "loading failed")
                    gifPlay.visibility = View.VISIBLE
                })
            }
        }
    }

    //Don't use lateinit as it will be called afte reconstruct
    private var gifProgressBar: CircularProgressIndicator? = null
    private lateinit var gifPlay: View
    var imageViewGif: AnimationView? = null

    private fun showGIFDialog(illust: Illust) {

        fun saveGIF() {
            if (!pictureXViewModel.isEncoding) {
                pictureXViewModel.saveGIF()
            } else {
                Toasty.warning(PxEZApp.instance, R.string.already_encoding)
            }
        }

        fun saveZIP() {
            val filePath =
                PxEZApp.storepath + File.separatorChar +
                        (if (PxEZApp.R18Folder && illust.restricted) PxEZApp.R18FolderPath else "") +
                        Works.parseSaveFormat(illust).substringBeforeLast(".").removePrefix("？")
            val zipPath: String =
                PxEZApp.instance.cacheDir.toString() + File.separatorChar + illust.id + ".zip"
            val fileCachedZIP = File(zipPath)
            if (fileCachedZIP.exists()) {
                fileCachedZIP.copyTo(
                    File("$filePath.zip"),
                    overwrite = true
                )
                Toasty.info(PxEZApp.instance, R.string.save_zip_success)
            } else {
                Toasty.error(PxEZApp.instance, R.string.not_downloaded)
            }
        }

        MaterialDialogs(mContext).show {
            setTitle(R.string.choice)
            setItems(
                arrayOf(
                    mContext.getString(R.string.savefirst),
                    mContext.getString(R.string.ugoira),
                    mContext.getString(R.string.encode_gif),
                    mContext.getString(R.string.save_zip),
                    mContext.getString(R.string.all),
                )
            ) { _, index ->
                when (index) {
                    0 -> Works.imgD(illust, 0)
                    1 -> pictureXViewModel.downloadUgoira()
                    2 -> saveGIF()
                    3 -> saveZIP()
                    else -> {
                        /* pictureXViewModel.downloadUgoira()
                         pictureXViewModel.zipUgoira()
                         pictureXViewModel.convertGIFUgoira()*/
                        Works.imgD(illust, 0)
                        saveGIF()
                        saveZIP()
                    }
                }
            }
        }
    }

    fun setProgress(progress: Int) {
        gifProgressBar?.setProgressCompat(progress, true)
    }

    private var previewImageView: ImageView? = null
    fun setProgressComplete(it: Boolean) {
        gifProgressBar?.visibility = View.GONE
        if (!it) {
            gifPlay.visibility = View.VISIBLE
            return
        }
        previewImageView?.visibility = View.GONE
        val parentPath = PxEZApp.instance.cacheDir.path + File.separatorChar + data.id
        val parentFile = File(parentPath)
        val listFiles = parentFile.listFiles()!!
        listFiles.sortWith { o1, o2 -> o1.name.compareTo(o2.name) }
        val result = listFiles.map { it.path }
        imageViewGif?.onStartListener { }
        imageViewGif?.onEndListener { }

        imageViewGif?.delayTime = pictureXViewModel.duration.toLong()
        imageViewGif?.startAnimation(result)
    }

    // ---- related ----
    val relatedPictureAdapter = SquareMediumAdapter(R.layout.view_relatedpic_item).also {
        it.isAutoLoadMore = false
        it.setOnItemClickListener { adapter, view, position ->
            val data = adapter.data as MutableList<Illust>
            DataHolder.setIllustList(data)
            PictureActivity.start(
                mContext, data[position].id, position, position,
                ActivityOptions.makeSceneTransitionAnimation(
                    mContext as Activity,
                    Pair(view, "shared_element_container")
                    //Pair(mainimage, "mainimage")
                ).toBundle()
            )
        }
    }
}
