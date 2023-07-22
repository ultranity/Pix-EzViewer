package com.perol.asdpl.pixivez.base.factory

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import java.util.Timer
import kotlin.collections.set
import kotlin.concurrent.schedule


// designed by lucasDev: https://www.jianshu.com/p/79d70b2712ee
val VMStores = HashMap<String, VMStore>()

// var VMFactory = ViewModelProvider.NewInstanceFactory()
inline fun <reified VM : ViewModel> Fragment.activitySharedViewModel(
    scopeName: String,
    holder: LifecycleOwner? = null,
    factory: ViewModelProvider.Factory? = null
): Lazy<VM> {
    val store: VMStore
    if (VMStores.keys.contains(scopeName)) {
        store = VMStores[scopeName]!!
    }
    else {
        store = VMStore()
        VMStores[scopeName] = store
    }
    store.register(holder ?: requireActivity())
    return ViewModelLazy(VM::class, { store.viewModelStore }, { factory ?: ViewModelProvider.NewInstanceFactory() })
}

// Add clearBindDelay to fix rotation data loss
inline fun <reified VM : ViewModel> LifecycleOwner.sharedViewModel(
    scopeName: String,
    holder: LifecycleOwner? = null,
    factory: ViewModelProvider.Factory? = null,
    clearBindDelay: Long = 1000
): Lazy<VM> {
    val store: VMStore
    if (VMStores.keys.contains(scopeName)) {
        store = VMStores[scopeName]!!
    }
    else {
        store = VMStore(clearBindDelay)
        VMStores[scopeName] = store
    }
    store.register(holder ?: this)
    return ViewModelLazy(VM::class, { store.viewModelStore }, { factory ?: ViewModelProvider.NewInstanceFactory() })
}
class VMStore(private val clearBindDelay: Long = 1000) : ViewModelStoreOwner {

    private val bindTargets = ArrayList<LifecycleOwner>()
    private var vmStore: ViewModelStore? = null

    fun isChangingConfigurations(owner: LifecycleOwner): Boolean {
        if (owner is Fragment) {
            val activity = owner.activity ?: return false
            return activity.isChangingConfigurations
        }
        return if (owner is FragmentActivity) {
            owner.isChangingConfigurations
        } else false
    }
    fun register(host: LifecycleOwner) {
        if (!bindTargets.contains(host)) {
            bindTargets.add(host)
            host.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        host.lifecycle.removeObserver(this)
                        bindTargets.remove(host)
                        Timer().schedule(clearBindDelay) {
                            if (bindTargets.isEmpty()) { // 如果当前没有关联对象
                                //check if is restoring: https://blog.csdn.net/huweijian5/article/details/114575986
                                if (isChangingConfigurations(source))
                                    return@schedule
                                VMStores.entries.find { it.value == this@VMStore }?.also {
                                    vmStore?.clear()
                                    VMStores.remove(it.key)
                                }
                            }
                        }
                    }
                }
            })
        }
    }

    override val viewModelStore: ViewModelStore
        get() {
            if (vmStore == null) {
                vmStore = ViewModelStore()
            }
            return vmStore!!
        }
}
