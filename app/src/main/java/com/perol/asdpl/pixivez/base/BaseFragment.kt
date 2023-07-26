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

package com.perol.asdpl.pixivez.base

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.perol.asdpl.pixivez.objects.ViewBindingUtil
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.runBlocking
import java.util.LinkedList

abstract class BaseFragment : Fragment() {
    var isR18on = false
    var blockTags = emptyList<String>()
    var isLoaded = false

    override fun onResume() {
        super.onResume()
        if (!isLoaded) {
            isLoaded = true
            loadData()
        }
    }

    protected abstract fun loadData()

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isR18on = PxEZApp.instance.pre.getBoolean("r18on", false)
        try {
            runBlocking {
                blockTags = LinkedList<String>()//BlockViewModel.getAllTags()
                //if (blockTags.isEmpty()) blockTags = emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

abstract class BaseVBFragment<VB : ViewBinding> : BaseFragment() {
    open val TAG: String = this::class.java.simpleName
    private var _binding: VB? = null
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    val binding: VB
        get() = requireNotNull(_binding) { "The property of binding has been destroyed." }

    //abstract fun onCreateBinding(binding: VB)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /* viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                handler.post { _binding = null }
            }
        }) */
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (_binding == null) {
            _binding = ViewBindingUtil.inflateWithGeneric(this, layoutInflater, null, false)
        }
        //onCreateBinding(binding)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}