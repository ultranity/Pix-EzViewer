package com.chad.brvah.animation

import android.animation.Animator
import android.view.View

/**
 * https://github.com/CymChad/BaseRecyclerViewAdapterHelper
 */
interface ItemAnimator {
    fun animator(view: View): Animator
}