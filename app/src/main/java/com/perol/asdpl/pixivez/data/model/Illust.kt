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

package com.perol.asdpl.pixivez.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface IIllustNext {
    val illusts: MutableList<Illust>
    val next_url: String?
}

data class IllustNext(
    override val illusts: MutableList<Illust>,
    override val next_url: String?
) : IIllustNext

/* "id": 102414087,
 * "title": "花に少女",
 * "type": "illust",
 * "image_urls": {
 *     "square_medium": "https://i.pximg.net/c/360x360_70/img-master/img/2022/11/01/04/58/36/102414087_p0_square1200.jpg",
 *     "medium": "https://i.pximg.net/c/540x540_70/img-master/img/2022/11/01/04/58/36/102414087_p0_master1200.jpg",
 *     "large": "https://i.pximg.net/c/600x1200_90/img-master/img/2022/11/01/04/58/36/102414087_p0_master1200.jpg"
 * },
 * "caption": "",
 * "restrict": 0,
 * "user": {
 *     "id": 85288725,
 *     "name": "天然ボケのyuu",
 *     "account": "bokeyuu",
 *     "profile_image_urls": {
 *     "medium": "https://i.pximg.net/user-profile/img/2022/08/22/10/06/52/23224791_1cca96c71cef33ba92a840bd2194a3e0_170.jpg"
 * },
 *     "is_followed": true
 * },
 * "tags": [
 * {
 *     "name": "女の子",
 *     "translated_name": "女孩子"
 * },
 * {
 *     "name": "黒髪",
 *     "translated_name": "黑发"
 * },
 * {
 *     "name": "オリジナル",
 *     "translated_name": "原创"
 * },
 * {
 *     "name": "髪飾り",
 *     "translated_name": "发饰"
 * },
 * {
 *     "name": "壁紙",
 *     "translated_name": "壁纸"
 * },
 * {
 *     "name": "花飾り",
 *     "translated_name": "flower decoration"
 * },
 * {
 *     "name": "旗袍",
 *     "translated_name": "cheongsam"
 * },
 * {
 *     "name": "チャイナドレス",
 *     "translated_name": "旗袍"
 * },
 * {
 *     "name": "オリキャラ",
 *     "translated_name": "原创角色"
 * },
 * {
 *     "name": "少女",
 *     "translated_name": "young girl"
 * }
 * ],
 * "tools": [],
 * "create_date": "2022-11-01T04:58:36+09:00",
 * "page_count": 1,
 * "width": 2048,
 * "height": 1230,
 * "sanity_level": 2,
 * "x_restrict": 0,
 * "series": null,
 * "meta_single_page": {
 *     "original_image_url": "https://i.pximg.net/img-original/img/2022/11/01/04/58/36/102414087_p0.jpg"
 * },
 * "meta_pages": [],
 * "total_view": 691,
 * "total_bookmarks": 399,
 * "is_bookmarked": false,
 * "visible": true,
 * "is_muted": false,
 * "total_comments": 0,
 * "illust_ai_type": 0,
 * "illust_book_style": 0,
 * "comment_access_control": 0
 *
 *
    "id": 102633379,
    "title": "チャイナドレスダイヤ",
    "type": "illust",
    "image_urls": {
        "square_medium": "https://i.pximg.net/c/360x360_70/img-master/img/2022/11/09/02/04/04/102633379_p0_square1200.jpg",
        "medium": "https://i.pximg.net/c/540x540_70/img-master/img/2022/11/09/02/04/04/102633379_p0_master1200.jpg",
        "large": "https://i.pximg.net/c/600x1200_90/img-master/img/2022/11/09/02/04/04/102633379_p0_master1200.jpg"
    },
    "caption": "もっと気軽にシャティン挑戦していけ<br /><strong><a href=\"https://twitter.com/Taki_oooooooo\" target=\"_blank\">twitter/Taki_oooooooo</a></strong>",
    "restrict": 0,
    "user": {
        "id": 81052264,
        "name": "TAKIO",
        "account": "user_mdft2883",
        "profile_image_urls": {
            "medium": "https://i.pximg.net/user-profile/img/2022/10/06/18/43/07/23426794_6bec9692e316c7572a4341dc3031ba06_170.jpg"
        },
        "is_followed": false
    },
    "tags": [
        {
            "name": "ウマ娘プリティーダービー",
            "translated_name": "赛马娘Pretty Derby"
        },
        {
            "name": "サトノダイヤモンド(ウマ娘)",
            "translated_name": "里见光钻（赛马娘）"
        },
        {
            "name": "チャイナドレス",
            "translated_name": "旗袍"
        },
        {
            "name": "はいてない",
            "translated_name": "真空"
        }
    ],
    "tools": [],
    "create_date": "2022-11-09T02:04:04+09:00",
    "page_count": 1,
    "width": 895,
    "height": 1343,
    "sanity_level": 2,
    "x_restrict": 0,
    "series": null,
    "meta_single_page": {
        "original_image_url": "https://i.pximg.net/img-original/img/2022/11/09/02/04/04/102633379_p0.jpg"
    },
    "meta_pages": [],
    "total_view": 12554,
    "total_bookmarks": 2334,
    "is_bookmarked": false,
    "visible": true,
    "is_muted": false,
    "total_comments": 9,
    "illust_ai_type": 1,
    "illust_book_style": 0,
    "comment_access_control": 0
 *** manga ***
    "id": 102799463,
    "title": "1日ごとにデレが増えてくツンデレデレデレデレデレデレデレちゃん",
    "type": "manga",
    "image_urls": {
        "square_medium": "https://i.pximg.net/c/360x360_70/img-master/img/2022/11/15/00/00/28/102799463_p0_square1200.jpg",
        "medium": "https://i.pximg.net/c/540x540_70/img-master/img/2022/11/15/00/00/28/102799463_p0_master1200.jpg",
        "large": "https://i.pximg.net/c/600x1200_90/img-master/img/2022/11/15/00/00/28/102799463_p0_master1200.jpg"
    },
    "caption": "",
    "restrict": 0,
    "user": {
        "id": 13651304,
        "name": "八木戸マト（焼きトマト）",
        "account": "sormanngaseisaku",
        "profile_image_urls": {
            "medium": "https://i.pximg.net/user-profile/img/2019/07/10/06/15/51/15988644_60085643340b7e91019dbdbe3ec61c61_170.png"
        },
        "is_followed": false
    },
    "tags": [
        {
            "name": "漫画",
            "translated_name": "manga"
        },
        {
            "name": "オリジナル",
            "translated_name": "原创"
        },
        {
            "name": "創作男女",
            "translated_name": "原创男女角色"
        },
        {
            "name": "ツン?そんな奴いたかい?",
            "translated_name": null
        },
        {
            "name": "ツン「こうなりゃ俺の親友を召喚する!」ヤン「呼んだ?」",
            "translated_name": null
        },
        {
            "name": "デレ「もう誰にも!止められねぇぇぇッ!!!」",
            "translated_name": null
        },
        {
            "name": "ツン「おのれデレめ…このままで終わると思うなよ!」",
            "translated_name": null
        },
        {
            "name": "ヤン「俺も加勢するぜ…」",
            "translated_name": null
        },
        {
            "name": "ツンの自覚症状",
            "translated_name": null
        }
    ],
    "tools": [
        "CLIP STUDIO PAINT"
    ],
    "create_date": "2022-11-15T00:00:28+09:00",
    "page_count": 2,
    "width": 1557,
    "height": 2150,
    "sanity_level": 2,
    "x_restrict": 0,
    "series": {
        "id": 171013,
        "title": "1日ごとにデレが増えてくツンツンツンツンツンデレちゃん"
    },
    "meta_single_page": {},
    "meta_pages": [
        {
            "image_urls": {
                "square_medium": "https://i.pximg.net/c/360x360_70/img-master/img/2022/11/15/00/00/28/102799463_p0_square1200.jpg",
                "medium": "https://i.pximg.net/c/540x540_70/img-master/img/2022/11/15/00/00/28/102799463_p0_master1200.jpg",
                "large": "https://i.pximg.net/c/600x1200_90/img-master/img/2022/11/15/00/00/28/102799463_p0_master1200.jpg",
                "original": "https://i.pximg.net/img-original/img/2022/11/15/00/00/28/102799463_p0.png"
            }
        },
        {
            "image_urls": {
                "square_medium": "https://i.pximg.net/c/360x360_70/img-master/img/2022/11/15/00/00/28/102799463_p1_square1200.jpg",
                "medium": "https://i.pximg.net/c/540x540_70/img-master/img/2022/11/15/00/00/28/102799463_p1_master1200.jpg",
                "large": "https://i.pximg.net/c/600x1200_90/img-master/img/2022/11/15/00/00/28/102799463_p1_master1200.jpg",
                "original": "https://i.pximg.net/img-original/img/2022/11/15/00/00/28/102799463_p1.png"
            }
        }
    ],
    "total_view": 130451,
    "total_bookmarks": 6641,
    "is_bookmarked": false,
    "visible": true,
    "is_muted": false,
    "illust_ai_type": 1,
    "illust_book_style": 0
 */
