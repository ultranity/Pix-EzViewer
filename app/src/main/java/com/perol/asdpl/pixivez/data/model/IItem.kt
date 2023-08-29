package com.perol.asdpl.pixivez.data.model
enum class Item {
    illust,
    manga,
    novel,
    ugoira,
}

interface INext<T> {
    val next_url: String?
    fun data(): T
}

interface IIllustNext : INext<MutableList<Illust>> {
    val illusts: MutableList<Illust>
    override fun data() = illusts
}