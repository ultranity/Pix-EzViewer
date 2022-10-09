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

package com.perol.asdpl.pixivez.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface.BUTTON_NEUTRAL
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target
import com.dinuscxj.progressbar.CircleProgressBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.activity.SearchRActivity
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.activity.ZoomActivity
import com.perol.asdpl.pixivez.databinding.ViewPicturexDetailBinding
import com.perol.asdpl.pixivez.databinding.ViewPicturexSurfaceGifBinding
import com.perol.asdpl.pixivez.objects.*
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.responses.Tag
import com.perol.asdpl.pixivez.services.GlideApp
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.sql.AppDatabase
import com.perol.asdpl.pixivez.sql.entity.BlockTagEntity
import com.perol.asdpl.pixivez.viewmodel.PictureXViewModel
import com.perol.asdpl.pixivez.viewmodel.ProgressInfo
import com.shehuan.niv.NiceImageView
import com.waynejo.androidndkgif.GifEncoder
import com.zhy.view.flowlayout.FlowLayout
import com.zhy.view.flowlayout.TagAdapter
import com.zhy.view.flowlayout.TagFlowLayout
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.extensions.LayoutContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.File


class PictureXAdapter(
    private val pictureXViewModel: PictureXViewModel,
    private val data: Illust,
    private val mContext: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val imageUrls = ArrayList<String>()
    val imageThumbnailUrls = ArrayList<String>()
    val pre = PreferenceManager.getDefaultSharedPreferences(mContext)!!
    lateinit var mListen: () -> Unit
    private lateinit var mViewCommentListen: () -> Unit
    private lateinit var mBookmarkedUserListen: () -> Unit
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
    fun setBookmarkedUserListen(listener: () -> Unit) {
        this.mBookmarkedUserListen = listener
    }

    init {
        val quality =
            pre.getString("quality","0")?.toInt()?: 0
        when (quality) {
            0 -> {
                if (data.meta_pages.isEmpty()) {
                    imageUrls.add(data.image_urls.medium)
                } else {
                    data.meta_pages.map {
                        imageUrls.add(it.image_urls.medium)
                    }
                }
            }
            else -> {
                if (data.meta_pages.isEmpty()) {
                    imageUrls.add(data.image_urls.large)
                } else {
                    data.meta_pages.map {
                        imageUrls.add(it.image_urls.large)
                    }
                }
            }
        }
        val needSmall =if(quality == 1)
                            (data.height/data.width > 3) ||(data.width/data.height >= 3)
                        else
                            data.height > 1800
        if (needSmall) {
            imageThumbnailUrls.add(data.image_urls.square_medium)
        }
        else {
            imageThumbnailUrls.add(data.image_urls.medium)
        }

        /*if (needSmall) {
            if (data.meta_pages.isEmpty()) {
                imageThumbnailUrls.add(data.image_urls.square_medium)
            } else {
                data.meta_pages.map {
                    imageThumbnailUrls.add(it.image_urls.square_medium)
                }
            }
        }
        else {
            if (data.meta_pages.isEmpty()) {
                imageUrls.add(data.image_urls.medium)
            } else {
                data.meta_pages.map {
                    imageUrls.add(it.image_urls.medium)
                }
            }
        }*/
    }

    class PictureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    }

    class GifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    }

    class SurfaceGifViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer {

    }

    class FisrtDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    }

    class DetailViewHolder(
        var binding: ViewPicturexDetailBinding
    )
        : RecyclerView.ViewHolder(binding.root) {
        private val tagFlowLayout = itemView.findViewById<TagFlowLayout>(R.id.tagflowlayout)
        private val captionTextView = itemView.findViewById<TextView>(R.id.textview_caption)
        private val btnTranslate = itemView.findViewById<TextView>(R.id.btn_translate)
        private val viewCommentTextView = itemView.findViewById<TextView>(R.id.textview_viewcomment)
        private val imageViewUser = itemView.findViewById<NiceImageView>(R.id.imageViewUser_picX)
        private val imageButtonDownload =
            itemView.findViewById<ImageButton>(R.id.imagebutton_download)
        private val bookmarkedUserNum =
            itemView.findViewById<TextView>(R.id.bookmarked_user_num)

        fun updateWithPage(
            mContext: Context,
            illust: Illust,
            mViewCommentListen: () -> Unit,
            mBookmarkedUserListen: () -> Unit,
            mUserPicLongClick: () -> Unit
        ) {
            binding.illust = illust
            //captionTextView.autoLinkMask = Linkify.WEB_URLS
            val colorPrimary = ThemeUtil.getColor(mContext, androidx.appcompat.R.attr.colorPrimary)
            val colorPrimaryDark = ThemeUtil.getColor(mContext, androidx.appcompat.R.attr.colorPrimaryDark)
            val badgeTextColor= ThemeUtil.getColor(mContext, com.google.android.material.R.attr.badgeTextColor)
            if (illust.user.is_followed)
                imageViewUser.setBorderColor(badgeTextColor)
            else
                imageViewUser.setBorderColor(colorPrimary)
            imageViewUser.setOnLongClickListener {
                mUserPicLongClick.invoke()
                true
            }
            imageViewUser.setOnClickListener {
                val intent = Intent(mContext, UserMActivity::class.java)
                intent.putExtra("data", illust.user.id)
                if (PxEZApp.animationEnable) {
                    val options = ActivityOptions.makeSceneTransitionAnimation(
                        mContext as Activity,
                        Pair.create(imageViewUser, "UserImage")
                    )
                    mContext.startActivity(intent, options.toBundle())
                } else
                    mContext.startActivity(intent)
            }
            if (illust.caption.isNotBlank())
                binding.html = Html.fromHtml(illust.caption)
            else
                binding.html = Html.fromHtml("~")
            Linkify.addLinks(captionTextView, Linkify.WEB_URLS)
            Log.d("url", captionTextView.urls.toString())
            captionTextView.movementMethod = LinkMovementMethod.getInstance()
            viewCommentTextView.setOnClickListener {
                mViewCommentListen.invoke()
            }
            bookmarkedUserNum.setOnClickListener {
                mBookmarkedUserListen.invoke()
            }
            //google translate app btn click listener
            val intent = Intent()
                .setType("text/plain")
            var componentPackageName = ""
            var componentName = ""
            var isGoogleTranslateEnabled = false
            //check google translate
            for (resolveInfo: ResolveInfo in mContext.packageManager.queryIntentActivities(
                intent,
                0
            )) {
                try {
                    //emui null point exception
                    if (resolveInfo.activityInfo.packageName.contains("com.google.android.apps.translate")) {
                        isGoogleTranslateEnabled = true
                        componentPackageName = resolveInfo.activityInfo.packageName
                        componentName = resolveInfo.activityInfo.name
                    }
                } catch (e: Exception) {

                }

            }
            if (!isGoogleTranslateEnabled) btnTranslate.visibility = View.GONE
            else {
                btnTranslate.setOnClickListener {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        intent.action = Intent.ACTION_PROCESS_TEXT
                        intent.putExtra(Intent.EXTRA_PROCESS_TEXT, captionTextView.text.toString())
                    } else {
                        intent.action = Intent.ACTION_SEND
                        intent.putExtra(Intent.EXTRA_TEXT, captionTextView.text.toString())
                    }
                    intent.component = ComponentName(
                        componentPackageName,
                        componentName
                    )
                    mContext.startActivity(intent)

                }
            }

            tagFlowLayout.apply {
                adapter = object : TagAdapter<Tag>(illust.tags) {
                    @SuppressLint("SetTextI18n")
                    override fun getView(parent: FlowLayout, position: Int, t: Tag): View {
                        val tv = LayoutInflater.from(context)
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
                        translateName.setOnClickListener {
                            val bundle = Bundle()
                            bundle.putString("searchword", illust.tags[position].name)
                            val intent = Intent(context, SearchRActivity::class.java)
                            intent.putExtras(bundle)
                            context.startActivity(intent)
                        }
                        name.setOnClickListener {
                            val bundle = Bundle()
                            bundle.putString("searchword", illust.tags[position].name)
                            val intent = Intent(context, SearchRActivity::class.java)
                            intent.putExtras(bundle)
                            context.startActivity(intent)
                        }
                        translateName.setOnLongClickListener {
                            MaterialDialog(mContext).show {
                                title(R.string.add_to_block_tag_list)
                                negativeButton(android.R.string.cancel)
                                positiveButton(android.R.string.ok) {
                                    runBlocking {
                                        withContext(Dispatchers.IO) {
                                            AppDatabase.getInstance(PxEZApp.instance).blockTagDao()
                                                .insert(
                                                    BlockTagEntity(
                                                        name = t.name,
                                                        translateName = "${t.translated_name}"
                                                    )
                                                )
                                        }
                                        EventBus.getDefault().post(AdapterRefreshEvent())
                                    }
                                }
                                lifecycleOwner(binding.lifecycleOwner)
                            }
                            true
                        }
                        name.setOnLongClickListener {
                            MaterialDialog(mContext).show {
                                title(R.string.add_to_block_tag_list)
                                negativeButton(android.R.string.cancel)
                                positiveButton(android.R.string.ok) {
                                    runBlocking {
                                        withContext(Dispatchers.IO) {
                                            AppDatabase.getInstance(PxEZApp.instance).blockTagDao()
                                                .insert(
                                                    BlockTagEntity(
                                                        name = t.name,
                                                        translateName = "${t.translated_name}"
                                                    )
                                                )
                                        }
                                        EventBus.getDefault().post(AdapterRefreshEvent())
                                    }
                                }
                                lifecycleOwner(binding.lifecycleOwner)
                            }
                            true
                        }
                        return tv
                    }
                }


            }
            binding.imagebuttonShare.setOnClickListener {
                val textIntent = Intent(Intent.ACTION_SEND)
                textIntent.type = "text/plain"
                textIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "https://www.pixiv.net/member_illust.php?illust_id=${illust.id}&mode=medium"
                )
                mContext.startActivity(Intent.createChooser(textIntent, mContext.getString(R.string.share)))
            }
            if(FileUtil.isDownloaded(illust))
                imageButtonDownload.drawable.setTint(badgeTextColor)
            if (illust.type == "ugoira") //gif
                imageButtonDownload.setOnClickListener {

                    MaterialAlertDialogBuilder(mContext as Activity)
                        .setTitle(R.string.download)
                        .setPositiveButton(R.string.savefirst) { _, _ ->
                            Works.imageDownloadAll(illust)
                        }.show()
                }
            else
                imageButtonDownload.setOnClickListener {
                    imageButtonDownload.drawable.setTint(colorPrimaryDark)
                    Works.imageDownloadAll(illust)
                }
            imageButtonDownload.setOnLongClickListener {
                //show detail of illust
                val detailstring =InteractionUtil.toDetailString(illust, false)
                MaterialAlertDialogBuilder(mContext as Activity)
                    .setMessage(detailstring)
                    .setTitle("Detail")
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                    }
                    .create().show()
                true
            }
        }
    }

    class RelatedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun updateWithPage(s: RelatedPictureAdapter, mContext: Context) {
            recyclerView.layoutManager = GridLayoutManager(mContext, 1+2*mContext.resources.configuration.orientation)
            recyclerView.adapter = s
        }

        val recyclerView = itemView.findViewById<RecyclerView>(R.id.recyclerview_related)!!

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
                    .inflate(R.layout.view_picturex_firstdetail, parent, false)
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

    class BlankViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            imageUrls.size -> ITEM_TYPE.ITEM_TYPE_DETAIL.ordinal
            imageUrls.size + 1 -> ITEM_TYPE.ITEM_TYPE_RELATIVE.ordinal
            imageUrls.size + 2 -> ITEM_TYPE.ITEM_TYPE_BLANK.ordinal

            else -> {
                if (data.type != "ugoira")
                    ITEM_TYPE.ITEM_TYPE_PICTURE.ordinal
                else
                    ITEM_TYPE.ITEM_TYPE_GIF.ordinal
            }
        }
    }


    enum class ITEM_TYPE {
        ITEM_TYPE_PICTURE,
        ITEM_TYPE_BLANK,
        ITEM_TYPE_DETAIL,
        ITEM_TYPE_RELATIVE,
        ITEM_TYPE_GIF,
    }

    override fun getItemCount() = imageUrls.size + 3


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val position = holder.bindingAdapterPosition
        when(holder) {
            is PictureViewHolder -> {
                val mainImage = holder.itemView.findViewById<ImageView>(R.id.imageview_pic)
                GlideApp.with(mContext).load(imageUrls[position]).placeholder(if(position % 2 == 1) R.color.transparent  else R.color.halftrans)
                    .thumbnail(GlideApp.with(mContext).load(if(position == 0) imageThumbnailUrls[0]  else ColorDrawable(ThemeUtil.halftrans)))
                    .transition(withCrossFade()).listener(object : RequestListener<Drawable> {

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            mListen.invoke()
                            if (position == 0)
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
                            if (position == 0) {
                                mListen.invoke()
                                (mContext as FragmentActivity).supportStartPostponedEnterTransition()
                            }
                            return false
                        }
                    })
                    .into(object : ImageViewTarget<Drawable>(mainImage) {
                        override fun setResource(resource: Drawable?) {
                            mainImage.setImageDrawable(resource)
                        }
                    })//.into(mainImage)
                mainImage.apply {
                    setOnLongClickListener {
                        val builder = MaterialAlertDialogBuilder(mContext as Activity)
                        builder.setTitle(mContext.resources.getString(R.string.saveselectpic1))
                        //show detail of illust
                        val detailstring = InteractionUtil.toDetailString(data)
                        builder.setMessage(detailstring)
                        builder.setPositiveButton(mContext.resources.getString(R.string.confirm)) { dialog, which ->
                            TToast.startDownload()
                            Works.imgD(data, position)
                        }
                        builder.setNegativeButton(mContext.resources.getString(android.R.string.cancel)) { dialog, which ->

                        }
                        if (data.meta_pages.isNotEmpty()) {
                            builder.setNeutralButton(R.string.multichoicesave) { _, which ->

                                val list = ArrayList<String>()
                                data.meta_pages.map { ot ->
                                    list.add(ot.image_urls.original)
                                }
                                val mSelectedItems =
                                    ArrayList<Int>()  // Where we track the selected items
                                val builder = MaterialAlertDialogBuilder(mContext)
                                val showlist = ArrayList<String>()
                                for (i in list.indices) {
                                    showlist.add(i.toString())
                                }
                                val boolean = BooleanArray(showlist.size)
                                for (i in boolean.indices) {
                                    boolean[i] = false
                                }
                                builder.setTitle(R.string.choice)
                                    .setMultiChoiceItems(
                                        showlist.toTypedArray(), boolean
                                    ) { _, which, isChecked ->
                                        if (isChecked) {
                                            // If the user checked the item, add it to the selected items

                                            mSelectedItems.add(which)
                                        } else if (mSelectedItems.contains(which)) {
                                            // Else, if the item is already in the array, remove it
                                            mSelectedItems.remove(Integer.valueOf(which))
                                        }
                                    }
                                    // Set the action buttons
                                builder.setPositiveButton(android.R.string.ok) { dialog, id ->
                                        TToast.startDownload()
                                        mSelectedItems.map {
                                            Works.imgD(data, it)
                                        }
                                    }
                                    .setNegativeButton(android.R.string.cancel) { dialog, id -> }
                                    .setNeutralButton(R.string.selectAll) { _, id ->
                                        //see below
                                    }
                                val dialog = builder.create()
                                dialog.show()
                                dialog.getButton(BUTTON_NEUTRAL).setOnClickListener {
                                    for (i in boolean.indices) {
                                        boolean[i] = true
                                        dialog.listView.setItemChecked(i,true)
                                    }
                                    mSelectedItems.clear()
                                    for (i in showlist.indices) {
                                        mSelectedItems.add(i)
                                    }
                                }
                            }
                        }

                        val dialog = builder.create()
                        dialog.show()
                        true
                    }
                    setOnClickListener {

                        val intent = Intent(mContext, ZoomActivity::class.java)
                        val bundle = Bundle()
                        bundle.putInt("num", position)
                        bundle.putParcelable(
                            "illust",
                            pictureXViewModel.illustDetail.value
                        )
                        intent.putExtras(bundle)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        mContext.startActivity(intent)
                    }
                    if (position == 0 && PxEZApp.animationEnable) {
                        transitionName = "mainimage"
                    }
                }
//            (mContext as FragmentActivity).supportStartPostponedEnterTransition()
            }
            is SurfaceGifViewHolder -> {
                val binding = ViewPicturexSurfaceGifBinding.bind(holder.itemView)
                gifProgressBar = binding.progressbarGif
                val play = binding.imageviewPlay

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
                GlideApp.with(mContext).load(imageUrls[position]).placeholder(if(position % 2 == 1) R.color.transparent  else R.color.halftrans)
                    .transition(withCrossFade()).listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            mListen.invoke()
                            if (position == 0)
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
                            if (position == 0)
                                (mContext as FragmentActivity).supportStartPostponedEnterTransition()
                            return false
                        }
                    }).into(binding.preview)
                previewImageView = binding.preview
                val path2 = PxEZApp.storepath + File.separatorChar + (if(PxEZApp.R18Folder && data.x_restrict == 1) PxEZApp.R18FolderPath else "") +
                        Works.parseSaveFormat(data).substringBeforeLast(".").removePrefix("？") + ".gif"
                imageViewGif!!.setOnLongClickListener {
                    if (gifProgressBar?.visibility != View.VISIBLE) {
                        MaterialDialog(mContext).show {
                            title(R.string.choice)
                            listItems(
                                items = arrayListOf(
                                    mContext.getString(R.string.encodinggif),
                                    mContext.getString(R.string.save_zip)
                                )
                            ) { dialog, index, text ->
                                when (index) {
                                    0 -> {
                                        if (!isEncoding) {
                                            isEncoding = true
                                            val file1 = File(path2)
                                            if (!file1.parentFile.exists()) {
                                                file1.parentFile.mkdirs()
                                            }
                                            val ob = encodingGif()
                                            //TODO: Works.imageDownloadWithFile(illust, resourceFile!!, position)
                                            if (ob != null) {
                                                ob.subscribe({
                                                    runBlocking {
                                                        withContext(Dispatchers.IO) {
                                                            File(path).copyTo(file1, true)
                                                        }
                                                        MediaScannerConnection.scanFile(
                                                            PxEZApp.instance,
                                                            arrayOf(path2),
                                                            arrayOf(
                                                                MimeTypeMap.getSingleton()
                                                                    .getMimeTypeFromExtension(
                                                                        file1.extension
                                                                    )
                                                            )
                                                        ) { _, _ ->

                                                        }
                                                        isEncoding = false
                                                        File(path).delete()
                                                        Toast.makeText(
                                                            PxEZApp.instance,
                                                            R.string.savegifsuccess,
                                                            Toast.LENGTH_SHORT
                                                        ).show()

                                                    }
                                                }, {
                                                    isEncoding = false
                                                    Toast.makeText(
                                                        PxEZApp.instance,
                                                        R.string.savegifsuccesserr,
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                }, {

                                                })
                                            } else {
                                                runBlocking {
                                                    withContext(Dispatchers.IO) {
                                                        File(path).copyTo(file1, true)
                                                    }
                                                    MediaScannerConnection.scanFile(
                                                        PxEZApp.instance, arrayOf(path2), arrayOf(
                                                            MimeTypeMap.getSingleton()
                                                                .getMimeTypeFromExtension(
                                                                    file1.extension
                                                                )
                                                        )
                                                    ) { _, _ ->
                                                    }
                                                    isEncoding = false
                                                    Toast.makeText(
                                                        PxEZApp.instance,
                                                        R.string.savegifsuccess,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }

                                            }
                                        } else {
                                            Toasty.info(
                                                PxEZApp.instance,
                                                mContext.getString(R.string.already_encoding),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                    else -> {
                                        val zipPath: String = PxEZApp.instance.cacheDir.toString() + File.separatorChar + data.id + ".zip"
                                        val file1 = File(zipPath)
                                        if (file1.exists()) {
                                            file1.copyTo(
                                                File(path2.substringBeforeLast(".")+".zip"),
                                                overwrite = true
                                            )
                                            Toasty.info(
                                                PxEZApp.instance,
                                                mContext.getString(R.string.save_zip_success),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    true
                }
                play.setOnClickListener {
                    play.visibility = View.GONE
                    Toasty.info(PxEZApp.instance, "Downloading...", Toast.LENGTH_SHORT).show()
                    pictureXViewModel.loadGif(data.id).flatMap {
                        duration = it.ugoira_metadata.frames[0].delay
                        pictureXViewModel.downloadZip(
                            it.ugoira_metadata.zip_urls.medium
                        )
                        return@flatMap Observable.just(it)
                    }.subscribe({
                    }, {
                        Log.d("throw", "throw it")
                        play.visibility = View.VISIBLE
                    }, {})
                }

            }
            is DetailViewHolder ->
                holder.updateWithPage(mContext, data, mViewCommentListen,mBookmarkedUserListen, mUserPicLongClick)
            is RelatedHolder -> {
//            aboutPictureAdapter.openLoadAnimation(BaseQuickAdapter.SCALEIN)
                holder.updateWithPage(relatedPictureAdapter, mContext)
            }
        }
    }

    var isEncoding = false

    private val path: String = PxEZApp.instance.cacheDir.toString() + File.separatorChar + data.id + ".gif"
    private fun encodingGif(): Observable<Int>? {
        val pathOk: String = PxEZApp.instance.cacheDir.toString() + File.separatorChar + data.id + ".ojbk"
        val fileOk = File(pathOk)
        if (fileOk.exists()) {
            return null
        }
        val parentPath = PxEZApp.instance.cacheDir.path + File.separatorChar + data.id
        val parentFile = File(parentPath)
        val listFiles = parentFile.listFiles()
        if (listFiles == null || listFiles.isEmpty()) {
            throw RuntimeException("unzip files not found")
        }
        if (listFiles.size < size) {
            throw RuntimeException("something wrong in ugoira files")
        }
        Toasty.info(PxEZApp.instance, "约有${listFiles.size}张图片正在合成", Toast.LENGTH_SHORT).show()
        return Observable.create<Int> {
            listFiles.sortWith { o1, o2 -> o1.name.compareTo(o2.name) }
            val gifEncoder = GifEncoder()
            for (i in listFiles.indices) {
                if (listFiles[i].isFile) {
                    val bitmap = BitmapFactory.decodeFile(listFiles[i].absolutePath)

                    if (i == 0) {
                        gifEncoder.init(
                            bitmap.width,
                            bitmap.height,
                            path,
                            GifEncoder.EncodingType.ENCODING_TYPE_STABLE_HIGH_MEMORY
                        )
                    }
                    gifEncoder.encodeFrame(bitmap, duration)
                    Log.d("progressset", i.toString())
                }

            }
            gifEncoder.close()
            fileOk.mkdirs()
            it.onNext(1)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())


    }


    var size = 1
    var duration: Int = 50
    private var gifProgressBar: CircleProgressBar? = null
    var imageViewGif: AnimationView? = null
    private val relatedPictureAdapter = RelatedPictureAdapter(R.layout.view_relatedpic_item)
    fun setRelatedPics(it: ArrayList<Illust>) {
        if (it.isEmpty()) {
            return
        }
        val list = it.map { it.image_urls.square_medium }.toMutableList()


        relatedPictureAdapter.setNewData(list)
        relatedPictureAdapter.setOnItemClickListener { adapter, view, position ->
            val bundle = Bundle()
            //val id = it[position].id
            //val arrayList = ArrayList<Long>()
            //it.forEach {
            //    arrayList.add(it.id)
            //}
            //bundle.putLongArray("illustidlist", arrayList.toLongArray())
            bundle.putLong("illustid", it[position].id)
            bundle.putInt("position", position)
            DataHolder.setIllustsList(it)
            val intent = Intent(mContext, PictureActivity::class.java)
            intent.putExtras(bundle)
            mContext.startActivity(intent)
        }
    }

    fun setProgress(it: ProgressInfo) {
        if (gifProgressBar != null) {
            gifProgressBar!!.max = it.all.toInt()
            gifProgressBar!!.progress = it.now.toInt()
        }
    }

    fun setUserPicColor(it: Boolean) {
        data.user.is_followed = it
        notifyItemChanged(imageUrls.size)

    }

    var previewImageView: ImageView? = null
    fun setProgressComplete(it: Boolean) {
        gifProgressBar?.visibility = View.GONE
        previewImageView?.visibility = View.GONE
        val parentPath = PxEZApp.instance.cacheDir.path + File.separatorChar + data.id
        val parentFile = File(parentPath)
        val listFiles = parentFile.listFiles()!!
        listFiles.sortWith { o1, o2 -> o1.name.compareTo(o2.name) }
        val result = listFiles.map {
            it.path
        }
        imageViewGif?.onStartListener {

        }
        imageViewGif?.onEndListener {

        }

        imageViewGif?.delayTime = duration.toLong()
        imageViewGif?.startAnimation(result)
    }
}