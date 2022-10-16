/*
 * MIT License
 *
 * Copyright (c) 2022 ultranity
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

package com.perol.asdpl.pixivez.responses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class IllustNext(
    val illusts: List<Illust>,
    val next_url: String
)

/**
 * id : 66137839
 * title : 97式
 * type : illust
 * image_urls : {"square_medium":"https://i.pximg.net/c/360x360_70/img-master/img/2017/12/03/05/15/02/66137839_p0_square1200.jpg","medium":"https://i.pximg.net/c/540x540_70/img-master/img/2017/12/03/05/15/02/66137839_p0_master1200.jpg","large":"https://i.pximg.net/c/600x1200_90/img-master/img/2017/12/03/05/15/02/66137839_p0_master1200.jpg"}
 * caption : (•ω•)
 * restrict : 0
 * user : {"id":19887389,"name":"kkrin1013","account":"kkrin1013","profile_image_urls":{"medium":"https://i.pximg.net/user-profile/img/2017/12/04/10/46/10/13525660_fc11c3a777f794271125c1f7ab043168_170.png"},"is_followed":false}
 * tags : [{"name":"97式"},{"name":"少女前线"},{"name":"少女前線"},{"name":"소녀전선"}]
 * tools : ["CLIP STUDIO PAINT"]
 * create_date : 2017-12-03T05:15:02+09:00
 * page_count : 1
 * width : 1800
 * height : 2158
 * sanity_level : 4
 * series : null
 * meta_single_page : {"original_image_url":"https://i.pximg.net/img-original/img/2017/12/03/05/15/02/66137839_p0.png"}
 * meta_pages : []
 * total_view : 1916
 * total_bookmarks : 399
 * is_bookmarked : false
 * visible : true
 * is_muted : false
 */
@Parcelize
data class Illust(
    val caption: String,
    val create_date: String,
    val height: Int,
    val id: Long,
    val image_urls: ImageUrls,
    var is_bookmarked: Boolean,
    val is_muted: Boolean,
    val meta_pages: List<MetaPage>,
    val meta_single_page: MetaSinglePage,
    val page_count: Int,
    val restrict: Int,
    val sanity_level: Int,
    val tags: List<Tag>,
    val title: String,
    val tools: List<String>,
    val total_bookmarks: Int,
    val total_view: Int,
    //val total_comments: Int,
    val type: String,
    val user: User,
    var visible: Boolean,
    val width: Int,
    val x_restrict: Int
) : Parcelable

@Parcelize
data class MetaPage(
        val image_urls: ImageUrlsX
) : Parcelable

/**
 * square_medium : https://i.pximg.net/c/360x360_70/img-master/img/2017/12/03/05/15/02/66137839_p0_square1200.jpg
 * medium : https://i.pximg.net/c/540x540_70/img-master/img/2017/12/03/05/15/02/66137839_p0_master1200.jpg
 * large : https://i.pximg.net/c/600x1200_90/img-master/img/2017/12/03/05/15/02/66137839_p0_master1200.jpg
 */
@Parcelize
data class ImageUrlsX(
    val large: String,
    val medium: String,
    val original: String,
    val square_medium: String
) : Parcelable

@Parcelize
data class ImageUrls(
    val large: String,
    val medium: String,
    val square_medium: String
) : Parcelable


/**
 * original_image_url : https://i.pximg.net/img-original/img/2017/12/03/05/15/02/66137839_p0.png
 */
@Parcelize
data class MetaSinglePage(
    val original_image_url: String?
) : Parcelable

/**
 * name : 97式
 */
@Parcelize
data class Tag(
    val name: String,
    val translated_name: String? = null
) : Parcelable
