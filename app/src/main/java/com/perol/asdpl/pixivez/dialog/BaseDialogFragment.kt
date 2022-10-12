package com.perol.asdpl.pixivez.dialog

import androidx.fragment.app.DialogFragment
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

open class BaseDialogFragment : DialogFragment() {
    val disposables = CompositeDisposable()
    fun Disposable.add() {
        disposables.add(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}