package com.perol.asdpl.pixivez.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.RinkActivity
import com.perol.asdpl.pixivez.core.PicListFragment
import com.perol.asdpl.pixivez.core.SelectDownloadFragment
import com.perol.asdpl.pixivez.core.TAG_TYPE
import com.perol.asdpl.pixivez.databinding.ActivityFragmentHostBinding
import com.perol.asdpl.pixivez.ui.account.AccountFragment
import com.perol.asdpl.pixivez.ui.pic.ZoomFragment
import com.perol.asdpl.pixivez.ui.settings.BlockTagFragment
import com.perol.asdpl.pixivez.ui.settings.HistoryFragment
import com.perol.asdpl.pixivez.ui.settings.ThemeFragment
import com.perol.asdpl.pixivez.ui.user.UserRelatedListFragment
import kotlinx.parcelize.Parcelize
import kotlin.reflect.KFunction0

class FragmentActivity : RinkActivity() {
    @Parcelize
    class FragmentItem(
        val factory: KFunction0<Fragment>?,
        val title: Int?,
    ) : Parcelable

    companion object {
        private val fragments = mapOf(
            "Account" to FragmentItem(::AccountFragment, R.string.account_management),
            "Block" to FragmentItem(::BlockTagFragment, R.string.block_tag),
            "Theme" to FragmentItem(::ThemeFragment, R.string.theme),
            "Users" to FragmentItem(::UserRelatedListFragment, null),
            "History" to FragmentItem(::HistoryFragment, R.string.view_history),
            TAG_TYPE.Collect.name to FragmentItem(::SelectDownloadFragment, R.string.download),
            "Zoom" to FragmentItem(::ZoomFragment, null),
            TAG_TYPE.WalkThrough.name to FragmentItem(null, R.string.walk_through)
        )

        fun start(
            context: Context,
            target: String,
            title: String? = null,
            arguments: Bundle? = null
        ) {
            val intent = Intent(context, FragmentActivity::class.java)
            intent.putExtra("target", target)
            intent.putExtra("args", arguments)
            intent.putExtra("title", title)
            context.startActivity(intent)
        }
    }

    //private val target: FragmentItem by argument()

    private lateinit var binding: ActivityFragmentHostBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFragmentHostBinding.inflate(layoutInflater)
        val targetTag = intent.extras!!.getString("target")!!
        val target = fragments[targetTag]!!
        (intent.extras!!.getString("title")?.toIntOrNull() ?: target.title)?.let {
            binding.toolbar.title = getString(it)
        }
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState != null) {
            //TODO: check if need getFragment
            val oldFragment = supportFragmentManager.findFragmentByTag(targetTag)
            if (oldFragment != null)
                return
        }
        val targetFragment: Fragment = target.factory?.let {
            it().apply {
                arguments = intent.extras!!.getBundle("args")
            }
        } ?: PicListFragment.newInstance(targetTag)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host, targetFragment, targetTag).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    fun hideAppBar() {
        binding.appBarLayout.visibility = View.GONE
    }
}
