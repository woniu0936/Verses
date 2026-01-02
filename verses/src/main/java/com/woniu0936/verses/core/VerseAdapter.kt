package com.woniu0936.verses.core

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.woniu0936.verses.model.ItemWrapper
import com.woniu0936.verses.model.SmartViewHolder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@PublishedApi
internal class VerseAdapter : ListAdapter<ItemWrapper, SmartViewHolder>(WrapperDiffCallback) {
    companion object {
        private val globalViewTypeCache = ConcurrentHashMap<Any, Int>()
        private val globalTypeToFactory = ConcurrentHashMap<Int, (ViewGroup) -> SmartViewHolder>()
        private val globalTypeCounter = AtomicInteger(1000)
        val globalPool = RecyclerView.RecycledViewPool()

        fun getGlobalViewType(key: Any, factory: (ViewGroup) -> SmartViewHolder): Int {
            return globalViewTypeCache.getOrPut(key) {
                val type = globalTypeCounter.getAndIncrement()
                globalTypeToFactory[type] = factory
                type
            }
        }

        fun getGlobalFactory(viewType: Int): (ViewGroup) -> SmartViewHolder {
            return globalTypeToFactory[viewType] ?: throw IllegalStateException("No factory registered for viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartViewHolder {
        val holder = getGlobalFactory(viewType)(parent)
        holder.itemView.setOnClickListener {
            val position = holder.bindingAdapterPosition
            val adapter = holder.bindingAdapter as? VerseAdapter
            if (position != RecyclerView.NO_POSITION && adapter != null) {
                val wrapper = adapter.getItem(position)
                wrapper.onClick?.invoke()
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: SmartViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.isClickable = item.onClick != null
        val params = holder.itemView.layoutParams
        if (params is StaggeredGridLayoutManager.LayoutParams) {
            if (params.isFullSpan != item.fullSpan) {
                params.isFullSpan = item.fullSpan
            }
        }
        item.bind(holder)
    }

    override fun onViewRecycled(holder: SmartViewHolder) {
        super.onViewRecycled(holder)
        cleanupView(holder.itemView)
    }

    private fun cleanupView(view: android.view.View) {
        if (view is RecyclerView) {
            (view.adapter as? VerseAdapter)?.submitList(null)
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                cleanupView(view.getChildAt(i))
            }
        }
    }

    fun getSpanSize(position: Int, totalSpan: Int): Int {
        if (position !in 0 until itemCount) return 1
        val item = getItem(position)
        return if (item.fullSpan) totalSpan else item.span
    }

    object WrapperDiffCallback : DiffUtil.ItemCallback<ItemWrapper>() {
        override fun areItemsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            return oldItem.data == newItem.data
        }
    }
}
