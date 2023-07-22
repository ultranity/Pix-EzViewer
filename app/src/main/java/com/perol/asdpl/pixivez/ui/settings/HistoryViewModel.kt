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
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.data.AppDatabase
import com.perol.asdpl.pixivez.data.entity.IllustBeanEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel : BaseViewModel() {
    val illustBeans = MutableLiveData<MutableList<IllustBeanEntity>>()
    private val appDatabase = AppDatabase.getInstance(PxEZApp.instance)

    fun first() {
        CoroutineScope(Dispatchers.IO).launch {
            val history = appDatabase.illusthistoryDao().getIllustHistory().asReversed()
            withContext(Dispatchers.Main) {
                illustBeans.value = history.toMutableList()
            }
        }
    }

    fun fabOnClick() {
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.illusthistoryDao().deleteHistory()
        }
    }

    fun deleteSelect(i: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.illusthistoryDao().deleteOne(illustBeans.value!![i])
            withContext(Dispatchers.Main) {
                illustBeans.value!!.removeAt(i)
            }
        }
    }
}
