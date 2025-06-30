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

package com.perol.asdpl.pixivez.ui.settings

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs

class ThanksFragment : PreferenceFragmentCompat() {
    private class GithubUser(
        val Preference: String,
        val AvatarUrl: String,
        val Placeholder: Int,
        val GithubProfile: String,
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pre_thanks)
        findPreference<PreferenceCategory>("huonaicai")?.isVisible = BuildConfig.FLAVOR != "play"

//        findPreference<Preference>("xuemo")?.apply {
//            loadDrawableByUrl(
//                "https://avatars.githubusercontent.com/LuckXuemo",
//                R.drawable.xuemo,
//                onLoadCleared = { it?.let { icon = it } },
//                onResourceReady = { icon = it }
//            )
//            onPreferenceClickListener = Preference.OnPreferenceClickListener {
//                startActivityByUri("https://github.com/LuckXuemo")
//                true
//            }
//        }
        listOf(
            GithubUser(
                "xuemo",
                "https://avatars.githubusercontent.com/LuckXuemo",
                R.drawable.xuemo,
                "https://github.com/LuckXuemo"
            ),
            GithubUser(
                "ultranity",
                "https://avatars.githubusercontent.com/ultranity",
                R.drawable.ultranity,
                "https://github.com/ultranity"
            ),
            GithubUser(
                "hunterx9",
                "https://avatars.githubusercontent.com/hunterx9",
                R.drawable.hunterx9,
                "https://github.com/hunterx9"
            ),
            GithubUser(
                "Skimige",
                "https://avatars.githubusercontent.com/Skimige",
                R.drawable.skimige,
                "https://github.com/Skimige"
            ),
            GithubUser(
                "TragicLife",
                "https://avatars.githubusercontent.com/TragicLifeHu",
                R.drawable.tragiclife,
                "https://github.com/TragicLifeHu"
            ),
            GithubUser(
                "Misoni",
                "https://avatars.githubusercontent.com/MISONLN41",
                R.drawable.misoni,
                "https://github.com/MISONLN41"
            ),
        ).forEach {
            (findPreference<Preference>(it.Preference))?.apply {
                loadDrawableByUrl(
                    it.AvatarUrl,
                    it.Placeholder,
                    onLoadCleared = { d -> d?.let { icon = d } },
                    onResourceReady = { d -> icon = d })
                setOnPreferenceClickListener { _ ->
                    startActivityByUri(it.GithubProfile)
                    true
                }
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            // "support" -> startActivityByUri("https://play.google.com/store/apps/details?id=com.perol.asdpl.play.pixivez")
            /*  "pr" -> startActivityByUri("https://github.com/ultranity/Pix-EzViewer/pulls")*/
            "thanks" -> {
                val thanksDialog = ThanksDialog()
                thanksDialog.show(childFragmentManager)
            }

            "support" -> SupportDialog().show(parentFragmentManager, "supportdialog")
            "Ultranity" -> SupportDialog().show(parentFragmentManager, "supportdialog")
            "Notsfsssf" -> {
                val view =
                    requireActivity().layoutInflater.inflate(R.layout.dialog_weixin_notsfsssf, null)
                MaterialDialogs(requireActivity()).show {
                    setView(view)
                    confirmButton()
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun startActivityByUri(uri: String) {
        Intent(Intent.ACTION_VIEW).apply {
            data = uri.toUri()
        }.also {
            it.resolveActivity(requireContext().packageManager)?.run {
                startActivity(it)
            }
        }
    }

    private fun loadDrawableByUrl(
        url: String,
        @DrawableRes placeholder: Int,
        onLoadCleared: (Drawable?) -> Unit,
        onResourceReady: (Drawable) -> Unit
    ) {
        Glide.with(this@ThanksFragment)
            .load(url)
            .apply(RequestOptions.bitmapTransform(CircleCrop()))
            .placeholder(placeholder)
            .into(object : CustomTarget<Drawable>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                    onLoadCleared(placeholder)
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    onResourceReady(resource)
                }
            })
    }
}
