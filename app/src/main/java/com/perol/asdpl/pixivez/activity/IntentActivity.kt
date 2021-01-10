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

package com.perol.asdpl.pixivez.activity

import android.content.Intent
import android.os.Bundle
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.objects.Toasty

class IntentActivity : RinkActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_intent);
        val uri = intent.data
        if (uri != null) {
            val scheme = uri.scheme
            val segment = uri.pathSegments
            //pixiv://illusts/
            if (scheme != null){
                if (scheme.contains("pixiv")) {
                    val host = uri.host
                    if (!host.isNullOrBlank()) {
                        if (host.contains("users")) {
                            try {
                                UserMActivity.start(this, segment[0].toLong())
                                finish()
                            } catch (e: Exception) {
                                Toasty.error(this, getString(R.string.wrong_id))
                            }
                        }
                        if (host.contains("illusts")) {
                                try {
                                    openIllust(segment[0].toLong())
                                    finish()
                                    return
                                } catch (e: Exception) {
                                    Toasty.error(this, getString(R.string.wrong_id))
                                }
                        }
                    }
                }
            }
            if (uri.path?.contains("artworks") == true) {
                val id = segment[segment.size - 1].toLong()
                try {
                    openIllust(id)
                    finish()
                    return
                } catch (e: Exception) {
                    Toasty.error(this, getString(R.string.wrong_id))
                }
                return
            }
            //en/user/xxxx
            if (segment.size == 2 || segment.size == 3) {
                if (segment[segment.size - 2] == "users") {
                    val s = segment[segment.size - 1]
                    val id = s.toLong()
                    try {
                        val intent1 = Intent(this, UserMActivity::class.java)
                        intent1.putExtra("data", id)
                        startActivity(intent1)
                        finish()
                        return
                    } catch (e: Exception) {
                        Toasty.error(this, getString(R.string.wrong_id))
                    }
                }
                if (segment[segment.size - 2] == "u") {
                    val id = segment[segment.size - 1].toLong()
                    try {
                        val intent1 = Intent(this, UserMActivity::class.java)
                        intent1.putExtra("data", id)
                        startActivity(intent1)
                        finish()
                        return
                    } catch (e: Exception) {
                        Toasty.error(this, getString(R.string.wrong_id))
                    }
                }
                if (segment[segment.size - 2] == "i") {
                    val id = segment[segment.size - 1].toLong()
                    try {
                        openIllust(id)
                        finish()
                        return
                    } catch (e: Exception) {
                        Toasty.error(this, getString(R.string.wrong_id))
                    }
                }
            }
            uri.getQueryParameter("illust_id")?.let {
                try {
                    openIllust(it.toLong())
                    finish()
                    return
                } catch (e: Exception) {
                    Toasty.error(this, getString(R.string.wrong_id))
                }
            }
            uri.getQueryParameter("id")?.let {
                try {
                    UserMActivity.start(this, it.toLong())
                    finish()
                    return
                } catch (e: Exception) {
                    Toasty.error(this, getString(R.string.wrong_id))
                }
            }
            if (uri.encodedSchemeSpecificPart.contains("/fanbox/creator/")) {
                val index = uri.pathSegments.indexOf("creator") + 1
                val targetString = uri.pathSegments[index]
                targetString.toLongOrNull()?.let {
                    try {
                        UserMActivity.start(this, it)
                        finish()
                    } catch (e: Exception) {
                        Toasty.error(this, getString(R.string.wrong_id))
                    }
                }

            }


        }


    }

    private fun openIllust(id: Long) {
        val bundle = Bundle()
        val arrayList = longArrayOf(id)
        bundle.putLongArray("illustidlist", arrayList)
        bundle.putLong("illustid", id)
        val intent2 = Intent(this, PictureActivity::class.java)
        intent2.putExtras(bundle)
        startActivity(intent2)
    }


}
