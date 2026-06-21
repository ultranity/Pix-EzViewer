/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
 * Copyright (c) 2019 Perol_Notsfsssf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package com.perol.asdpl.pixivez.view

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/*
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ SafeStaggeredGridLayoutManager —— 兜底:吞掉 SGLM 内部状态损坏的已知       │
 * │ framework 崩溃(AOSP 长期 known issue)。                                   │
 * │                                                                            │
 * │ 根治靠 onPause 停 fling + 避免滑动中途整表重置(见 PicListFragment);     │
 * │ 此处仅作安全网:当 mSpans/LazySpanLookup 在回收/布局时仍被并发置空,        │
 * │ 捕获 NPE/IndexOutOfBounds 使其降级为"本帧不滚动/不布局"而非 crash。       │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
class SafeStaggeredGridLayoutManager(spanCount: Int, orientation: Int) :
    StaggeredGridLayoutManager(spanCount, orientation) {

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: Exception) {
            Log.w(TAG, "onLayoutChildren swallowed framework SGLM state crash", e)
        }
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int = try {
        super.scrollVerticallyBy(dy, recycler, state)
    } catch (e: Exception) {
        Log.w(TAG, "scrollVerticallyBy swallowed framework SGLM state crash", e)
        0
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int = try {
        super.scrollHorizontallyBy(dx, recycler, state)
    } catch (e: Exception) {
        Log.w(TAG, "scrollHorizontallyBy swallowed framework SGLM state crash", e)
        0
    }

    companion object {
        private const val TAG = "SafeSGLM"
    }
}
