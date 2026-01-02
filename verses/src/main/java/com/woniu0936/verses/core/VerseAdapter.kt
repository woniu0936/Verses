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

/**
 * A high-performance [ListAdapter] that orchestrates the rendering of [ItemWrapper] units.
 * 
 * Key responsibilities:
 * 1. **Global ViewType Management**: Uses a static registry to ensure consistent IDs across instances.
 * 2. **Efficient ViewHolder Creation**: Maps ViewTypes to factories in O(1) time.
 * 3. **Shared Resource Support**: Provides a global pool for nested horizontal list optimization.
 * 4. **Safe Click Handling**: Implements a stateless click system using the modern `bindingAdapter` API.
 */
@PublishedApi
internal class VerseAdapter : ListAdapter<ItemWrapper, SmartViewHolder>(WrapperDiffCallback) {
    companion object {
        private val globalViewTypeCache = ConcurrentHashMap<Any, Int>()
        private val globalTypeToFactory = ConcurrentHashMap<Int, (ViewGroup) -> SmartViewHolder>()
        private val globalTypeCounter = AtomicInteger(1000)

        /**
         * A shared [RecyclerView.RecycledViewPool] used automatically by the library to optimize
         * nested structures and cross-page navigation.
         */
        val globalPool = RecyclerView.RecycledViewPool()

        /**
         * Assigns or retrieves a unique, stable ViewType ID for a given layout key.
         * Stability is critical for safe sharing of [globalPool].
         */
        fun getGlobalViewType(key: Any, factory: (ViewGroup) -> SmartViewHolder): Int {
            return globalViewTypeCache[key] ?: synchronized(globalTypeToFactory) {
                // Double-checked locking for thread-safe Type generation
                globalViewTypeCache[key] ?: run {
                    val type = globalTypeCounter.getAndIncrement()
                    globalTypeToFactory[type] = factory
                    globalViewTypeCache[key] = type
                    type
                }
            }
        }

        /**
         * Retrieves the creation factory for a specific ViewType ID.
         */
        fun getGlobalFactory(viewType: Int): (ViewGroup) -> SmartViewHolder {
            return globalTypeToFactory[viewType] ?: synchronized(globalTypeToFactory) {
                globalTypeToFactory[viewType]
            } ?: throw IllegalStateException("No factory registered for viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartViewHolder {
        val factory = getGlobalFactory(viewType)
        val holder = factory(parent)
        
        // High-performance Click Listener:
        // Using a stateless listener capture to prevent memory leaks and redundant object creation.
        holder.itemView.setOnClickListener {
            val currentAdapter = holder.bindingAdapter as? VerseAdapter ?: return@setOnClickListener
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                currentAdapter.getItem(pos).onClick?.invoke()
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: SmartViewHolder, position: Int) {
        val item = getItem(position)
        
        // Update interactive state efficiently
        val isClickable = item.onClick != null
        if (holder.itemView.isClickable != isClickable) {
            holder.itemView.isClickable = isClickable
        }
        
        // Handle StaggeredGrid full-span items
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
        // Auto-clean nested adapters to prevent 'ghosting' visual artifacts.
        // We only look for direct RecyclerView children to avoid deep tree traversal.
        cleanupNestedRecyclerViews(holder.itemView)
    }

    private fun cleanupNestedRecyclerViews(view: android.view.View) {
        if (view is RecyclerView) {
            (view.adapter as? VerseAdapter)?.submitList(null)
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is RecyclerView) {
                    (child.adapter as? VerseAdapter)?.submitList(null)
                }
            }
        }
    }

    /**
     * Helper for GridLayoutManager to determine item span size.
     */
    fun getSpanSize(position: Int, totalSpan: Int): Int {
        if (position !in 0 until itemCount) return 1
        val item = getItem(position)
        return if (item.fullSpan) totalSpan else item.span
    }

    /**
     * Optimized diffing logic for [ItemWrapper] units.
     */
    object WrapperDiffCallback : DiffUtil.ItemCallback<ItemWrapper>() {
        override fun areItemsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            return oldItem.id == newItem.id
        }

        @android.annotation.SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            // We compare the raw data objects. Users are encouraged to use data classes 
            // for their models to ensure efficient and correct diffing.
            return oldItem.data == newItem.data
        }
    }
}