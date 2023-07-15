package com.perol.asdpl.pixivez.ui

import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

// edited from https://github.com/hasancse91/Collapsing-Toolbar-Tutorial
abstract class AppBarStateChangeListener(
    private val offset: Int = 0,
    private val listenIDLE: Boolean = false
) : AppBarLayout.OnOffsetChangedListener {

    enum class State {
        EXPANDED, COLLAPSED, IDLE
    }

    private var mCurrentState = State.EXPANDED

    override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
        if (offset==0){ //original
            if (i == 0 && mCurrentState != State.EXPANDED) {
                onStateChanged(appBarLayout, State.EXPANDED)
                mCurrentState = State.EXPANDED
            } else if (abs(i) >= appBarLayout.totalScrollRange && (mCurrentState != State.COLLAPSED)) {
                onStateChanged(appBarLayout, State.COLLAPSED)
                mCurrentState = State.COLLAPSED
            } else if (listenIDLE and (mCurrentState != State.IDLE)) {
                onStateChanged(appBarLayout, State.IDLE)
                mCurrentState = State.IDLE
            }
        }
        else {
            if ((i <= offset - appBarLayout.totalScrollRange) && (mCurrentState != State.COLLAPSED)) {
                onStateChanged(appBarLayout, State.COLLAPSED)
                mCurrentState = State.COLLAPSED
            } else if ((i > offset - appBarLayout.totalScrollRange) && mCurrentState != State.EXPANDED) {
                onStateChanged(appBarLayout, State.EXPANDED)
                mCurrentState = State.EXPANDED
            } else if (listenIDLE and (mCurrentState != State.IDLE)) {
                onStateChanged(appBarLayout, State.IDLE)
                mCurrentState = State.IDLE
            }
        }
    }

    abstract fun onStateChanged(
        appBarLayout: AppBarLayout,
        state: State
    )
}