/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
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

package com.perol.asdpl.pixivez.manager

import android.os.Build
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.perol.asdpl.pixivez.objects.FileInfo
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import com.perol.asdpl.pixivez.viewmodel.BaseViewModel
import com.tencent.mmkv.MMKV
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.TimeUnit

class RenameTask(fileInfo: FileInfo) {
    val file: FileInfo = fileInfo
    val pid: Long? = if (file.isPic()) file.pid else null
    val part: Int? = if (file.isPic()) fileInfo.part.toIntOrNull() else null
}

class ImgManagerViewModel : BaseViewModel() {
    val pre = PxEZApp.instance.pre
    private val retrofitRepository: RetrofitRepository = RetrofitRepository.getInstance()
    var path = MutableLiveData<String>()
    var saveformat = pre.getString("ImgManagerSaveFormat", PxEZApp.saveformat)!!
    var TagSeparator = pre.getString("ImgManagerTagSeparator", PxEZApp.TagSeparator)!!
    var files: MutableList<FileInfo>? = null
    var task: List<RenameTask>? = null
    var length_filter = false
    var rename_once = false
    lateinit var adapter: ImgManagerAdapter
    lateinit var layoutManager: LinearLayoutManager

    fun getInfo() {
        /*Flowable.fromIterable(files).parallel().map {
            if(!length_filter ||it.file.name.length>40)
                it.pid?.run {
                    retrofitRepository.getIllust(this).subscribeOn(Schedulers.io()).subscribe({rt->
                        it.file.illust = rt!!.illust
                        it.file.target = Works.parseSaveFormat(rt.illust,it.part?.toInt(),saveformat,TagSeparator,false)
                        it.file.checked = it.file.target != it.file.name
                    }, {}, {}).add()
                    Log.d("imgMgr","get"+this)
                }
            return@map it
        }.subscribeOn(Schedulers.io().add()*/
        val kv = MMKV.defaultMMKV(MMKV.MULTI_PROCESS_MODE, null)
        val taskmap = HashMap<Long, RenameTask>()
        task?.filter {
            (!length_filter || it.file.name.length < 50) && it.pid != null
        }?.forEach {
            Observable.just(it).subscribeOn(Schedulers.io()).flatMap { it ->
                taskmap[it.pid!!] = it
                if (kv.containsKey(it.pid.toString())) {
                    Observable.just(kv.decodeParcelable(it.pid.toString(), Illust::class.java))
                }
                else {
                    retrofitRepository.getIllust(it.pid).flatMap {
                        kv.encode(it.illust.id.toString(), it.illust)
                        Observable.just(it.illust)
                    }.subscribeOn(Schedulers.io()).doOnError { e ->
                        Log.e("imgMgr", "getIllust ${it.pid} : ${e.message} ")
                    }
                }
            }
                .subscribeOn(Schedulers.io())
                .map { rt ->
                    // Log.d("imgMgr","get"+this+"p"+it.part)
                    val it = taskmap[rt.id]!!
                    it.file.illust = rt
                    it.file.target = Works.parseSaveFormat(rt, it.part, saveformat, TagSeparator, false)
                    it.file.checked = (it.file.target != it.file.name)
                    // Log.d("imgMgr","get"+it.pid+"p"+it.part+"check"+it.file.checked )
                    if (rename_once) {
                        rename(it)
                    }
                    it
                }
                .doFinally {
                    // Log.d("imgMgr","all")
                    File(path.value + File.separatorChar + "rename.log")
                        .writeText(Gson().toJson(task))
                    taskmap.clear()
                    AndroidSchedulers.mainThread().scheduleDirect({
                        adapter.notifyDataSetChanged()
                    }, 100, TimeUnit.MICROSECONDS).add()
                }
                .observeOn(AndroidSchedulers.mainThread()).subscribe({ rt ->
                    // Log.d("imgMgr","refresh"+it.pid+"p"+it.part)
                    val preIndex = files!!.indexOf(rt.file)
                    if (preIndex >= layoutManager.findFirstVisibleItemPosition() &&
                        preIndex <= layoutManager.findLastVisibleItemPosition()
                    ) {
                        adapter.notifyItemChanged(preIndex)
                    }
                }, {
                    Log.e("imgMgr", "error$it")
                }, {}).add()
        }
    }

    fun rename(it: RenameTask) {
        if (it.file.name == it.file.target || it.file.target == null) {
            return
        }
        val orig = File(it.file.path)
        val tar = "${orig.parent}${File.separator}${it.file.target}"
        it.file.name = it.file.target!!
        File(it.file.path).renameTo(File(tar))
        val preIndex = files!!.indexOf(it.file)
        AndroidSchedulers.mainThread().scheduleDirect {
            if (preIndex >= layoutManager.findFirstVisibleItemPosition() &&
                preIndex <= layoutManager.findLastVisibleItemPosition()
            ) {
                adapter.notifyItemChanged(preIndex)
            }
        }.add()
    }

    fun renameAll() {
        Thread {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                task?.parallelStream()?.filter {
                    it.file.checked
                }?.forEach {
                    rename(it)
                }
            }
            else {
                task?.filter {
                    it.file.checked
                }?.forEach {
                    rename(it)
                }
            }
        }.start()
    }
}
