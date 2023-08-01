package com.perol.asdpl.pixivez.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.mikepenz.fastadapter.ClickListener
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.drag.ItemTouchCallback
import com.mikepenz.fastadapter.drag.SimpleDragCallback
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.mikepenz.fastadapter.utils.DragDropUtil
import com.perol.asdpl.pixivez.objects.ViewBindingUtil

class BaseItemAdapter<Item : GenericItem> : ItemAdapter<Item>() {
    lateinit var data: MutableList<Any>
    override fun set(items: List<Item>): ModelAdapter<Item, Item> {
        data = items as MutableList<Any>
        return super.add(items)
    }
}

open class BaseBindingItem<VB : ViewBinding> : AbstractBindingItem<VB>() {
    override val type: Int = -1

    private var onBind: ((binding: VB, payloads: List<Any>) -> Unit)? = null
    fun BaseBindingItem<VB>.onBind(block: (binding: VB, payloads: List<Any>) -> Unit) {
        onBind = block
    }

    override fun bindView(binding: VB, payloads: List<Any>) {
        onBind?.invoke(binding, payloads)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): VB {
        return ViewBindingUtil.inflateWithGeneric(this, inflater, null, false)
    }
}
/**
 * Created by mikepenz on 25.02.16.
 */
typealias ClickListener<Item> = (v: View?, adapter: ItemAdapter<Item>, item: Item, position: Int) -> Boolean

fun <T : GenericItem> FastAdapter<T>.onClick(listener: ClickListener<T>): FastAdapter<T> {
    onClickListener = listener
    return this
}

fun <T : GenericItem> FastAdapter<T>.onItemClick(
    id: Int,
    listener: ClickListener<T>
): FastAdapter<T> {
    addEventHook(object : ClickEventHook<T>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return viewHolder.itemView.findViewById(id)
        }

        override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<T>, item: T) {
            fastAdapter.getAdapter(position)?.let { listener.invoke(v, it, item, position) }
        }
    })
    return this
}

fun <T : GenericItem> RecyclerView.setDragCallback(adapter: BaseItemAdapter<T>): RecyclerView {
    val dragCallback = SimpleDragCallback(object : ItemTouchCallback {
        override fun itemTouchOnMove(oldPosition: Int, newPosition: Int): Boolean {
            DragDropUtil.onMove(adapter, oldPosition, newPosition)
            return true
        }

        override fun itemTouchDropped(oldPosition: Int, newPosition: Int) {
            // save the new item order, i.e. in your database
            // remove visual highlight to dropped item
            adapter.data.add(newPosition, adapter.data.removeAt(oldPosition))
        }
    })
    val touchHelper = ItemTouchHelper(dragCallback)
    touchHelper.attachToRecyclerView(this)
    return this
}

fun <Item : GenericItem> ItemAdapter<Item>.setList(items: List<Item>): ItemAdapter<Item> {
    set(items)
    return this
}

fun <T : GenericItem> RecyclerView.setup(block: RecyclerView.() -> ItemAdapter<T>): FastAdapter<T> {
    return FastAdapter.with(block(this)).also {
        this.adapter = it
    }
}

/*fun <T:GenericItem> RecyclerView.setup(block: RecyclerView.() -> Collection<ItemAdapter<T>>): FastAdapter<T> {
    return FastAdapter.with(block(this)).also{
        this.adapter = it
    }
}*/

fun RecyclerView.linear(vertical: Boolean = true, reverse: Boolean = false): RecyclerView {
    layoutManager = LinearLayoutManager(
        context,
        if (vertical) LinearLayoutManager.VERTICAL else LinearLayoutManager.HORIZONTAL,
        reverse
    )
    return this
}

fun RecyclerView.grid(
    spanCount: Int = 2,
    vertical: Boolean = false,
    reverse: Boolean = false
): RecyclerView {
    layoutManager = GridLayoutManager(
        context,
        spanCount,
        if (vertical) GridLayoutManager.VERTICAL else GridLayoutManager.HORIZONTAL,
        reverse
    )
    return this
}

fun RecyclerView.staggeredGrid(
    spanCount: Int = 2,
    vertical: Boolean = false,
    reverse: Boolean = false
): RecyclerView {
    layoutManager = StaggeredGridLayoutManager(
        spanCount,
        if (vertical) StaggeredGridLayoutManager.VERTICAL else StaggeredGridLayoutManager.HORIZONTAL
    )
    return this
}