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

package com.perol.asdpl.pixivez.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.TagsShowAdapter
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

// UserBookMarkFragment
class TagsShowDialog : DialogFragment() {

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, "tagshowDialog")
    }

    val disposables = CompositeDisposable()
    fun Disposable.add() {
        disposables.add(this)
    }
    var callback: Callback? = null
    override fun onDestroy() {
        super.onDestroy()
        callback = null
        disposables.clear()
    }

    override fun onCancel(dialog: DialogInterface) {
        val tabLayout = getDialog()?.findViewById<TabLayout>(R.id.tablayout_tagsshow)
        if (tabLayout != null) {
            callback!!.onClick(
                "",
                if (tabLayout.selectedTabPosition == 0) {
                    "public"
                }
                else {
                    "private"
                }
            )
        }
        super.onCancel(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    interface Callback {
        fun onClick(string: String, public: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val retrofitRepository = RetrofitRepository.getInstance()

        val bundle = arguments
        val inflater = LayoutInflater.from(activity)
        val tagList = bundle!!.getStringArrayList("tags")
        val countList = bundle.getIntegerArrayList("counts")
        val id = bundle.getLong("id")
        var nextUrl = bundle.getString("nextUrl")
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val dialogView = inflater.inflate(R.layout.view_tagsshow, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerview_tags)
        val all = dialogView.findViewById<ConstraintLayout>(R.id.all)
//        val viewPager = dialogView.findViewById<ViewPager>(R.id.viewpager)
        val tabLayout = dialogView.findViewById<TabLayout>(R.id.tablayout_tagsshow)
        val tagsShowAdapter = TagsShowAdapter(R.layout.view_tagsshow_item, tagList, countList!!)
        recyclerView.adapter = tagsShowAdapter
        tagsShowAdapter.setOnItemClickListener { adapter, view, position ->
            callback!!.onClick(
                tagsShowAdapter.data[position],
                if (tabLayout.selectedTabPosition == 0) {
                    "public"
                }
                else {
                    "private"
                }
            )
            this.dismiss()
        }
        all.setOnClickListener {
            callback!!.onClick(
                "",
                if (tabLayout.selectedTabPosition == 0) {
                    "public"
                }
                else {
                    "private"
                }
            )
            this.dismiss()
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {}

            override fun onTabUnselected(p0: TabLayout.Tab?) {}

            override fun onTabSelected(p0: TabLayout.Tab?) {
                if (p0 != null) {
                    retrofitRepository.getIllustBookmarkTags(
                        id,
                        if (p0.position == 0) {
                            "public"
                        }
                        else {
                            "private"
                        }
                    )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io()).subscribe({
                            nextUrl = it.next_url
                            val x = ArrayList<String>()
                            tagsShowAdapter.counts.clear()
                            if (it.bookmark_tags.isNullOrEmpty()) {
                                callback!!.onClick(
                                    "",
                                    if (tabLayout.selectedTabPosition == 0) {
                                        "public"
                                    }
                                    else {
                                        "private"
                                    }
                                )
                                this@TagsShowDialog.dismiss()
                            }
                            it.bookmark_tags.map { ot ->
                                x.add(ot.name)
                                tagsShowAdapter.counts.add(ot.count)
                            }
                            tagsShowAdapter.setNewInstance(x)
                        }, {}, {}).add()
                }
            }
        })
        tagsShowAdapter.loadMoreModule.setOnLoadMoreListener {
            if (!nextUrl.isNullOrBlank()) {
                retrofitRepository.getNextTags(nextUrl!!)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                        {
                            nextUrl = it.next_url
                            val arrayList = ArrayList<String>()
                            it.bookmark_tags.map {
                                arrayList.add(it.name)
                                tagsShowAdapter.counts.add(it.count)
                            }
                            tagsShowAdapter.addData(arrayList)
                        },
                        { tagsShowAdapter.loadMoreModule.loadMoreFail() },
                        { tagsShowAdapter.loadMoreModule.loadMoreComplete() }
                    ).add()
            }
            else {
                tagsShowAdapter.loadMoreModule.loadMoreEnd()
            }
        }
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        builder.setView(dialogView)
        return builder.create()
    }
}
