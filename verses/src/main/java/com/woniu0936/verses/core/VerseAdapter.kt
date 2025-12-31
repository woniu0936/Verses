package com.woniu0936.verses.core

import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.woniu0936.verses.model.ItemWrapper
import com.woniu0936.verses.model.SmartViewHolder
import java.util.concurrent.atomic.AtomicInteger

/**
 * A specialized [androidx.recyclerview.widget.ListAdapter] that handles [ItemWrapper] units.
 *
 * This adapter manages ViewType caching and provides helper methods for Grid and Staggered
 * layout managers to support dynamic span sizes and full-span items.
 */
@PublishedApi
internal class VerseAdapter : ListAdapter<ItemWrapper, SmartViewHolder>(WrapperDiffCallback) {

    /**
     * Cache for mapping view type keys (Inflate functions or custom IDs) to unique integer IDs.
     */
    private val viewTypeCache = mutableMapOf<Any, Int>()

    /**
     * Counter to generate unique view type IDs incrementally.
     */
    private val typeCounter = AtomicInteger(0)

    /**
     * Retrieves an existing ViewType ID or generates a new one for the given [key].
     *
     * @param key The unique key identifying a view type (usually an [com.woniu0936.verses.model.Inflate] function or a content type).
     * @return A unique integer ID for the [androidx.recyclerview.widget.RecyclerView] view pool.
     */
    fun getOrCreateViewType(key: Any): Int {
        return viewTypeCache.getOrPut(key) { typeCounter.getAndIncrement() }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartViewHolder {
        // Look up the factory from the current list using the viewType.
        // Since DiffUtil works asynchronously, we find the first item that matches this type to use its factory.
        val wrapper = currentList.first { it.viewType == viewType }
        return wrapper.factory(parent)
    }

    override fun onBindViewHolder(holder: SmartViewHolder, position: Int) {
        val item = getItem(position)
        
        // Special handling for StaggeredGridLayout: Apply full-span attribute to the LayoutParams.
        val params = holder.itemView.layoutParams
        if (params is StaggeredGridLayoutManager.LayoutParams) {
            if (params.isFullSpan != item.fullSpan) {
                params.isFullSpan = item.fullSpan
            }
        }
        
        item.bind(holder)
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
