package com.perol.asdpl.pixivez.manager

import android.content.res.Configuration
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.arialyy.aria.core.Aria
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.RinkActivity
import com.perol.asdpl.pixivez.databinding.SettingsActivityBinding
class ManagerSettingsActivity : RinkActivity() {

private lateinit var binding: SettingsActivityBinding
	override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
		binding = SettingsActivityBinding.inflate(layoutInflater)
		setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }

    }


    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            findPreference<ListPreference>("max_task_num")!!.setOnPreferenceChangeListener { preference, newValue ->
                Aria.get(requireActivity()).downloadConfig.apply {
                    maxTaskNum = (newValue as String).toInt()
                }
                true
            }
            findPreference<ListPreference>("thread_num")!!.setOnPreferenceChangeListener { preference, newValue ->
                Aria.get(requireActivity()).downloadConfig.apply {
                    threadNum  = (newValue as String).toInt()
                }
                true
            }
        }

        override fun onConfigurationChanged(newConfig: Configuration) {
            super.onConfigurationChanged(newConfig)
        }
    }
}