package com.woniu0936.verses.core

import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.woniu0936.verses.model.ItemWrapper
import com.woniu0936.verses.model.SmartViewHolder
import java.util.concurrent.atomic.AtomicInteger

class VerseAdapter : ListAdapter<ItemWrapper, SmartViewHolder>(WrapperDiffCallback) {

    // ViewType Cache Pool (Key -> Int ID)
    // Key is usually Inflate function reference, or user specified contentType
    private val viewTypeCache = mutableMapOf<Any, Int>()
    private val typeCounter = AtomicInteger(0)

    /**
     * Get or generate ViewType ID
     * Ensures same Inflate function maps to same ID across renders for ViewHolder reuse
     */
    fun getOrCreateViewType(key: Any): Int {
        return viewTypeCache.getOrPut(key) { typeCounter.getAndIncrement() }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartViewHolder {
        // Find Factory based on ViewType (from a sample in current list)
        // We look through currentList to find a wrapper that matches the viewType.
        val wrapper = currentList.first { it.viewType == viewType }
        return wrapper.factory(parent)
    }

    override fun onBindViewHolder(holder: SmartViewHolder, position: Int) {
        val item = getItem(position)
        
        // Special handling: FullSpan for StaggeredGridLayoutManager
        val params = holder.itemView.layoutParams
        if (params is StaggeredGridLayoutManager.LayoutParams) {
            if (params.isFullSpan != item.fullSpan) {
                params.isFullSpan = item.fullSpan
            }
        }
        
        item.bind(holder)
    }

    // Helper for GridLayoutManager
    fun getSpanSize(position: Int, totalSpan: Int): Int {
        if (position !in 0 until itemCount) return 1
        val item = getItem(position)
        return if (item.fullSpan) totalSpan else item.spanSize
    }

    // Smart Diff Strategy
    object WrapperDiffCallback : DiffUtil.ItemCallback<ItemWrapper>() {
        override fun areItemsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            // As long as data content hasn't changed, don't trigger re-bind (critical for performance)
            return oldItem.data == newItem.data
        }
    }
}
