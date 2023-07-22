package com.perol.asdpl.pixivez.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.databinding.ActivityFragmentHostBinding
import com.perol.asdpl.pixivez.objects.argument
import com.perol.asdpl.pixivez.ui.account.AccountFragment
import com.perol.asdpl.pixivez.ui.settings.BlockTagFragment
import com.perol.asdpl.pixivez.ui.settings.HistoryFragment
import com.perol.asdpl.pixivez.ui.settings.ThemeFragment
import kotlinx.parcelize.Parcelize
import kotlin.reflect.KFunction0

class FragmentActivity : RinkActivity() {
    @Parcelize
    class FragmentItem(
        val factory: KFunction0<Fragment>,
        val title: Int?,
    ) : Parcelable

    companion object {
        private val fragments = mapOf(
            "Account" to FragmentItem(::AccountFragment, R.string.account_management),
            "Block" to FragmentItem(::BlockTagFragment, R.string.block_tag),
            "Theme" to FragmentItem(::ThemeFragment, R.string.theme),
            //"Users" to FragmentItem(::UserListFragment, null),
            "History" to FragmentItem(::HistoryFragment, R.string.view_history),
        )

        fun start(context: Context, target: String) {
            val intent = Intent(context, FragmentActivity::class.java)
            intent.putExtra("target", target)
            context.startActivity(intent)
        }
    }

    private val target: FragmentItem by argument()

    private lateinit var binding: ActivityFragmentHostBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFragmentHostBinding.inflate(layoutInflater)
        val targetTag = intent.extras!!.getString("target")
        val target = fragments[targetTag]!!
        target.title?.let { binding.toolbar.title = getString(it) }
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction().replace(R.id.nav_host, target.factory()).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
