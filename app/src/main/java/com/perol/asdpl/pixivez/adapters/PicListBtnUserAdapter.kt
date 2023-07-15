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

import android.os.Bundle
import android.view.View
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.objects.IllustFilter
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.ui.NiceImageView

/**
 *  simple Adapter for image item with user imageView and save/like button
 */
class PicListBtnUserAdapter(
    layoutResId: Int,
    data: List<Illust>?,
    filter: IllustFilter
) :
    PicListBtnAdapter(layoutResId, data, filter), LoadMoreModule {

    override fun viewPicsOptions(view: View, illust: Illust): Bundle {
        return PicListXUserAdapter.viewOptions(this, view, illust)
    }

    override fun setUIFollow(status: Boolean, position: Int) {
        (getViewByAdapterPosition(
            position, R.id.imageview_user
        ) as NiceImageView?)?.let { PicListXUserAdapter.badgeUIFollow(this, status, it) }
    }

    override fun setUIFollow(status: Boolean, view: View) {
        val user = view as NiceImageView
        PicListXUserAdapter.badgeUIFollow(this, status, user)
    }


    override fun convert(holder: BaseViewHolder, item: Illust) {
        super.convert(holder, item)
        PicListXUserAdapter.convertUser(this, holder, item)
    }
}
