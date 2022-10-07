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

import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.databinding.ViewRankingItemSBinding
import com.perol.asdpl.pixivez.databinding.ViewRecommandItemSBinding
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works

// basic Adapter for image item
//TODO: reuse more code

//fun <T:ViewBinding> convert(helper: BaseBindingAdapter.BaseVBViewHolder<T>, item: Illust) {}

abstract class PicItemXUserAdapter(
    layoutResId: Int,
    data: List<Illust>?
) :
    PicItemAdapterBaseBinding<ViewRankingItemSBinding>(layoutResId,ViewRankingItemSBinding::class, data?.toMutableList()) {

    override fun convert(helper: BaseVBViewHolder<ViewRankingItemSBinding>, item: Illust) {
        super.convert(helper, item)
        if (PxEZApp.CollectMode == 1) {
            helper.bd.imageviewLike.apply {
                setOnClickListener {
                    setBorderColor(colorPrimaryDark)
                    Works.imageDownloadAll(item)
                    if (!item.is_bookmarked) {
                        retrofitRepository.postLikeIllustWithTags(item.id, x_restrict(item), null)
                            .subscribe({
                                setImageResource(R.drawable.heart_red)
                                alpha = 0.9F
                                item.is_bookmarked = true
                            }, {}, {})
                    }
                }
            }
        } else {
            helper.bd.imageviewLike.apply {
                setOnClickListener {
                    if (item.is_bookmarked) {
                        retrofitRepository.postUnlikeIllust(item.id).subscribe({
                            //GlideApp.with(this).load(R.drawable.ic_action_heart).into(binding.fab)
                            setImageResource(R.drawable.ic_action_heart)
                            alpha = 0.5F
                            item.is_bookmarked = false
                        }, {}, {})
                    } else {
                        retrofitRepository.postLikeIllustWithTags(item.id, x_restrict(item), null)
                            .subscribe({
                                setImageResource(R.drawable.heart_red)
                                alpha = 0.9F
                                item.is_bookmarked = true
                            }, {}, {})
                    }
                }
                setOnLongClickListener {
                    setBorderColor(colorPrimaryDark)
                    Works.imageDownloadAll(item)
                    true
                }
            }
        }
        helper.bd.imageviewLike.apply {

            if (item.is_bookmarked) {
                setImageResource(R.drawable.heart_red)
                alpha = 0.9F
            } else {
                setImageResource(R.drawable.ic_action_heart)
                alpha = 0.5F
            }

            if (FileUtil.isDownloaded(item)) {
                setBorderColor(badgeTextColor)
            } else {
                setBorderColor(colorTransparent)
            }
        }

    }
}

abstract class PicItemXAdapter(
    layoutResId: Int,
    data: List<Illust>?
) :
    PicItemAdapterBaseBinding<ViewRecommandItemSBinding>(layoutResId,ViewRecommandItemSBinding::class, data?.toMutableList()) {

    override fun convert(helper: BaseVBViewHolder<ViewRecommandItemSBinding>, item: Illust) {
        super.convert(helper, item)
        if (PxEZApp.CollectMode == 1) {
            helper.bd.imageviewLike.apply {
                setOnClickListener {
                    setBorderColor(colorPrimaryDark)
                    Works.imageDownloadAll(item)
                    if (!item.is_bookmarked) {
                        retrofitRepository.postLikeIllustWithTags(item.id, x_restrict(item), null)
                            .subscribe({
                                setImageResource(R.drawable.heart_red)
                                alpha = 0.9F
                                item.is_bookmarked = true
                            }, {}, {})
                    }
                }
            }
        } else {
            helper.bd.imageviewLike.apply {
                setOnClickListener {
                    if (item.is_bookmarked) {
                        retrofitRepository.postUnlikeIllust(item.id).subscribe({
                            //GlideApp.with(this).load(R.drawable.ic_action_heart).into(binding.fab)
                            setImageResource(R.drawable.ic_action_heart)
                            alpha = 0.5F
                            item.is_bookmarked = false
                        }, {}, {})
                    } else {
                        retrofitRepository.postLikeIllustWithTags(item.id, x_restrict(item), null)
                            .subscribe({
                                setImageResource(R.drawable.heart_red)
                                alpha = 0.9F
                                item.is_bookmarked = true
                            }, {}, {})
                    }
                }
                setOnLongClickListener {
                    setBorderColor(colorPrimaryDark)
                    Works.imageDownloadAll(item)
                    true
                }
            }
        }
        helper.bd.imageviewLike.apply {

            if (item.is_bookmarked) {
                setImageResource(R.drawable.heart_red)
                alpha = 0.9F
            } else {
                setImageResource(R.drawable.ic_action_heart)
                alpha = 0.5F
            }

            if (FileUtil.isDownloaded(item)) {
                setBorderColor(badgeTextColor)
            } else {
                setBorderColor(colorTransparent)
            }
        }

    }
}
