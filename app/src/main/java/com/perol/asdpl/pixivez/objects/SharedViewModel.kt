package com.perol.asdpl.pixivez.objects

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import java.util.*
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

    fun register(host: LifecycleOwner) {
        if (!bindTargets.contains(host)) {
            bindTargets.add(host)
            host.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        host.lifecycle.removeObserver(this)
                        bindTargets.remove(host)
                        Timer().schedule(clearBindDelay) {
                            if (bindTargets.isEmpty()) { // 如果当前没有关联对象，则释放资源
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

    override fun getViewModelStore(): ViewModelStore {
        if (vmStore == null) {
            vmStore = ViewModelStore()
        }
        return vmStore!!
    }
}
