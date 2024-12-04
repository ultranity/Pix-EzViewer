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

package com.perol.asdpl.pixivez.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.base.getInputField
import com.perol.asdpl.pixivez.base.setInput
import com.perol.asdpl.pixivez.core.TAG_TYPE
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.data.entity.UserEntity
import com.perol.asdpl.pixivez.databinding.AppMainBinding
import com.perol.asdpl.pixivez.databinding.NavHeaderMainBinding
import com.perol.asdpl.pixivez.objects.LARGE_SCREEN_WIDTH_SIZE
import com.perol.asdpl.pixivez.objects.MEDIUM_SCREEN_WIDTH_SIZE
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.objects.dp
import com.perol.asdpl.pixivez.objects.screenWidthDp
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.account.LoginActivity
import com.perol.asdpl.pixivez.ui.home.HelloMainViewPager
import com.perol.asdpl.pixivez.ui.manager.DownloadManagerActivity
import com.perol.asdpl.pixivez.ui.manager.ImgManagerActivity
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import com.perol.asdpl.pixivez.ui.search.SearchActivity
import com.perol.asdpl.pixivez.ui.search.SearchResultActivity
import com.perol.asdpl.pixivez.ui.settings.SaucenaoActivity
import com.perol.asdpl.pixivez.ui.settings.SettingsActivity
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : RinkActivity(), NavigationView.OnNavigationItemSelectedListener {

    /** ref: AdaptiveUtil in Material catalog
     * Updates the visibility of the main navigation view components according to screen size.
     *
     *
     * The small screen layout should have a bottom navigation and optionally a fab. The medium
     * layout should have a navigation rail with a fab, and the large layout should have a navigation
     * drawer with an extended fab.
     */
    private fun updateNavigationViewLayout() {
        //binding.contentView.fitsSystemWindows = true
        val fab: FloatingActionButton? = null
        val navFab: ExtendedFloatingActionButton? = null
        val screenWidthDp = screenWidthDp()
        if (screenWidthDp < MEDIUM_SCREEN_WIDTH_SIZE) {
            // Small screen
            binding.navToolbar.visibility = View.VISIBLE
            binding.navRail.visibility = View.GONE
            binding.navDrawer.visibility = View.GONE
            val useBottomBar = PxEZApp.instance.pre.getBoolean("bottomAppbar", true)
            if (useBottomBar) {
                binding.appBarLayout.visibility = View.GONE
                binding.navToolbar.removeAllViews()
                binding.bottomToolbar.addView(binding.tablayout)
                binding.bottomToolbar.visibility = View.VISIBLE
                binding.navContainer.fitsSystemWindows = true

                /* // mannual fitsystemwindows
                 val decorView = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    window.decorView.rootWindowInsets.systemWindowInsetTop
                } else { 20.dp }
                binding.contentView.setPadding(0, 30.dp, 0, 0)*/

                setSupportActionBar(binding.bottomToolbar)
            } else {
                binding.appBarLayout.visibility = View.VISIBLE
                binding.appBarLayout.fitsSystemWindows = true
                setSupportActionBar(binding.navToolbar)
                //binding.navContainer.fitsSystemWindows = false
            }
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            ActionBarDrawerToggle(
                this, binding.drawerLayout,
                if (useBottomBar) binding.bottomToolbar else binding.navToolbar,
                R.string.menu, R.string.menu
            ).apply {
                setHomeAsUpIndicator(R.drawable.ic_action_logo)
                isDrawerIndicatorEnabled = true
                setToolbarNavigationClickListener {
                    binding.drawerLayout.openDrawer(GravityCompat.START)
                }
                binding.drawerLayout.addDrawerListener(this)
                syncState()
            }
        } else { //if (screenWidth < LARGE_SCREEN_WIDTH_SIZE) {
            // Medium screen
            //fab?.visibility = View.GONE
            //navFab?.shrink()
            binding.navToolbar.visibility = View.GONE
            binding.navRail.visibility = View.VISIBLE
            binding.navDrawer.visibility = View.GONE
            //binding.navRail.setOnItemReselectedListener {
            //}
            binding.navRail.setOnItemSelectedListener {
                onOptionsItemSelected(it)
            }
            // Set navigation menu button to show modal navigation drawer.
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            binding.navRail.headerView!!.findViewById<ImageView>(R.id.nav_button)
                .setOnClickListener { binding.drawerLayout.openDrawer(binding.navView) }

            binding.navContainer.fitsSystemWindows = true
            val navRailWidth = 64.dp
            /* //custom padding to navRail when not constrained
            binding.contentView.layoutParams.width = screenWidthPx() - navRailWidth
            binding.contentView.setPadding(navRailWidth, 0, 0, 0)
            binding.contentView.setPadding(0, 28.dp, 0, 0)
            binding.contentView.translationX = navRailWidth.toFloat()*/
            if (screenWidthDp < LARGE_SCREEN_WIDTH_SIZE) {
                binding.navRail.layoutParams.width = navRailWidth
            }
        }/* else {
            // Large screen
            fab?.visibility = View.GONE
            navFab?.extend()
            binding.navToolBar.visibility = View.GONE
            binding.navRail.visibility = View.GONE
            binding.navDrawer?.visibility = View.VISIBLE
        }*/
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_gallery -> {
                FragmentActivity.start(this, TAG_TYPE.WalkThrough.name)
            }

            R.id.nav_search_pic -> {
                startActivity(
                    Intent(
                        this,
                        SaucenaoActivity::class.java
                    ).setAction("your.custom.action")
                )
            }

            R.id.nav_cache_clean -> {
                clean()
            }

            R.id.nav_settings -> {
                val intent =
                    Intent(this, SettingsActivity::class.java).setAction("your.custom.action")
                startActivity(intent)
            }

            R.id.nav_theme -> {
                FragmentActivity.start(this, "Theme")
            }

            R.id.nav_history -> {
                FragmentActivity.start(this, "History")
            }

            R.id.nav_cache_repo -> {
                FragmentActivity.start(this, "Cache")
            }

            R.id.nav_rename -> {
                startActivity(
                    Intent(
                        this,
                        ImgManagerActivity::class.java
                    ).setAction("ImgMgr.start")
                )
            }

            R.id.nav_progress -> {
                DownloadManagerActivity.start(this)
            }

            R.id.nav_account -> {
                FragmentActivity.start(this, "Account")
            }

            R.id.nav_blocklist -> {
                FragmentActivity.start(this, "Block")
            }
        }
        findViewById<DrawerLayout>(R.id.drawer_layout).close()
        //binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private var exitTime = 0L

    private lateinit var binding: AppMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppDataRepo.userInited().not()) {
            startActivity(
                Intent(
                    this@MainActivity,
                    LoginActivity::class.java
                ).setAction("login.try")
            )
            finish()
            return
        }
        val permissionList = ArrayList<String>()
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        checkAndRequestPermissions(permissionList)
        onBackPressedDispatcher.addCallback {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                return@addCallback
            }

            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toasty.warning(applicationContext, R.string.again_to_exit)
                exitTime = System.currentTimeMillis()
            } else {
                finish()
            }
        }
        binding = AppMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateNavigationViewLayout()
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.navView.setNavigationItemSelectedListener(this)
        //initView()
        binding.tablayout.setupWithViewPager(binding.contentView)
        binding.contentView.adapter = HelloMainViewPager(supportFragmentManager)

        binding.contentView.offscreenPageLimit =
            if (PxEZApp.instance.pre.getBoolean("refreshTab", false)) 0 else 3

        val position = PxEZApp.instance.pre.getString("firstpage", "0")?.toInt() ?: 0
        binding.tablayout.selectTab(binding.tablayout.getTabAt(position)!!)
        if (binding.navRail.isVisible) binding.tablayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.navRail.selectedItemId = binding.navRail.menu.getItem(tab.position).itemId
            }
        })
        initNavDrawerHeader(
            NavHeaderMainBinding.bind(binding.navView.getHeaderView(0)),
            AppDataRepo.currentUser
        )
        // reset icon after setupWithViewPager 清空所有tab
        for (i in 0..2) {
            val tabItem = binding.tablayout.getTabAt(i)!!
            tabItem.icon = when (i) {
                0 -> ContextCompat.getDrawable(this, R.drawable.ic_action_home)
                1 -> ContextCompat.getDrawable(this, R.drawable.ic_action_rank)
                else -> ContextCompat.getDrawable(this, R.drawable.ic_action_user)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S)
        if (PxEZApp.instance.pre.getBoolean("check_clipboard", false))
            this.window.decorView.post(Runnable {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                if (!clipboard.hasPrimaryClip()) return@Runnable
                val clipData = clipboard.primaryClip
                if (null != clipData && clipData.itemCount > 0) {
                    // for (item in 0 until clipData.itemCount){
                    //    val content = item.text.toString()
                    // }
                    // clipboard.addPrimaryClipChangedListener {
                    val text = clipData.getItemAt(0)?.text ?: return@Runnable
                    if (text.length > 200) return@Runnable //prevent issues 85
                    var item = Regex("""\d{7,9}""")
                        .find(text)
                        ?.value ?: Regex(getString(R.string.check_clipboard_policy))
                        .find(text)?.groupValues?.last()?.trim()
                    ?: return@Runnable

                    val pre = PxEZApp.instance.pre
                    if (item == pre.getString("lastclip2", "")) {
                        return@Runnable
                    }
                    MaterialDialogs(this).show {
                        setTitle(R.string.clipboard_detected)
                        setMessage(R.string.jumpto)
                        setInput {
                            editText!!.inputType = InputType.TYPE_CLASS_TEXT
                            editText!!.setText(item)

                        }
                        confirmButton { dialog, _ ->
                            item = getInputField(dialog).text.toString()
                            if (item.isBlank()) {
                                return@confirmButton
                            }
                            if ((item).toIntOrNull() != null) {
                                PictureActivity.start(this@MainActivity, item.toInt())
                            } else {
                                SearchResultActivity.start(this@MainActivity, item, 1)
                            }
                        }
                        cancelButton()
                        setOnDismissListener {
                            pre.edit { putString("lastclip2", item) }
                        }
                    }
                    // }
                }
            })
    }

    private fun checkAndRequestPermissions(permissionList: ArrayList<String>) {
        val list = ArrayList(permissionList)
        val it = list.iterator()

        while (it.hasNext()) {
            val permission = it.next()
            val hasPermission = ContextCompat.checkSelfPermission(this, permission)
            if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                it.remove()
            }
        }
        if (list.isEmpty()) {
            return
        }
        val permissions = list.toTypedArray()
        ActivityCompat.requestPermissions(this, permissions, 3000)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            3000 -> {
                val length = grantResults.size
                var reRequest = false
                for (i in 0 until length) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        reRequest = true
                    }
                }
                if (reRequest) {
                    Toasty.error(this, R.string.permission_denied)
                }
            }

            else -> {
            }
        }
    }

    private fun initNavDrawerHeader(header: NavHeaderMainBinding, user: UserEntity) {
        Glide.with(header.imageView.context)
            .load(user.userimage)
            .circleCrop().into(header.imageView)
        header.imageView.setOnClickListener {
            UserMActivity.start(this@MainActivity)
            binding.drawerLayout.close()
        }

        header.headtext.text = user.username
        header.textView.text = user.useremail
    }

    /* private inline fun initView() {
        ifPxEZApp.instance.pre.getBoolean("refreshTab", true))
            getFragmentContent(position).let {
                supportFragmentManager.beginTransaction().replace(R.id.binding.contentView, it).commit()
            }
        else if (savedInstanceState == null){ //https://blog.csdn.net/yuzhiqiang_1993/article/details/75014591
            binding.tablayoutHellom.getTabAt(position)!!.select()
            getFragmentContent(position).let {
                supportFragmentManager.beginTransaction().add(R.id.binding.contentView, it).commit()
                    curFragment = it
                    fragments[position] = it
                    //supportFragmentManager.fragments.forEach(){ it ->
                    //    supportFragmentManager.beginTransaction().remove(it).commit()
                    //}
            }
        }
        else {
            curFragment = supportFragmentManager.findFragmentById(R.id.binding.contentView)
        }
    }*/

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_main -> {
                binding.tablayout.selectTab(binding.tablayout.getTabAt(0)!!)
                true
            }

            R.id.nav_trend -> {
                binding.tablayout.selectTab(binding.tablayout.getTabAt(1)!!)
                true
            }

            R.id.nav_follow -> {
                binding.tablayout.selectTab(binding.tablayout.getTabAt(2)!!)
                true
            }

            R.id.action_search -> {
                if (binding.navRail.isVisible) //action_search selection in navRail should be reset
                    binding.navRail.postDelayed(500) { //after onOptionsItemSelected
                        binding.navRail.selectedItemId =
                            binding.navRail.menu.getItem(binding.tablayout.selectedTabPosition).itemId
                    }
                SearchActivity.start(this)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clean() {
        MaterialDialogs(this).show {
            setMessage(getString(R.string.cache_clear_message))
            confirmButton { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    Glide.get(applicationContext).clearDiskCache()
                    deleteDir(applicationContext.cacheDir)
                    if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                        deleteDir(applicationContext.externalCacheDir)
                    }
                }
            }
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children!!.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        return dir!!.delete()
    }
}
