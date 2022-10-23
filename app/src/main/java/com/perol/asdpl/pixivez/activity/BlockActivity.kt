package com.perol.asdpl.pixivez.activity

import android.os.Bundle
import android.view.MenuItem
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.databinding.ActivityBlockBinding
import com.perol.asdpl.pixivez.fragments.BlockTagFragment

class BlockActivity : RinkActivity() {

    private lateinit var binding: ActivityBlockBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, BlockTagFragment.newInstance("", "")).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
