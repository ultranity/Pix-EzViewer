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

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.chrynan.parcelable.core.getParcelableExtra
import com.chrynan.parcelable.core.putExtra
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.data.entity.UserEntity
import com.perol.asdpl.pixivez.data.model.ProfileImageUrls
import com.perol.asdpl.pixivez.data.model.User
import com.perol.asdpl.pixivez.databinding.ActivityUserMBinding
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.objects.UpToTopListener
import com.perol.asdpl.pixivez.objects.UserCacheRepo
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.settings.BlockViewModel
import com.perol.asdpl.pixivez.view.AppBarStateChangeListener
import com.perol.asdpl.pixivez.view.AutoTabLayoutMediator
import com.perol.asdpl.pixivez.view.loadBGImage
import com.perol.asdpl.pixivez.view.loadUserImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class UserMActivity : RinkActivity() {
    companion object {
        fun start(context: Context, id: Int, options: Bundle? = null, skipBlock: Boolean = false) {
            if (!skipBlock && id in BlockViewModel.getBlockUIDs()) {
                Snackbar.make(
                    (context as Activity).findViewById(android.R.id.content),
                    context.getString(R.string.view_user_blocked, id),
                    Snackbar.LENGTH_SHORT
                ).setAction(R.string.confirm) {
                    start(context, id, options, true)
                }.show()
                return
            }
            val intent = Intent(context, UserMActivity::class.java).setAction("user.id.start")
            intent.putExtra(if (UserCacheRepo.get(id) != null) "userid" else "uid", id)
            context.startActivity(intent, options)
        }

        fun start(context: Context, user: User, options: Bundle? = null) =
            start(context, user.id, options)

        fun start(context: Context, options: Bundle? = null) {
            val intent = Intent(context, UserMActivity::class.java).setAction("user.start")
            context.startActivity(intent, options)
        }

        fun UserEntity.toUser() = User(userid, username, "", ProfileImageUrls(userimage), "", false)
        fun start(context: Context, user: UserEntity, options: Bundle? = null) {
            val intent = Intent(context, UserMActivity::class.java).setAction("user.entity.start")
            intent.putExtra("user", user.toUser())
            context.startActivity(intent, options)
        }
    }

    var id: Int = 0
    private val SELECT_IMAGE = 2

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            val selectedImage = data.data
            val filePathColumns = arrayOf(MediaStore.Images.Media.DATA)
            val c = contentResolver.query(selectedImage!!, filePathColumns, null, null, null)
            c!!.moveToFirst()
            val columnIndex = c.getColumnIndex(filePathColumns[0])
            val imagePath = c.getString(columnIndex)
            Toasty.info(this, R.string.uploading)
            viewModel.tryToChangeProfile(imagePath)
            c.close()
        }
    }

    private val viewModel: UserMViewModel by viewModels()
    private lateinit var binding: ActivityUserMBinding
    private lateinit var user: User
    private fun setUser(usr: User) {
        user = usr
        viewModel.follow.value = user.is_followed
        user.addBinder("${user.name}|${this.hashCode()}", viewModel.follow)
        id = user.id
        binding.textviewUsername.text = user.name
        loadUserImage(binding.imageviewUserimage, user.profile_image_urls.medium)
        viewModel.insertHistory(user)

        if (AppDataRepo.isSelfPage(id)) {
            //binding.imageviewUserimage.transitionName = "CurrentUserImage"
            viewModel.currentTab.value = 2
            binding.fab.visibility = View.GONE
        } else {
            binding.fab.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserMBinding.inflate(layoutInflater)
        setContentView(binding.root)
        /*window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)*/
        window.decorView.fitsSystemWindows = false
        window.statusBarColor = Color.TRANSPARENT
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
        if (intent.extras == null) {
            if (AppDataRepo.userInited())
                setUser(AppDataRepo.currentUser.toUser())
            else return
        } else if (intent.extras!!.containsKey("userid")) {
            id = intent.extras!!.getInt("userid")
            UserCacheRepo.get(id)?.let { setUser(it) }
        } else if (intent.extras!!.containsKey("user")) {
            setUser(intent.getParcelableExtra("user", User.serializer())!!)
        } else if (intent.extras!!.containsKey("uid")) {
            id = intent.getIntExtra("uid", 0)
        }
        //避免重复加载
        if (!viewModel.userDetail.isInitialized)
            viewModel.getData(id)
        binding.viewpager.adapter = UserMPagerAdapter(this, id)
        //binding.viewpager.offscreenPageLimit = -1
        val upToTopListener = UpToTopListener(this, supportFragmentManager)
        AutoTabLayoutMediator(binding.tablayout, binding.viewpager) { tab, position ->
            tab.text = getString(UserMPagerAdapter.getPageTitle(position))
        }.attach().setOnTabReSelectedStrategy { upToTopListener.onTabReselected(it) }
        viewModel.currentTab.observeAfterSet(this) {
            binding.viewpager.setCurrentItem(it, false)
        }
        viewModel.userDetail.observe(this) {
            if (it != null) {
                if (!::user.isInitialized)
                    setUser(it.user)
                loadBGImage(binding.imageviewUserBackground, it.profile.background_image_url)
            }
        }
        viewModel.follow.observe(this) {
            if (it != null) {
                user.is_followed = it
                binding.fab.setIconResource(
                    if (it) R.drawable.ic_check_white_24dp
                    else R.drawable.ic_add_white_24dp
                )
                if (user.is_followed)
                    binding.fab.setText(
                        if (viewModel.privateFollowed.value == true)
                            R.string.following_private else R.string.following
                    )
                else
                    binding.fab.setText(R.string.follow)

            }
        }
        viewModel.privateFollowed.observe(this) {
            if (it != null)
                if (user.is_followed)
                    binding.fab.setText(if (it) R.string.following_private else R.string.following)
        }

        binding.fab.setOnClickListener {
            viewModel.onFabClick()
        }
        binding.fab.setOnLongClickListener {
            Toasty.info(applicationContext, "Private...")
            viewModel.onFabLongClick()
            true
        }

        val shareLink = "https://www.pixiv.net/member.php?id=$id"
        binding.imageviewUserimage.setOnClickListener {
            var array = resources.getStringArray(R.array.user_profile)
            if (!AppDataRepo.isSelfPage(id)) {
                array = array.sliceArray(0..1)
            }
            MaterialAlertDialogBuilder(this)
                .setItems(array) { i, which ->
                    when (which) {
                        0 -> {
                            val clipboard =
                                getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            val clip: ClipData = ClipData.newPlainText("share Link", shareLink)
                            clipboard.setPrimaryClip(clip)
                            Toasty.info(this@UserMActivity, R.string.copied)
                        }

                        1 -> {
                            CoroutineScope(Dispatchers.IO).launch {
                                val f = Glide.with(this@UserMActivity).downloadOnly()
                                    .load(viewModel.userDetail.value!!.user.profile_image_urls.medium)
                                    .submit()
                                val file = f.get()
                                val target = File(
                                    PxEZApp.storepath,
                                    "user_${viewModel.userDetail.value!!.user.id}.png"
                                )
                                FileUtil.move(file, target)
                                MediaScannerConnection.scanFile(
                                    PxEZApp.instance,
                                    arrayOf(target.path),
                                    arrayOf(
                                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                            target.extension
                                        )
                                    )
                                ) { _, _ -> }

                                withContext(Dispatchers.Main) {
                                    Toasty.info(this@UserMActivity, R.string.saved)
                                }
                            }
                        }

                        else -> {
                            val intent = Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            startActivityForResult(intent, SELECT_IMAGE)
                        }
                    }
                }.show()
        }

        val defaultTablayoutColors = binding.tablayout.tabTextColors
        binding.appBarLayout.addOnOffsetChangedListener(
            object : AppBarStateChangeListener(140) {
                override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
                    when (state) {
                        State.COLLAPSED -> {
                            binding.tablayout.setTag(
                                R.id.tag_first,
                                binding.tablayout.tabTextColors
                            )
                            binding.tablayout.setTabTextColors(
                                ThemeUtil.getTextColorPrimary(this@UserMActivity),
                                ThemeUtil.getTextColorPrimaryInverse(this@UserMActivity)
                            )
                            binding.tablayout.translationX = -15f
                        }

                        State.EXPANDED -> {
                            binding.tablayout.tabTextColors = defaultTablayoutColors
                            binding.tablayout.translationX = 0f
                        }

                        else -> {}
                    }
                }
            })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_userx, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finishAfterTransition()
            R.id.action_block_user -> block()
            R.id.action_share -> share()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun block() {
        MaterialDialogs(this).show {
            setTitle(R.string.block_user)
            confirmButton { dialog, _ ->
                //stop current fragment first to prevent recycler view crash
                binding.viewpager.adapter = null
                supportFragmentManager.fragments.forEach { it.onStop() }
                supportFragmentManager.fragments.clear()
                runBlocking(Dispatchers.IO) {
                    BlockViewModel.insertBlockUser(id)
                }
                finishAfterTransition()
            }
            cancelButton()
        }
    }

    private fun share() {
        val textIntent = Intent(Intent.ACTION_SEND)
        textIntent.type = "text/plain"
        textIntent.putExtra(Intent.EXTRA_TEXT, "https://www.pixiv.net/member.php?id=$id")
        startActivity(Intent.createChooser(textIntent, getString(R.string.share)))
    }
}
