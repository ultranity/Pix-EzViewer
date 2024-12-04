package com.perol.asdpl.pixivez.data.model

interface INext<T> {
    val next_url: String?
    fun data(): MutableList<T>
}

interface IIllustNext : INext<Illust> {
    val illusts: MutableList<Illust>
    override fun data() = illusts
}