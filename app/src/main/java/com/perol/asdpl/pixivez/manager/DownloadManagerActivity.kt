package com.perol.asdpl.pixivez.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivityDownloadManagerBinding
class DownloadManagerActivity : RinkActivity() {



private lateinit var binding: ActivityDownloadManagerBinding
	override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
		binding = ActivityDownloadManagerBinding.inflate(layoutInflater)
		setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_body, DownLoadManagerFragment.newInstance()).commitNow()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DownloadManagerActivity::class.java))
        }
    }
}