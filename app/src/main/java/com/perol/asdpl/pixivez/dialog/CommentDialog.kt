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

import android.app.ActivityOptions
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Pair
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.activity.UserMActivity
import com.perol.asdpl.pixivez.adapters.CommentAdapter
import com.perol.asdpl.pixivez.databinding.DialogCommentBinding
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.objects.argument
import com.perol.asdpl.pixivez.objects.argumentNullable
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.services.PxEZApp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException

// TODO: Refactor as Bottom Sheet
// TODO: comment select emoji
// TODO: panel helper
class CommentDialog : BaseVBDialogFragment<DialogCommentBinding>() {

    private var id: Long by argument()
    private var parent_comment_id = 1
    private val retrofitRepository = RetrofitRepository.getInstance()
    var nextUrl: String?  by argumentNullable()

    private fun getData(commentAdapter: CommentAdapter) {
        retrofitRepository.getIllustComments(id, include_total_comments = true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                Toasty.longToast("${it.comments.size}/${it.total_comments} comments in total")
                commentAdapter.setNewInstance(it.comments)
                nextUrl = it.next_url
            }, {
                it.printStackTrace()
            }, {
                binding.button.isEnabled = true
            }).add()
    }

    private fun commit(commentAdapter: CommentAdapter) {
        retrofitRepository.postIllustComment(
            id,
            binding.edittextComment.text.toString(),
            if (parent_comment_id == 1) null else parent_comment_id
        ).subscribe({
            retrofitRepository.getIllustComments(id).subscribe({
                commentAdapter.setNewInstance(it.comments)
                Toast.makeText(context, getString(R.string.comment_successful), Toast.LENGTH_SHORT).show()
                binding.edittextComment.setText("")
                parent_comment_id = 1
                binding.edittextComment.hint = ""
            }, { e ->
                when ((e as HttpException).response()!!.code()) {
                    403 -> {
                        Toasty.warning(requireContext(), getString(R.string.rate_limited), Toast.LENGTH_SHORT)
                            .show()
                    }
                    404 -> {
                        //TODO: why 404
                        e.printStackTrace()
                    }
                }
            }, {}).add()
        }, {}, {
            binding.button.isEnabled = true
        }).add()
    }

    override fun onStart() {
        super.onStart()
        val window = dialog!!.window
        val params = window!!.attributes
        params.gravity = Gravity.BOTTOM
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        // params.height = screenHeightPx()/2
        window.attributes = params
        window.setBackgroundDrawable(ColorDrawable(ThemeUtil.transparent))
    }

    override fun onCreateDialogBinding(builder: MaterialAlertDialogBuilder) {
        val commentAdapter = CommentAdapter(R.layout.view_comment_item, null)
        binding.recyclerviewComments.apply {
            adapter = commentAdapter
            isNestedScrollingEnabled = false
            layoutManager =
                GridLayoutManager(
                    context,
                    resources.configuration.orientation,
                    RecyclerView.VERTICAL,
                    false
                )
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL))
            //layoutParams.height = screenHeightPx()/2 - 50
        }
        commentAdapter.setOnItemClickListener { adapter, view, position ->
            val comment = commentAdapter.data[position].comment
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(comment)
                .show()
        }
        commentAdapter.addChildClickViewIds(R.id.commentuserimage, R.id.reply_to_hit)
        commentAdapter.setOnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.commentuserimage) {
                val options = if (PxEZApp.animationEnable) {
                    ActivityOptions.makeSceneTransitionAnimation(
                        requireActivity(),
                        Pair(view, "userimage")
                    ).toBundle()
                } else null
                UserMActivity.start(requireContext(), commentAdapter.data[position].user.id, options)
            }
            if (view.id == R.id.reply_to_hit) {
                parent_comment_id = commentAdapter.data[position].id
                binding.edittextComment.hint =
                    getString(R.string.reply_to) + ":" + commentAdapter.data[position].user.name
            }
        }
        commentAdapter.setOnLoadMoreListener {
            if (!nextUrl.isNullOrBlank()) {
                retrofitRepository.getNextIllustComments(
                    nextUrl!!
                ).subscribe({
                    commentAdapter.addData(it.comments)
                    nextUrl = it.next_url
                    commentAdapter.loadMoreComplete()
                }, {
                    commentAdapter.loadMoreFail()
                    it.printStackTrace()
                }, {}).add()
            }
            else {
                commentAdapter.loadMoreEnd()
            }
        }
        binding.button.setOnClickListener {
            if (!binding.edittextComment.text.isNullOrBlank()) {
                commit(commentAdapter)
                binding.button.isEnabled = false
            }
        }
        binding.button.isEnabled = false
        getData(commentAdapter)
    }

    companion object {
        fun newInstance(id: Long): CommentDialog {
            val commentDialog = CommentDialog()
            commentDialog.id = id
            return commentDialog
        }
    }
}
