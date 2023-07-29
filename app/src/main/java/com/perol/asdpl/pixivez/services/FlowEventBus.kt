package com.perol.asdpl.pixivez.services

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withStateAtLeast
import com.perol.asdpl.pixivez.base.KotlinUtil.Int
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

sealed class Event {
    data class BlockTagsChanged(val blockTags: List<String>) : Event()
}

/** to add ref: https://github.com/biubiuqiu0/flow-event-bus
 * 作者：yollpoll,HxBuer
 * 链接：https://juejin.cn/post/7054462654902960135, https://juejin.cn/post/7091547413076246565
 * 来源：稀土掘金
 */
object FlowEventBus {
    private val normalBus: HashMap<String, MutableSharedFlow<out Any>> = hashMapOf()
    private val stickyBus: HashMap<String, MutableSharedFlow<out Any>> = hashMapOf()

    private fun <T : Any> with(key: String, isSticky: Boolean = false): MutableSharedFlow<T> {
        val bus = if (isSticky) stickyBus else normalBus
        return (bus[key] ?: MutableSharedFlow<T>(
            replay = isSticky.Int(),
            extraBufferCapacity = Int.MAX_VALUE //避免挂起导致数据发送失败
        ).also { bus[key] = it }) as MutableSharedFlow<T>
    }

    /**
     * 对外只暴露SharedFlow
     * @param action String
     * @return SharedFlow<T>
     */
    fun <T> getFlow(action: String): SharedFlow<T> {
        return with(action)
    }


    fun <T : Any> post(
        event: T,
        delay: Long = 0,
        isSticky: Boolean = false,
        scope: CoroutineDispatcher = Dispatchers.Main
    ) {
        CoroutineScope(scope).launch {
            delay(delay)
            with<T>(event.javaClass.simpleName, isSticky).emit(event)
        }
    }

    /**
     * 挂起函数
     * @param action String
     * @param data T
     */
    suspend fun <T : Any> post(action: String, data: T) {
        with<T>(action).emit(data)
    }

    /**
     * 详见tryEmit和emit的区别
     * @param action String
     * @param data T
     * @return Boolean
     */
    fun <T : Any> tryPost(action: String, data: T): Boolean {
        return with<T>(action).tryEmit(data)
    }

    /**
     * sharedFlow会长久持有，所以要加声明周期限定，不然会出现内存溢出
     * @param lifecycle Lifecycle
     * @param action String
     * @param block Function1<T, Unit>
     */
    suspend fun <T : Any> subscribe(lifecycle: Lifecycle, action: String, block: (T) -> Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
            with<T>(action).collect {
                block(it)
            }
        }
    }

    /**
     * 注意，使用这个方法需要将协程在合适的时候取消，否则会导致内存溢出
     * @param action String
     * @param block Function1<T, Unit>
     */
    suspend fun <T : Any> subscribe(action: String, block: (T) -> Unit) {
        with<T>(action).collect {
            block(it)
        }
    }

    //Lifecycle.State参数可以更精细地将控制接受到事件时的执行时机
    inline fun <reified T : Any> observe(
        lifecycleOwner: LifecycleOwner,
        minState: Lifecycle.State = Lifecycle.State.CREATED, //Lifecycle.State.RESUMED,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        crossinline onReceived: (T) -> Unit
    ) = lifecycleOwner.lifecycleScope.launch(dispatcher) {
        getFlow<T>(T::class.java.simpleName).collect {
            lifecycleOwner.lifecycle.withStateAtLeast(minState) {
                onReceived(it)
            }
        }
    }
}