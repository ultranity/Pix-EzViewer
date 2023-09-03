package com.perol.asdpl.pixivez.data.model

enum class Item {
    illust,
    manga,
    novel,
    ugoira,
}

enum class Restrict(val value: String) {
    PUBLIC("public"),
    PRIVATE("private"),
    ALL("all"),
}

//ref: https://github.com/ArkoClub/async-pixiv/blob/0fcce0c5a096b5473424310ce5d9b6db35c7fd23/src/async_pixiv/model/other.py#L40
enum class AIType {
    NONE,  //0没有使用AI
    HALF,  //1使用了AI进行辅助
    FULL,  //2使用AI生成
}