/*
 * MIT License
 *
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
package com.perol.asdpl.pixivez.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * tag : 少女
 * illust : {"id":57808781,"title":"芜！","type":"illust","image_urls":{"square_medium":"https://i.pximg.net/c/360x360_70/img-master/img/2016/07/09/00/15/53/57808781_p0_square1200.jpg","medium":"https://i.pximg.net/c/540x540_70/img-master/img/2016/07/09/00/15/53/57808781_p0_master1200.jpg","large":"https://i.pximg.net/c/600x1200_90/img-master/img/2016/07/09/00/15/53/57808781_p0_master1200.jpg"},"caption":"！","restrict":0,"user":{"id":9580845,"name":"Magicians","account":"zhkahogigzkh","profile_image_urls":{"medium":"https://i1.pixiv.net/user-profile/img/2016/04/08/23/55/16/10781688_5e77417c9e9871ef7edf10dc2cda76dc_170.png"},"is_followed":false},"tags":[{"name":"少女"},{"name":"女子"},{"name":"刀"},{"name":"女の子"},{"name":"オリジナル"}],"tools":["Photoshop","SAI"],"create_date":"2016-07-09T00:15:53+09:00","page_count":1,"width":1311,"height":784,"sanity_level":2,"meta_single_page":{"original_image_url":"https://i2.pixiv.net/img-original/img/2016/07/09/00/15/53/57808781_p0.jpg"},"meta_pages":[],"total_view":11951,"total_bookmarks":904,"is_bookmarked":false,"visible":true}
 */
@Serializable
class TrendingtagResponse(
    @SerialName("trend_tags")
    val trend_tags: MutableList<TrendTagsBean>
)

@Serializable
class TrendTagsBean(
    val tag: String,
    @SerialName("translated_name")
    val translated_name: String?,
    val illust: Illust,
)
