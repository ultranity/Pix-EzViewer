package com.perol.asdpl.pixivez.objects

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

object KotlinUtil {
    fun Boolean.Int(): Int = if (this) 1 else 0
    operator fun Boolean.times(i: Int): Int = if (this) i else 0
    operator fun Boolean.plus(i: Int): Int = if (this) i + 1 else i
    operator fun Int.times(b: Boolean): Int = if (b) this else 0
    operator fun Int.plus(b: Boolean): Int = if (b) this + 1 else this
    // reference:https://gist.github.com/bartekpacia/eb1c92886acf3972c3f030cde2579ebb
    fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, reactToChange: (T) -> Unit): Observer<T> {
        val wrappedObserver = object : Observer<T> {
            override fun onChanged(data: T) {
                reactToChange(data)
                removeObserver(this)
            }
        }

        observe(owner, wrappedObserver)
        return wrappedObserver
    }
}