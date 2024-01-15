package com.perol.asdpl.pixivez.base

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.perol.asdpl.pixivez.objects.ViewBindingUtil

abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {
    open val TAG: String = this::class.java.simpleName
    private var _binding: VB? = null
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    val binding: VB
        get() = requireNotNull(_binding) { "The property of binding has been destroyed." }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    abstract fun onCreateDialogBinding(builder: MaterialAlertDialogBuilder)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                handler.post { _binding = null }
            }
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (_binding == null) {
            _binding = ViewBindingUtil.inflateWithGeneric(this, layoutInflater, null, false)
        }
        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setView(binding.root)
        onCreateDialogBinding(builder)
        return builder.create()
    }
}