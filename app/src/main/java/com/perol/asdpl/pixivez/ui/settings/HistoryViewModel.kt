/*
 * MIT License
 *
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

package com.perol.asdpl.pixivez.ui.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.data.HistoryDatabase
import com.perol.asdpl.pixivez.data.entity.HistoryEntity
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel : BaseViewModel() {
    val history = MutableLiveData<MutableList<HistoryEntity>>()
    private val historyDatabase = HistoryDatabase.getInstance(PxEZApp.instance)

    // These used to run on bare CoroutineScope(Dispatchers.IO) objects, which are tied to nothing
    // and are never cancelled: leaving the screen mid-query left the work running and still able
    // to publish into LiveData. viewModelScope cancels with the ViewModel.

    /** True while a page request is in flight, so scrolling cannot queue duplicate loads. */
    private var loadingPage = false

    /** Set once a short page comes back, meaning there is nothing older left to fetch. */
    private var reachedEnd = false

    fun first() {
        viewModelScope.launch {
            loadingPage = true
            val loaded = withContext(Dispatchers.IO) {
                historyDatabase.viewHistoryDao().getPage(PAGE_SIZE, 0).toMutableList()
            }
            reachedEnd = loaded.size < PAGE_SIZE
            history.value = loaded
            loadingPage = false
        }
    }

    /**
     * Appends the next page. Called when the list is scrolled near its end, so the whole history
     * remains reachable while only what has actually been scrolled to is ever held in memory.
     */
    fun loadMore() {
        if (loadingPage || reachedEnd) return
        val current = history.value ?: return
        loadingPage = true
        viewModelScope.launch {
            val next = withContext(Dispatchers.IO) {
                historyDatabase.viewHistoryDao().getPage(PAGE_SIZE, current.size)
            }
            // A page smaller than requested means the table is exhausted.
            if (next.size < PAGE_SIZE) reachedEnd = true
            if (next.isNotEmpty()) {
                current.addAll(next)
                // Re-emit the same instance so the observer rebinds; the adapter was handed this
                // list, so its contents are already in step.
                history.value = current
            }
            loadingPage = false
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyDatabase.viewHistoryDao().clear()
            }
            // Also empty the observed list; previously the cleared rows stayed on screen until
            // the fragment happened to be recreated.
            reachedEnd = true
            history.value = mutableListOf()
        }
    }

    fun deleteSelect(i: Int, after: () -> Unit) {
        // Resolve the item on the caller's (main) thread and bounds-check it. The old code read
        // history.value!![i] from an IO thread and would NPE/crash if the list was not loaded yet
        // or the index had moved on.
        val current = history.value ?: return
        val item = current.getOrNull(i) ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyDatabase.viewHistoryDao().delete(item)
            }
            // Mutated in place on purpose: the adapter was handed this same list instance, so this
            // keeps it in step with the notifyItemRemoved(i) the caller issues in `after`.
            current.removeAt(i)
            after()
        }
    }

    companion object {
        /** Rows fetched per page. The rest stay on disk until scrolling asks for them. */
        const val PAGE_SIZE = 500

        /** How close to the end of the list a scroll gets before the next page is requested. */
        const val PREFETCH_DISTANCE = 30
    }
}
