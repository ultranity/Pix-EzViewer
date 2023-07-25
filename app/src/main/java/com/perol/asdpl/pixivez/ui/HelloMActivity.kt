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
import android.app.ActivityOptions
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Pair
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.data.entity.UserEntity
import com.perol.asdpl.pixivez.databinding.NavHeaderMainBinding
import com.perol.asdpl.pixivez.manager.DownloadManagerActivity
import com.perol.asdpl.pixivez.manager.ImgManagerActivity
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.account.LoginActivity
import com.perol.asdpl.pixivez.ui.home.ActivityHelloMBinding
import com.perol.asdpl.pixivez.ui.home.HelloMainViewPager
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

class HelloMActivity : RinkActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_gallery -> {
                startActivity(Intent(this, SaucenaoActivity::class.java))
            }

            R.id.nav_cache_clean -> {
                clean()
            }

            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

            R.id.nav_theme -> {
                FragmentActivity.start(this, "Theme")
            }

            R.id.nav_history -> {
                FragmentActivity.start(this, "History")
            }

            R.id.nav_rename -> {
                startActivity(Intent(this, ImgManagerActivity::class.java))
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(
                applicationContext,
                getString(R.string.again_to_exit),
                Toast.LENGTH_SHORT
            ).show()
            exitTime = System.currentTimeMillis()
        } else {
            finish()
        }
    }

    private lateinit var binding: ActivityHelloMBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppDataRepo.userInited().not()) {
            startActivity(Intent(this@HelloMActivity, LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityHelloMBinding.inflate(
            layoutInflater,
            if (PxEZApp.instance.pre.getBoolean("bottomAppbar", true))
                R.layout.app_main_bottombar else
                R.layout.activity_hello_m,
        )
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.menu,
            R.string.menu
        )
        toggle.setHomeAsUpIndicator(R.drawable.ic_action_logo)
        toggle.isDrawerIndicatorEnabled = true
        toggle.setToolbarNavigationClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)
        val permissionList = ArrayList<String>()
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        checkAndRequestPermissions(permissionList)

        //initView()
        binding.tablayout.setupWithViewPager(binding.contentView)
        binding.contentView.adapter = HelloMainViewPager(supportFragmentManager)

        binding.contentView.offscreenPageLimit =
            if (PxEZApp.instance.pre.getBoolean("refreshTab", false)) 0 else 3

        val position = PxEZApp.instance.pre.getString("firstpage", "0")?.toInt() ?: 0
        binding.tablayout.selectTab(binding.tablayout.getTabAt(position)!!)

        initNavDrawer(
            NavHeaderMainBinding.bind(binding.navView.getHeaderView(0)),
            AppDataRepo.currentUser
        )

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
        this.window.decorView.post(
            Runnable {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                if (!clipboard.hasPrimaryClip()) {
                    return@Runnable
                }
                val clipData = clipboard.primaryClip
                if (null != clipData && clipData.itemCount > 0) {
                    // for (item in 0 until clipData.itemCount){
                    //    val content = item.text.toString()
                    // }
                    // clipboard.addPrimaryClipChangedListener {
                    val text = clipData.getItemAt(0)?.text ?: return@Runnable
                    var item = Regex("""\d{7,9}""")
                        .find(text)
                        ?.value ?: Regex("""((画师)|(artist)|(by)|(twi(tter)?))([：:\s]*)(\S+)""")
                        .find(text)?.groupValues?.last()?.trim()
                    ?: return@Runnable

                    val pre = PxEZApp.instance.pre
                    if (item == pre.getString("lastclip2", "")) {
                        return@Runnable
                    }
                    MaterialDialog(this).show {
                        title(R.string.clipboard_detected)
                        message(R.string.jumpto)
                        input(prefill = item, inputType = InputType.TYPE_CLASS_TEXT)
                        positiveButton(android.R.string.ok) {
                            item = getInputField().text.toString()
                            pre.edit().putString("lastclip2", item).apply()
                            if ((item).toLongOrNull() != null) {
                                PictureActivity.start(this@HelloMActivity, item.toLong())
                            } else {
                                SearchResultActivity.start(this@HelloMActivity, item, 1)
                            }
                        }
                        neutralButton(R.string.not_this_one) {
                            pre.edit().putString("lastclip2", item).apply()
                        }
                        negativeButton(android.R.string.cancel)
                    }

                    // }
                }
            }
        )
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
        if (list.size == 0) {
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
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT)
                        .show()
                }
            }

            else -> {
            }
        }
    }

    private fun initNavDrawer(header: NavHeaderMainBinding, user: UserEntity) {
        Glide.with(header.imageView.context)
            .load(user.userimage)
            .circleCrop().into(header.imageView)
        header.imageView.setOnClickListener {
            val options = if (PxEZApp.animationEnable) {
                ActivityOptions.makeSceneTransitionAnimation(
                    this@HelloMActivity,
                    Pair(header.imageView, "userimage")
                ).toBundle()
            } else null
            UserMActivity.start(this@HelloMActivity, AppDataRepo.currentUser, options)
            binding.drawerLayout.close()
        }

        header.headtext.text = user.username
        header.textView.text = user.useremail
    }

    private inline fun initView() {
        /*if (PxEZApp.instance.pre.getBoolean("refreshTab", true))
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
        }*/
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clean() {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.cache_clear_message))
            .setPositiveButton(
                getString(R.string.ok)
            ) { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    Glide.get(applicationContext).clearDiskCache()
                    deleteDir(applicationContext.cacheDir)
                    if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                        deleteDir(applicationContext.externalCacheDir)
                    }
                }
            }.show()
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
