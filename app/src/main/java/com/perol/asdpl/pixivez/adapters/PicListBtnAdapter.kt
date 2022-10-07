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

package com.perol.asdpl.pixivez.adapters

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.PictureActivity
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min


// simple Adapter for image item, without user imageView
//TODO: rename
class PicListBtnAdapter(
    layoutResId: Int,
    data: List<Illust>?,
    override var R18on: Boolean,
    override var blockTags: List<String>,
    override var hideBookmarked: Int = 0,
    override var hideDownloaded: Boolean = false,
    override var sortCoM: Int = 0
) :
    PicItemAdapter(layoutResId, data?.toMutableList()), LoadMoreModule {

    init {
        if (PxEZApp.CollectMode == 2) {
            setOnItemClickListener { adapter, view, position ->
                (this.data as ArrayList<Illust?>)[position]?.let {
                    val item = it
                    view.findViewById<MaterialButton>(R.id.save).setTextColor(colorPrimaryDark)
                    Works.imageDownloadAll(item)
                    if (!item.is_bookmarked) {
                        retrofitRepository.postLikeIllustWithTags(item.id, x_restrict(item), null)
                            .subscribe({
                                view.findViewById<MaterialButton>(R.id.like).setTextColor(badgeTextColor)
                                item.is_bookmarked = true
                            }, {}, {})
                    }
                    if (!item.user.is_followed) {
                        retrofitRepository.postfollowUser(item.user.id, x_restrict(item)).subscribe({
                            item.user.is_followed = true
                        }, {}, {})
                    }
                }
            }
            setOnItemLongClickListener { adapter, view, position ->
                val bundle = Bundle()
                DataHolder.setIllustsList(this.data.subList(max(position-30,0), min(this.data.size,max(position-30,0)+60)))
                bundle.putInt("position",position - max(position-30,0))
                bundle.putLong("illustid", this.data[position].id)
                val intent = Intent(context, PictureActivity::class.java)
                intent.putExtras(bundle)
                if (PxEZApp.animationEnable) {
                    val mainimage = view.findViewById<View>(R.id.item_img)
                    val title = view.findViewById<View>(R.id.title)
                    val options = ActivityOptions.makeSceneTransitionAnimation(
                        context as Activity,
                        Pair.create(
                            mainimage,
                            "mainimage"
                        ),
                        Pair.create(title, "title")
                    )
                    ContextCompat.startActivity(context, intent, options.toBundle())
                } else
                    ContextCompat.startActivity(context, intent, null)
                true
            }
        } else {
            setOnItemClickListener { adapter, view, position ->
                val bundle = Bundle()
                //bundle.putLong("illustid", this@RecommendAdapter.data[position].id)
                //val illustlist = LongArray(this.data.count())
                //for (i in this.data.indices) {
                //    illustlist[i] = this.data[i].id
                //}
                //bundle.putLongArray("illustidlist", illustlist)
                DataHolder.setIllustsList(this.data.subList(max(position-30,0), min(this.data.size,max(position-30,0)+60)))
                bundle.putInt("position",position - max(position-30,0))
                bundle.putLong("illustid", this.data[position].id)
                val intent = Intent(context, PictureActivity::class.java)
                intent.putExtras(bundle)
                if (PxEZApp.animationEnable) {
                    val mainimage = view.findViewById<View>(R.id.item_img)
                    val title = view.findViewById<View>(R.id.title)
                    val options = ActivityOptions.makeSceneTransitionAnimation(
                        context as Activity,
                        Pair.create(
                            mainimage,
                            "mainimage"
                        ),
                        Pair.create(title, "title")
                    )
                    ContextCompat.startActivity(context, intent, options.toBundle())
                } else
                    ContextCompat.startActivity(context, intent, null)
            }
            setOnItemLongClickListener { adapter, view, position ->
                //show detail of illust
                (adapter.data as ArrayList<Illust?>)[position]?.let {
                    val detailstring =
                        "id: " + it.id.toString() +
                                "caption: " + it.caption + "create_date: " + it.create_date +
                                "width: " + it.width.toString() + "height: " + it.height.toString() +
                                //+ "image_urls: " + illust.image_urls.toString() + "is_bookmarked: " + illust.is_bookmarked.toString() +
                                "user: " + it.user.name +
                                "tags: " + it.tags.toString() +// "title: " + illust.title.toString() +
                                "total_bookmarks: " + it.total_bookmarks.toString() +
                                "total_view: " + it.total_view.toString() +
                                "user account: " + it.user.account + "\n" +
                                "tools: " + it.tools.toString() + "\n" +
                                "type: " + it.type + "\n" +
                                "page_count: " + it.page_count.toString() + "\n" +
                                "visible: " + it.visible.toString() + "\n" +
                                "is_muted: " + it.is_muted.toString() + "\n" +
                                "sanity_level: " + it.sanity_level.toString() + "\n" +
                                "restrict: " + it.restrict.toString() + "\n" +
                                "x_restrict: " + it.x_restrict.toString()
                    MaterialAlertDialogBuilder(context as Activity)
                        .setMessage(detailstring)
                        .setTitle("Detail")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                        }
                        .create().show()
                }
                true
            }
        }
    }

}
