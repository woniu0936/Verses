package com.woniu0936.verses.core

import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.woniu0936.verses.model.ItemWrapper
import com.woniu0936.verses.model.SmartViewHolder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A specialized [androidx.recyclerview.widget.ListAdapter] that handles [ItemWrapper] units.
 *
 * This adapter manages ViewType caching globally to ensure that same layouts have the same 
 * ViewType ID across different instances, which is a prerequisite for safe RecycledViewPool sharing.
 */
@PublishedApi
internal class VerseAdapter : ListAdapter<ItemWrapper, SmartViewHolder>(WrapperDiffCallback) {

    companion object {
        private val globalViewTypeCache = ConcurrentHashMap<Any, Int>()
        private val globalTypeToFactory = ConcurrentHashMap<Int, (ViewGroup) -> SmartViewHolder>()
        private val globalTypeCounter = AtomicInteger(1000)

        /**
         * A pre-configured global pool for sharing ViewHolders across multiple RecyclerViews.
         * Recommended for nested horizontal rows using the same item types.
         */
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
        
        // Performance Optimization: Set listener once in onCreate.
        // We use 'bindingAdapter' to dynamically find the adapter currently using this holder.
        // This is much cleaner than using tags and is safer for RecycledViewPool sharing.
        holder.itemView.setOnClickListener {
            val position = holder.bindingAdapterPosition
            val adapter = holder.bindingAdapter as? VerseAdapter
            if (position != RecyclerView.NO_POSITION && adapter != null) {
                val wrapper = adapter.getItem(position)
                // Simply invoke the baked closure. No data passing needed!
                wrapper.onClick?.invoke()
            }
        }
        
        return holder
    }

    override fun onBindViewHolder(holder: SmartViewHolder, position: Int) {
        val item = getItem(position)
        
        // Just update the clickable state, no tag needed.
        holder.itemView.isClickable = item.onClick != null
        
        // Special handling for StaggeredGridLayout: Apply full-span attribute to the LayoutParams.
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
        // No need to null out the listener because it's set in onCreate and
        // doesn't hold stale references to data. Just cleanup the view state.
        cleanupView(holder.itemView)
    }

    /**
     * Recursively clears nested adapters to prevent stale data (ghosting).
     * This is internal library logic to keep the user API clean.
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
     * Calculates the span size for a given position.
     *
     * @param position The adapter position of the item.
     * @param totalSpan The total span count of the [androidx.recyclerview.widget.GridLayoutManager].
     * @return The number of spans this item should occupy.
     */
    fun getSpanSize(position: Int, totalSpan: Int): Int {
        if (position !in 0 until itemCount) return 1
        val item = getItem(position)
        return if (item.fullSpan) totalSpan else item.span
    }

    /**
     * Internal [androidx.recyclerview.widget.DiffUtil.ItemCallback] for comparing [ItemWrapper]s.
     */
    object WrapperDiffCallback : DiffUtil.ItemCallback<ItemWrapper>() {
        override fun areItemsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            return oldItem.data == newItem.data
        }
    }
}
