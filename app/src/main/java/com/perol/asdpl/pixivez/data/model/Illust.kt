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

import androidx.lifecycle.MutableLiveData
import com.perol.asdpl.pixivez.base.EmptyAsNullJsonTransformingSerializer
import com.perol.asdpl.pixivez.objects.CopyFrom
import com.perol.asdpl.pixivez.objects.IllustCacheRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.WeakHashMap

@Serializable
class IllustNext(
    override val illusts: MutableList<Illust>,
    override val next_url: String?
) : IIllustNext

@Serializable
class UserIllustNext(
    val user: User,
    override val illusts: MutableList<Illust>,
    override val next_url: String?,
) : IIllustNext

/**
 * illust : {"id":67261030,"title":"吃狗粮的日子到了","type":"illust","image_urls":{"square_medium":"https://i.pximg.net/c/360x360_70/img-master/img/2018/02/14/01/02/59/67261030_p0_square1200.jpg","medium":"https://i.pximg.net/c/540x540_70/img-master/img/2018/02/14/01/02/59/67261030_p0_master1200.jpg","large":"https://i.pximg.net/c/600x1200_90/img-master/img/2018/02/14/01/02/59/67261030_p0_master1200.jpg"},"caption":"你们快乐，我继续肝崩崩崩","restrict":0,"user":{"id":24087148,"name":"脸黑の零氪渣","account":"zeng_yu","profile_image_urls":{"medium":"https://i.pximg.net/user-profile/img/2017/08/04/20/04/06/12978904_75e510554696aaa9f228cf94736b57e9_170.gif"},"is_followed":true},"tags":[{"name":"崩坏3"},{"name":"崩坏3rd"},{"name":"德莉莎"},{"name":"情人节"},{"name":"崩壊3rd"}],"tools":["SAI"],"create_date":"2018-02-14T01:02:59+09:00","page_count":1,"width":1200,"height":1600,"sanity_level":2,"series":null,"meta_single_page":{"original_image_url":"https://i.pximg.net/img-original/img/2018/02/14/01/02/59/67261030_p0.jpg"},"meta_pages":[],"total_view":884,"total_bookmarks":91,"is_bookmarked":false,"visible":true,"is_muted":false,"total_comments":3}
 */
@Serializable
class IllustDetailResponse(val illust: Illust)

/*
@Serializable
data class Illust_original(
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
    val series: Series?,
    @Serializable(with = EmptyAsNullMetaSinglePage::class)
    val meta_single_page: MetaSinglePage?,
    var meta_pages: List<MetaPage>,
    val total_view: Int,
    val total_bookmarks: Int,
    var is_bookmarked: Boolean,
    var visible: Boolean,
    val is_muted: Boolean,
    val total_comments: Int = 0, //not included in recommends
    val illust_ai_type: Int,
    val illust_book_style: Int, //TODO:
    val comment_access_control: Int = 0, //TODO:
){
    fun merge_meta(){
        if (meta_pages.isEmpty()){
            meta_pages = listOf(MetaPage(ImageUrlsX(
                image_urls.large,
                image_urls.medium,
                meta_single_page!!.original_image_url,
                image_urls.square_medium)))
        }
    }
}
*/
@Serializable
data class IllustX(
    val id: Int,
    val title: String,
    val type: String, // illust, ugoira, manga //TODO: novel?
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
    val series: Series?,
    val meta: List<ImageUrlsX>,
    var total_view: Int,
    var total_bookmarks: Int,
    var visible: Boolean,
    val is_muted: Boolean,
    val total_comments: Int = 0, //not included in recommends
    val illust_ai_type: Int,
    val illust_book_style: Int, //TODO:
    val comment_access_control: Int = 0, //TODO:
) : CopyFrom<IllustX> {
    @Transient
    private val binders = WeakHashMap<MutableLiveData<Boolean>, String>()
    fun addBinder(key: String, binder: MutableLiveData<Boolean>) {
        binders[binder] = key
    }

    var is_bookmarked: Boolean = false
        set(value) {
            val updated = field != value
            if (updated) {
                field = value
                CoroutineScope(Dispatchers.Main).launch {
                    binders.forEach { it.key.value = value }
                }
            }

        }

    override fun copyFrom(src: IllustX) {
        total_view = src.total_view
        total_bookmarks = src.total_bookmarks
        is_bookmarked = src.is_bookmarked
        visible = src.visible
    }
}
// workaround until https://github.com/Kotlin/kotlinx.serialization/issues/1169 fixed
typealias Illust = @Serializable(with = MergeMetaIllustSerializer::class) IllustX

object MergeMetaIllustSerializer :
    EmptyAsNullJsonTransformingSerializer<Illust>(IllustX.serializer()) {
    override fun deserialize(decoder: Decoder): Illust {
        var illust = super.deserialize(decoder)
        illust = IllustCacheRepo.update(illust.id, illust)
        return illust
    }

    override fun transformDeserialize(element: JsonElement): JsonElement {
        val transformed = buildJsonObject {
            val meta_pages = if (element.jsonObject["meta_pages"]!!.jsonArray.isEmpty()) {
                JsonArray(listOf(buildJsonObject {
                    put(
                        "original", element.jsonObject["meta_single_page"]!!
                            .jsonObject["original_image_url"]!!.jsonPrimitive
                    )
                    element.jsonObject["image_urls"]!!.jsonObject.forEach {
                        put(it.key, it.value)
                    }
                }))
            } else {
                JsonArray(element.jsonObject["meta_pages"]!!.jsonArray.map {
                    it.jsonObject["image_urls"]!!
                })
            }
            element.jsonObject.filterKeys {
                it != "meta_single_page" && it != "image_urls" && it != "meta_pages"
            }.forEach {
                put(it.key, it.value)
            }
            put("meta", meta_pages)
        }
        return super.transformDeserialize(transformed)
    }
}



/*@Serializable
class MetaPage(
    val image_urls: ImageUrlsX
)*/

/**
 * square_medium : https://i.pximg.net/c/360x360_70/img-master/img/2017/12/03/05/15/02/66137839_p0_square1200.jpg
 * medium : https://i.pximg.net/c/540x540_70/img-master/img/2017/12/03/05/15/02/66137839_p0_master1200.jpg
 * large : https://i.pximg.net/c/600x1200_90/img-master/img/2017/12/03/05/15/02/66137839_p0_master1200.jpg
 */
@Serializable
class ImageUrlsX(
    val large: String,
    val medium: String,
    val original: String,
    val square_medium: String
)

@Serializable
class ImageUrls(
    val large: String,
    val medium: String,
    val square_medium: String
)

/**
 * original_image_url : https://i.pximg.net/img-original/img/2017/12/03/05/15/02/66137839_p0.png
 */
/*@Serializable
class MetaSinglePage(
    val original_image_url: String
)*/

/**
 * {
"id": 171013,
"title": "1日ごとにデレが増えてくツンツンツンツンツンデレちゃん"
}
 */
@Serializable
class Series(
    val id: Int,
    val title: String
)

//class EmptyAsNullMetaSinglePage:
//    EmptyAsNullJsonTransformingSerializer<MetaSinglePage?>(MetaSinglePage.serializer().nullable)
