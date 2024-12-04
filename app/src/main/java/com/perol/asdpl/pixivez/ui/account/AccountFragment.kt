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

package com.perol.asdpl.pixivez.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.data.entity.UserEntity
import com.perol.asdpl.pixivez.databinding.FragmentAccountBinding
import com.perol.asdpl.pixivez.networks.RefreshToken
import com.perol.asdpl.pixivez.objects.ClipBoardUtil
import com.perol.asdpl.pixivez.objects.ToastQ
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnLogin.setOnClickListener {
            startActivity(
                Intent(requireContext(), LoginActivity::class.java)
                    .setAction("login.try")
            )
        }
        binding.btnLogout.setOnClickListener {
            MaterialDialogs(requireContext()).show {
                setTitle(R.string.logoutallaccount)
                confirmButton { i, j ->
                    runBlocking {
                        AppDataRepo.deleteAllUser()
                    }
                    startActivity(
                        Intent(requireContext(), LoginActivity::class.java)
                            .setAction("login.try")
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) // Clear task stack.
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                cancelButton()
            }
        }
        binding.recyclerviewAccount.layoutManager = LinearLayoutManager(requireContext())
        runBlocking {
            val users = AppDataRepo.getAllUser()
            binding.recyclerviewAccount.adapter = AccountChoiceAdapter(
                R.layout.view_account_item, users
            ).apply {
                setOnItemClickListener { adapter, view, position ->
                    this.notifyItemChanged(AppDataRepo.pre.getInt("usernum"))
                    AppDataRepo.pre.setInt("usernum", position)
                    AppDataRepo.setCurrentUser(users[position])
                    this.notifyItemChanged(position)
                    PxEZApp.ActivityCollector.recreate()
                }
                setOnItemLongClickListener { adapter, view, position ->
                    showTokenDialog(context, users[position])
                    true
                }
            }
        }
    }

    private fun showTokenDialog(context: Context, user: UserEntity) {
        val userToken = user.Refresh_token
        MaterialAlertDialogBuilder(context)
            .setTitle("Token")
            .setMessage(R.string.token_warning)
            .setNeutralButton(R.string.refresh_token) { _, _ ->
                if (BuildConfig.DEBUG) {
                    AppDataRepo.currentUser.Authorization = "invalid_token"
                    CoroutineScope(Dispatchers.Main).launch {
                        AppDataRepo.updateUser(AppDataRepo.currentUser)
                    }
                } else {
                    try {
                        RefreshToken.getInstance().refreshToken()
                    } catch (e: Exception) {
                        ToastQ.post(R.string.refresh_token_fail)
                    }
                }
            }
            .setNegativeButton("SHOW") { _, _ ->
                MaterialAlertDialogBuilder(context)
                    .setTitle("Token|OAuth")
                    .setMessage(userToken + "|${user.Authorization}")
                    .show()
            }
            .setPositiveButton(androidx.preference.R.string.copy) { _, _ ->
                ClipBoardUtil.putTextIntoClipboard(
                    context,
                    userToken, false, "PxEz Token"
                )
            }.show()
    }
}
