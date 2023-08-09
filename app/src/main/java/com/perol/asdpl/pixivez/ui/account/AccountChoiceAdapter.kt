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

package com.perol.asdpl.pixivez.ui.account

import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.data.entity.UserEntity
import com.perol.asdpl.pixivez.databinding.ViewAccountItemBinding
import com.perol.asdpl.pixivez.objects.ViewBindingUtil.getBinding
import kotlinx.coroutines.runBlocking

class AccountChoiceAdapter(layoutResId: Int, data: List<UserEntity>) :
    BaseQuickAdapter<UserEntity, BaseViewHolder>(layoutResId, data.toMutableList()) {

    override fun convert(holder: BaseViewHolder, item: UserEntity) {
        val it = holder.getBinding(ViewAccountItemBinding::bind)
        Glide.with(context).load(item.userimage).circleCrop().into(it.imageviewUser)
        it.textviewUser.text = item.username
        it.textviewEmail.text = item.useremail
        val isCurrent = holder.layoutPosition == AppDataRepo.pre.getInt("usernum", 0)
        it.root.isClickable = !isCurrent
        it.imageviewDelete.apply {
            //isClickable = !isCurrent
            setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.confirm_title)
                    .setMessage(item.username)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        runBlocking {
                            AppDataRepo.deleteUser(item)
                            this@AccountChoiceAdapter.remove(item)
                        }
                    }
            }
            setImageResource(
                if (isCurrent) R.drawable.ic_check_black_24dp
                else R.drawable.ic_close_black_24dp
            )
            //colorFilter = LightingColorFilter(Color.BLACK, Color.BLACK)
        }
    }
}
