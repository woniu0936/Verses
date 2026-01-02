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
            return globalViewTypeCache.getOrPut(key) {
                val type = globalTypeCounter.getAndIncrement()
                globalTypeToFactory[type] = factory
                type
            }
        }

        /**
         * Retrieves the creation factory for a specific ViewType ID.
         */
        fun getGlobalFactory(viewType: Int): (ViewGroup) -> SmartViewHolder {
            return globalTypeToFactory[viewType] ?: throw IllegalStateException("No factory registered for viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartViewHolder {
        val holder = getGlobalFactory(viewType)(parent)
        
        // High-performance Click Listener:
        // Set once during creation to minimize object allocation.
        // Uses bindingAdapter to backtrack the data at the moment of the click.
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
        
        // Update interactive state without re-setting listeners
        holder.itemView.isClickable = item.onClick != null
        
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
        // Auto-clean nested adapters to prevent 'ghosting' visual artifacts
        cleanupView(holder.itemView)
    }

    /**
     * Recursively cleans up nested [RecyclerView] instances when a holder is recycled.
     *
     * This is vital for nested horizontal lists. By clearing the nested adapter's list,
     * we prevent "ghosting" (where a recycled view briefly shows old data from a previous
     * row before being updated).
     */
    private fun cleanupView(view: android.view.View) {
        if (view is RecyclerView) {
            (view.adapter as? VerseAdapter)?.submitList(null)
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                cleanupView(view.getChildAt(i))
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