package com.perol.asdpl.pixivez.data.model

import com.perol.asdpl.pixivez.base.EmptyAsNullJsonTransformingSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

// 字段对照 app-api 小说响应;非 id/图片/作者的元字段一律给默认值,
// 兼容 recommend/rank/follow/search 等列表间的字段差异,防解析崩
@Serializable
data class Novel(
    val id: Int,
    val title: String = "",
    val caption: String = "",
    val restrict: Int = 0,
    @SerialName("x_restrict")
    val x_restrict: Int = 0,
    @SerialName("image_urls")
    val image_urls: ImageUrls,
    @SerialName("is_original")
    val is_original: Boolean = false,
    @SerialName("create_date")
    val create_date: String = "",
    val tags: List<Tag> = emptyList(),
    @SerialName("page_count")
    val page_count: Int = 0,
    @SerialName("text_length")
    val text_length: Int = 0,
    val user: User,
    @Serializable(with = EmptyAsNullSeries::class)
    val series: Series? = null,
    @SerialName("total_view")
    val totalView: Int = 0,
    @SerialName("total_bookmarks")
    val total_bookmarks: Int = 0,
    @SerialName("is_bookmarked")
    val is_bookmarked: Boolean = false,
    @SerialName("visible")
    val visible: Boolean = true,
    @SerialName("is_muted")
    val is_muted: Boolean = false,
    @SerialName("total_comments")
    val total_comments: Int = 0,
    @SerialName("is_mypixiv_only")
    val is_mypixiv_only: Boolean = false,
    @SerialName("is_x_restricted")
    val is_x_restricted: Boolean = false,
    @SerialName("novel_ai_type")
    val novel_ai_type: Int = 0
)
object EmptyAsNullSeries :
    EmptyAsNullJsonTransformingSerializer<Series?>(Series.serializer().nullable)

// ─── 小说列表 / 详情 / 正文 响应 ─────────────────────────────────

// GET /v1/user/novels 等列表响应,next_url 分页复用 INext 惯例
@Serializable
class NovelResponse(
    val novels: MutableList<Novel>,
    override val next_url: String?
) : INext<Novel> {
    override fun data() = novels
}

// GET /v2/novel/detail
@Serializable
class NovelDetailResponse(
    val novel: Novel
)

// GET /webview/v2/novel 内嵌 JSON 的正文模型:字段为 camelCase、id 为字符串;
// images = 用户上传图([uploadedimage:id]),illusts = 引用插画([pixivimage:id]),
// 其余字段靠 Json.ignoreUnknownKeys 容错
@Serializable
class NovelWebResponse(
    val id: String = "",
    val title: String = "",
    val text: String = "",
    val seriesNavigation: SeriesNavigation? = null,
    @Serializable(with = NovelImageMapSerializer::class)
    val images: Map<String, NovelImage>? = null,
    @Serializable(with = NovelIllustMapSerializer::class)
    val illusts: Map<String, NovelIllustRef?>? = null
)

/**
 * The webview payload normally uses an object keyed by image/id. Some novels emit []
 * for an empty collection instead. Treat only that exact empty array as an empty map;
 * a non-empty array still reaches the map serializer and fails loudly rather than
 * silently discarding references.
 */
private open class EmptyArrayAsEmptyMapSerializer<T>(
    elementSerializer: KSerializer<T>,
) : KSerializer<Map<String, T>?> {
    private val delegate = MapSerializer(String.serializer(), elementSerializer).nullable

    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Map<String, T>?) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(encoder.json.encodeToJsonElement(delegate, value))
        } else {
            delegate.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): Map<String, T>? {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            val normalized: JsonElement =
                if (element is JsonArray && element.isEmpty()) JsonObject(emptyMap()) else element
            return decoder.json.decodeFromJsonElement(delegate, normalized)
        }
        return delegate.deserialize(decoder)
    }
}

private object NovelImageMapSerializer : EmptyArrayAsEmptyMapSerializer<NovelImage>(NovelImage.serializer())

private object NovelIllustMapSerializer : EmptyArrayAsEmptyMapSerializer<NovelIllustRef?>(NovelIllustRef.serializer().nullable)

@Serializable
class NovelImage(val urls: NovelImageUrls = NovelImageUrls())

@Serializable
class NovelImageUrls(
    @SerialName("480mw") val mw480: String? = null,
    val original: String? = null
)

@Serializable
class NovelIllustRef(val illust: NovelIllustBody? = null)

@Serializable
class NovelIllustBody(val images: NovelIllustImages = NovelIllustImages())

@Serializable
class NovelIllustImages(
    val medium: String? = null,
    val original: String? = null
)

@Serializable
class SeriesNavigation(
    val prevNovel: NovelNaviItem? = null,
    val nextNovel: NovelNaviItem? = null
)

@Serializable
class NovelNaviItem(
    val id: Int,
    val viewable: Boolean = true,
    val title: String = ""
)

// Legacy GET /v1/novel/text response model; the reader uses /webview/v2/novel.
@Serializable
class NovelTextResponse(
    @SerialName("novel_text")
    val novel_text: String = ""
)

// GET /v1/trending-tags/novel:结构对照 trending-tags/illust,承载对象为 novel
@Serializable
class NovelTrendingtagResponse(
    @SerialName("trend_tags")
    val trend_tags: MutableList<NovelTrendTagsBean>
)

@Serializable
class NovelTrendTagsBean(
    val tag: String,
    @SerialName("translated_name")
    val translated_name: String?,
    val novel: Novel
)
