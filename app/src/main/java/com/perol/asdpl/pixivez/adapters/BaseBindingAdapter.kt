package com.perol.asdpl.pixivez.adapters

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.perol.asdpl.pixivez.adapters.BaseBindingAdapter.BaseVBViewHolder
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass

// from https://github.com/CymChad/BaseRecyclerViewAdapterHelper/issues/3138
inline fun <reified VB : ViewBinding> Activity.inflate() = lazy {
    inflateBinding<VB>(layoutInflater).apply { setContentView(root) }
}

inline fun <reified VB : ViewBinding> Dialog.inflate() = lazy {
    inflateBinding<VB>(layoutInflater).apply { setContentView(root) }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified VB : ViewBinding> inflateBinding(layoutInflater: LayoutInflater) =
    VB::class.java.getMethod("inflate", LayoutInflater::class.java).invoke(null, layoutInflater) as VB

@Suppress("UNCHECKED_CAST")
fun <VB : ViewBinding> BaseViewHolder.getBinding(bind: (View) -> VB): VB =
    itemView.getTag(Int.MIN_VALUE) as? VB ?: bind(itemView).also { itemView.setTag(Int.MIN_VALUE, it) }

abstract class BaseBindingAdapter<T, VB : ViewBinding>(layoutResId: Int, VBClass: KClass<*>, data: List<T>?) :
    BaseQuickAdapter<T, BaseVBViewHolder<VB>>(layoutResId, data?.toMutableList()) {
    private val vbClass = VBClass.java

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseVBViewHolder<VB> {
        return BaseVBViewHolder(getViewBinding(LayoutInflater.from(parent.context), parent)!!)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun getViewBinding(inflater: LayoutInflater?, parent: ViewGroup?): VB? {
        var binding: VB? = null
        try {
            val method = vbClass.getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.javaPrimitiveType
            )
            binding = method.invoke(null, inflater, parent, false) as VB
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        return binding
    }

    class BaseVBViewHolder<VB : ViewBinding>(bd: VB) : BaseViewHolder(bd.root) {
        var binding: VB = bd
    }
}
