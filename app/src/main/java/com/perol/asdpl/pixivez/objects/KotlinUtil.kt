package com.perol.asdpl.pixivez.objects

object KotlinUtil {
    fun Boolean.Int(): Int = if (this) 1 else 0
    operator fun Boolean.times(i: Int): Int = if (this) i else 0
    operator fun Boolean.plus(i: Int): Int = if (this) i + 1 else i
    operator fun Int.times(b: Boolean): Int = if (b) this else 0
    operator fun Int.plus(b: Boolean): Int = if (b) this + 1 else this
}