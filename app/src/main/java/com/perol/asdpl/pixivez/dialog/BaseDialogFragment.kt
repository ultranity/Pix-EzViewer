package com.perol.asdpl.pixivez.dialog

import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
open class BaseDialogFragment : DialogFragment() {
    private val disposables = CompositeDisposable()
    fun Disposable.add() {
        disposables.add(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}