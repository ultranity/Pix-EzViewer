package com.perol.asdpl.pixivez.objects

import android.text.TextUtils

// edited from Pixiv-Shaft: https://github.com/CeuiLiSA/Pixiv-Shaft/blob/4329496da88676d313f72b67afe4b2f910a5ff7c/app/src/main/java/ceui/lisa/utils/Emoji.java
class EmojiItem(var name: String, var resource: String)
object EmojiUtil {
    private const val EMOJI_101 = "(normal)"
    private const val EMOJI_102 = "(surprise)"
    private const val EMOJI_103 = "(serious)"
    private const val EMOJI_104 = "(heaven)"
    private const val EMOJI_105 = "(happy)"
    private const val EMOJI_106 = "(excited)" //为你写诗，为你静止 //-1s
    private const val EMOJI_107 = "(sing)"
    private const val EMOJI_108 = "(cry)"

    private const val EMOJI_201 = "(normal2)"
    private const val EMOJI_202 = "(shame2)"
    private const val EMOJI_203 = "(love2)"
    private const val EMOJI_204 = "(interesting2)"
    private const val EMOJI_205 = "(blush2)"
    private const val EMOJI_206 = "(fire2)"
    private const val EMOJI_207 = "(angry2)"
    private const val EMOJI_208 = "(shine2)"
    private const val EMOJI_209 = "(panic2)"

    private const val EMOJI_301 = "(normal3)"
    private const val EMOJI_302 = "(satisfaction3)"
    private const val EMOJI_303 = "(surprise3)"
    private const val EMOJI_304 = "(smile3)"
    private const val EMOJI_305 = "(shock3)"
    private const val EMOJI_306 = "(gaze3)"
    private const val EMOJI_307 = "(wink3)"
    private const val EMOJI_308 = "(happy3)"
    private const val EMOJI_309 = "(excited3)"
    private const val EMOJI_310 = "(love3)"

    private const val EMOJI_401 = "(normal4)"
    private const val EMOJI_402 = "(surprise4)"
    private const val EMOJI_403 = "(serious4)"
    private const val EMOJI_404 = "(love4)"
    private const val EMOJI_405 = "(shine4)"
    private const val EMOJI_406 = "(sweat4)"
    private const val EMOJI_407 = "(shame4)"
    private const val EMOJI_408 = "(sleep4)"

    private const val EMOJI_501 = "(heart)"
    private const val EMOJI_502 = "(teardrop)"
    private const val EMOJI_503 = "(star)"
    // resource from https://s.pximg.net/common/images/emoji/101.png
    private val RESOURCE = arrayOf(
        "101.png", "102.png", "103.png", "104.png", "105.png", "106.png", "107.png", "108.png",
        "201.png", "202.png", "203.png", "204.png", "205.png", "206.png", "207.png", "208.png", "209.png",
        "301.png", "302.png", "303.png", "304.png", "305.png", "306.png", "307.png", "308.png", "309.png", "310.png",
        "401.png", "402.png", "403.png", "404.png", "405.png", "406.png", "407.png", "408.png",
        "501.png", "502.png", "503.png"
    )
    private val NAMES = arrayOf(
        EMOJI_101, EMOJI_102, EMOJI_103, EMOJI_104, EMOJI_105, EMOJI_106, EMOJI_107, EMOJI_108,
        EMOJI_201, EMOJI_202, EMOJI_203, EMOJI_204, EMOJI_205, EMOJI_206, EMOJI_207, EMOJI_208, EMOJI_209,
        EMOJI_301, EMOJI_302, EMOJI_303, EMOJI_304, EMOJI_305, EMOJI_306, EMOJI_307, EMOJI_308, EMOJI_309, EMOJI_310,
        EMOJI_401, EMOJI_402, EMOJI_403, EMOJI_404, EMOJI_405, EMOJI_406, EMOJI_407, EMOJI_408,
        EMOJI_501, EMOJI_502, EMOJI_503
    )
    private val EMOJI_REGEX = Regex("""(\(.{3,13}?\))""")
    private const val HEAD = "<img width=\"24\" height=\"24\" src=\""
    private const val EOF = "\">"

    /**
     * 判断一个字符串中是否包含形如 (sleep4) (heart) (star) 的表情
     *
     * @param origin comments
     * @return transformed string
     */
    fun transform(origin: String): String {
        if (TextUtils.isEmpty(origin)) {
            return origin
        }
        val final = EMOJI_REGEX.replace(origin){
            (map[it.value]?.let{ "$HEAD$it$EOF" }?:it.value).toString()
        }
        return final
    }
    private val map: MutableMap<String, String> = (NAMES zip RESOURCE).toMap(HashMap(NAMES.size))

    val emojis: List<EmojiItem> = map.map { EmojiItem(it.key, it.value) }
}
