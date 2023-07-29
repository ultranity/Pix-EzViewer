package com.perol.asdpl.pixivez.data.model

interface INext {
    val next_url: String?
}

interface IIllustNext : INext {
    val illusts: MutableList<Illust>
}