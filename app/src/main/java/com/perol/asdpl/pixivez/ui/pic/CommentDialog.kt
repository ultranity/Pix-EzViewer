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

package com.perol.asdpl.pixivez.ui.pic

import android.app.ActivityOptions
import android.util.Pair
import android.view.Gravity
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.BaseDialogFragment
import com.perol.asdpl.pixivez.base.KotlinUtil.launchCatching
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.data.RetrofitRepository
import com.perol.asdpl.pixivez.databinding.DialogCommentBinding
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.objects.ToastQ
import com.perol.asdpl.pixivez.objects.Toasty
import com.perol.asdpl.pixivez.objects.argument
import com.perol.asdpl.pixivez.objects.argumentNullable
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.ui.user.UserMActivity
import retrofit2.HttpException

// TODO: Refactor as Bottom Sheet
// TODO: comment select emoji
// TODO: panel helper
class CommentDialog : BaseDialogFragment<DialogCommentBinding>() {

    private var pid: Int by argument()
    private var parent_comment_id = 1
    private val retrofit = RetrofitRepository.getInstance()
    var nextUrl: String? by argumentNullable()

    private fun getData(commentAdapter: CommentAdapter) {
        lifecycleScope.launchCatching({
            retrofit.api.getIllustComments(pid, 0, true)
        }, {
            ToastQ.post("${it.comments.size}/${it.total_comments} comments in total")
            commentAdapter.setNewInstance(it.comments)
            nextUrl = it.next_url
            binding.button.isEnabled = true
        }, {
            it.printStackTrace()
            binding.button.isEnabled = true
        })
    }

    private fun commit(commentAdapter: CommentAdapter) {
        lifecycleScope.launchCatching({
            retrofit.api.postIllustComment(
                pid,
                binding.edittextComment.text.toString(),
                if (parent_comment_id == 1) null else parent_comment_id
            )
        }, {
            lifecycleScope.launchCatching({ retrofit.api.getIllustComments(pid) }, {
                commentAdapter.setNewInstance(it.comments)
                Toasty.success(requireContext(), R.string.comment_successful)
                binding.edittextComment.setText("")
                parent_comment_id = 1
                binding.edittextComment.hint = ""
            }, { e ->
                when ((e as HttpException).response()!!.code()) {
                    403 -> {
                        Toasty.warning(requireContext(), R.string.rate_limited)
                    }

                    404 -> {
                        //TODO: why 404
                        e.printStackTrace()
                    }
                }
            })
        }, {
            binding.button.isEnabled = true
        })
    }

    override fun onStart() {
        super.onStart()
        val window = dialog!!.window
        val params = window!!.attributes
        params.gravity = Gravity.BOTTOM
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        // params.height = screenHeightPx()/2
        window.attributes = params
        window.setBackgroundDrawable(
            ThemeUtil.getAttrColor(requireContext(), android.R.attr.colorBackground).toDrawable()
        )
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
            MaterialDialogs(requireContext()).show {
                setMessage(comment)
            }
        }
        commentAdapter.addChildClickViewIds(R.id.commentuserimage, R.id.reply_to_hit)
        commentAdapter.setOnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.commentuserimage) {
                val options = if (PxEZApp.animationEnable) {
                    ActivityOptions.makeSceneTransitionAnimation(
                        requireActivity(),
                        Pair(view, "shared_element_container")//"userimage")
                    ).toBundle()
                } else null
                UserMActivity.start(
                    requireContext(),
                    commentAdapter.data[position].user.id,
                    options
                )
            }
            if (view.id == R.id.reply_to_hit) {
                parent_comment_id = commentAdapter.data[position].id
                binding.edittextComment.hint =
                    getString(R.string.reply_to) + ":" + commentAdapter.data[position].user.name
            }
        }
        commentAdapter.setOnLoadMoreListener {
            if (!nextUrl.isNullOrBlank()) {
                lifecycleScope.launchCatching({
                    retrofit.getNextIllustComments(nextUrl!!)
                }, {
                    commentAdapter.addData(it.comments)
                    nextUrl = it.next_url
                    commentAdapter.loadMoreComplete()
                }, {
                    commentAdapter.loadMoreFail()
                })
            } else {
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
        fun newInstance(pid: Int): CommentDialog {
            return CommentDialog().apply { this.pid = pid }
        }
    }
}
