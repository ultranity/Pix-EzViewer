package com.perol.asdpl.pixivez.data.model

interface INext<T> {
    val next_url: String?
    fun data(): T
}

interface IIllustNext : INext<MutableList<Illust>> {
    val illusts: MutableList<Illust>
    override fun data() = illusts
}