@Parcelize
data class Illust(
    val id: Long,
    val title: String,
    val type: String, // illust, ugoira, manga //TODO: novel?
    val image_urls: ImageUrls,
    val caption: String,
    val restrict: Int, //TODO: level 分级
    val user: User,
    val tags: List<Tag>,
    val tools: List<String>,
    val create_date: String,
    val page_count: Int,
    val width: Int,
    val height: Int,
    val sanity_level: Int, //TODO: sanity_level > 5 is nsfw
    val x_restrict: Int,
    var series: Series?,
    val meta_single_page: MetaSinglePage,
    val meta_pages: List<MetaPage>,
    val total_view: Int,
    val total_bookmarks: Int,
    var is_bookmarked: Boolean,
    var visible: Boolean,
    val is_muted: Boolean,
    val total_comments: Int, //not included in recommends
    val illust_ai_type: Int,
    val illust_book_style: Int, //TODO:
    val comment_access_control: Int, //TODO:
) : Parcelable

//ref: https://github.com/ArkoClub/async-pixiv/blob/0fcce0c5a096b5473424310ce5d9b6db35c7fd23/src/async_pixiv/model/other.py#L40
enum class AIType {
    NONE,  //0没有使用AI
    HALF,  //1使用了AI进行辅助
    FULL,  //2使用AI生成
}

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
 * {
"id": 171013,
"title": "1日ごとにデレが増えてくツンツンツンツンツンデレちゃん"
}
 */
@Parcelize
data class Series(
    var id: Int,
    var title: String
) : Parcelable