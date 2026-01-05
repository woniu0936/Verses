package com.woniu0936.verses.core

import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.woniu0936.verses.model.ItemWrapper
import com.woniu0936.verses.model.SmartViewHolder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * A high-performance [ListAdapter] that orchestrates the rendering of [ItemWrapper] units.
 * 
 * Key responsibilities:
 * 1. **Global ViewType Management**: Uses a static registry to ensure consistent IDs across instances.
 * 2. **Efficient ViewHolder Creation**: Maps ViewTypes to factories in O(1) time.
 * 3. **Shared Resource Support**: Provides a global pool for nested horizontal list optimization.
 * 4. **Safe Click Handling**: Implements a stateless click system using the modern `bindingAdapter` API.
 */
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.WeakHashMap

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
internal class VerseAdapter : ListAdapter<ItemWrapper, SmartViewHolder>(
    AsyncDifferConfig.Builder(WrapperDiffCallback)
        .setBackgroundThreadExecutor(Dispatchers.Default.asExecutor())
        .build()
) {
    companion object {
        private val globalViewTypeCache = ConcurrentHashMap<Any, Int>()
        private val globalTypeToFactory = ConcurrentHashMap<Int, (ViewGroup) -> SmartViewHolder>()
        private val contextPools = WeakHashMap<android.content.Context, RecyclerView.RecycledViewPool>()

        /**
         * Retrieves or creates a [RecyclerView.RecycledViewPool] scoped to the given [context].
         * This prevents memory leaks and ensures correct theme/styling for recycled views.
         */
        fun getPool(context: android.content.Context): RecyclerView.RecycledViewPool {
            return synchronized(contextPools) {
                contextPools.getOrPut(context) { RecyclerView.RecycledViewPool() }
            }
        }

        /**
         * Assigns or retrieves a unique, stable ViewType ID for a given layout key.
         */
        fun getGlobalViewType(key: Any, factory: (ViewGroup) -> SmartViewHolder): Int {
            return globalViewTypeCache[key] ?: synchronized(globalTypeToFactory) {
                globalViewTypeCache[key] ?: run {
                    var type = key.hashCode()
                    
                    // Linear probing: If this hashCode is already taken by a DIFFERENT key,
                    // increment until we find a free slot.
                    while (globalTypeToFactory.containsKey(type)) {
                        val existingKey = globalViewTypeCache.entries.find { it.value == type }?.key
                        if (existingKey == null || existingKey == key) break
                        type++
                    }
                    
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

        /**
         * Explicitly clears all global registries and the shared ViewPools.
         * 
         * Call this when the app is undergoing a major state change (e.g., user logout, 
         * dynamic theme switch) to release all static references to view factories 
         * and recycled views.
         */
        fun clearRegistry() {
            globalViewTypeCache.clear()
            globalTypeToFactory.clear()
            synchronized(contextPools) {
                contextPools.clear()
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).viewType

    /**
     * Delegates to the global registry to get a stable ViewType.
     */
    fun getOrCreateViewType(key: Any, factory: (ViewGroup) -> SmartViewHolder): Int {
        return getGlobalViewType(key, factory)
    }

    /**
     * Submits a new list to be diffed, and displayed.
     */
    fun submit(list: List<ItemWrapper>, commitCallback: (() -> Unit)? = null) {
        submitList(list, commitCallback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartViewHolder {
        val factory = getGlobalFactory(viewType)
        val holder = factory(parent)
        
        // High-performance Stateless Proxy Listener:
        // 1. Set ONCE during creation (Zero allocation during scroll).
        // 2. Dynamically looks up the CURRENT wrapper from the adapter when clicked.
        // 3. This allows us to ignore 'onClick' changes in DiffUtil (preventing flashes)
        //    while ensuring the latest logic is always executed.
        holder.itemView.setOnClickListener {
            val currentAdapter = holder.bindingAdapter as? VerseAdapter ?: return@setOnClickListener
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                // Fetch the LATEST wrapper from the current list
                currentAdapter.getItem(pos).onClick?.invoke()
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: SmartViewHolder, position: Int) {
        val item = getItem(position)
        
        try {
            // 1. Setup the holder with the latest data reference for itemData() access
            holder.prepare(item.data)
            
            // 2. Handle StaggeredGrid full-span items
            val params = holder.itemView.layoutParams
            if (params is StaggeredGridLayoutManager.LayoutParams) {
                if (params.isFullSpan != item.fullSpan) {
                    params.isFullSpan = item.fullSpan
                }
            }
            
            // 3. Trigger the scoped binding logic
            item.bind(holder, item.data)
            
            // 4. Update interactive state (Restore convenience onClick support)
            val isClickable = item.onClick != null
            if (holder.itemView.isClickable != isClickable) {
                holder.itemView.isClickable = isClickable
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onViewAttachedToWindow(holder: SmartViewHolder) {
        super.onViewAttachedToWindow(holder)
        val pos = holder.bindingAdapterPosition
        if (pos != RecyclerView.NO_POSITION) {
            getItem(pos).onAttach?.invoke()
        }
    }

    override fun onViewDetachedFromWindow(holder: SmartViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val pos = holder.bindingAdapterPosition
        if (pos != RecyclerView.NO_POSITION) {
            getItem(pos).onDetach?.invoke()
        }
    }

    override fun onViewRecycled(holder: SmartViewHolder) {
        super.onViewRecycled(holder)
        // Auto-clean nested adapters to prevent 'ghosting' visual artifacts.
        // We only look for direct RecyclerView children to avoid deep tree traversal.
        cleanupNestedRecyclerViews(holder.itemView)
    }

    private fun cleanupNestedRecyclerViews(view: android.view.View) {
        when (view) {
            is RecyclerView -> {
                view.adapter = null
            }
            is android.view.ViewGroup -> {
                for (i in 0 until view.childCount) {
                    cleanupNestedRecyclerViews(view.getChildAt(i))
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

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun getItem(position: Int): ItemWrapper = super.getItem(position)

    /**
     * Optimized diffing logic for [ItemWrapper] units.
     */
    object WrapperDiffCallback : DiffUtil.ItemCallback<ItemWrapper>() {
        override fun areItemsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            return oldItem.id == newItem.id
        }

        @android.annotation.SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            // By utilizing ItemWrapper's custom equals(), we ensure that:
            // 1. Data and Layout metadata are accurately compared.
            // 2. Unstable function properties (lambdas) are safely ignored.
            // 3. New properties added to ItemWrapper in the future are automatically handled.
            return oldItem == newItem
        }
    }
}