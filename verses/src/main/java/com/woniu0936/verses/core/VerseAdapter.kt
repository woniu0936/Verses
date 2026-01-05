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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.ConcurrentModificationException
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * An industrial-grade [ListAdapter] that orchestrates the rendering of [ItemWrapper] units.
 *
 * VerseAdapter is designed for maximum performance and memory safety. It leverages a global
 * static registry for ViewTypes to ensure consistent IDs across multiple RecyclerView instances,
 * enabling seamless [RecyclerView.RecycledViewPool] sharing.
 *
 * Internal Architecture:
 * 1. **Deterministic ViewTypes**: Uses linear probing to resolve hashCode collisions.
 * 2. **Context-Scoped Pools**: Prevents theme pollution and Context leaks via [WeakHashMap].
 * 3. **Proxy Event System**: Statelessly routes clicks to the latest data to avoid re-binding flashes.
 */
@PublishedApi
internal class VerseAdapter : ListAdapter<ItemWrapper, SmartViewHolder>(
    AsyncDifferConfig.Builder(WrapperDiffCallback)
        .setBackgroundThreadExecutor(Dispatchers.Default.asExecutor())
        .build()
) {
    companion object {
        /**
         * Maps layout keys (usually Class objects) to stable integer ViewTypes.
         */
        private val globalViewTypeCache = ConcurrentHashMap<Any, Int>()

        /**
         * Maps integer ViewTypes back to the factories that create their ViewHolders.
         */
        private val globalTypeToFactory = ConcurrentHashMap<Int, (ViewGroup) -> SmartViewHolder>()

        /**
         * Stores RecycledViewPool instances scoped to specific Contexts to prevent leaks.
         */
        private val contextPools = WeakHashMap<android.content.Context, RecyclerView.RecycledViewPool>()

        /**
         * Returns a [RecyclerView.RecycledViewPool] tied to the provided [context].
         * If no pool exists, a new one is created and cached.
         *
         * @param context The context (typically an Activity) used as the lookup key.
         */
        fun getPool(context: android.content.Context): RecyclerView.RecycledViewPool {
            return synchronized(contextPools) {
                contextPools.getOrPut(context) { RecyclerView.RecycledViewPool() }
            }
        }

        /**
         * Generates or retrieves a unique, stable ViewType ID for a layout key.
         * 
         * Time Complexity: O(1) average, O(N) worst case (linear probing).
         *
         * @param key The unique key representing the view layout (e.g., ViewBinding class).
         * @param factory The factory lambda used to create the holder if the ViewType is new.
         */
        fun getGlobalViewType(key: Any, factory: (ViewGroup) -> SmartViewHolder): Int {
            return globalViewTypeCache[key] ?: synchronized(globalTypeToFactory) {
                globalViewTypeCache[key] ?: run {
                    var type = key.hashCode()
                    
                    // Linear probing to resolve rare hashCode collisions in the global registry.
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
         * @throws IllegalStateException if no factory is registered for the given [viewType].
         */
        fun getGlobalFactory(viewType: Int): (ViewGroup) -> SmartViewHolder {
            return globalTypeToFactory[viewType] ?: throw IllegalStateException(
                "No factory registered for viewType $viewType. Ensure registry is not cleared prematurely."
            )
        }

        /**
         * Explicitly purges all global registries and context-scoped pools.
         * Call this during major app state changes (e.g., Logout) to release static references.
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
     * Delegates ViewType generation to the global static registry.
     */
    fun getOrCreateViewType(key: Any, factory: (ViewGroup) -> SmartViewHolder): Int {
        return getGlobalViewType(key, factory)
    }

    /**
     * Submits a new list of items to be asynchronously diffed and displayed.
     */
    fun submit(list: List<ItemWrapper>, commitCallback: (() -> Unit)? = null) {
        submitList(list, commitCallback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartViewHolder {
        val factory = getGlobalFactory(viewType)
        val holder = factory(parent)
        
        // Proxy Listener: We attach the listener ONCE during creation.
        // It dynamically looks up the latest onClick lambda from the current list,
        // allowing us to ignore onClick instances in DiffUtil contents equality.
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
        
        try {
            // 1. Update stateful context
            holder.prepare(item.data)
            
            // 2. Handle layout metadata for Staggered grids
            val params = holder.itemView.layoutParams
            if (params is StaggeredGridLayoutManager.LayoutParams) {
                if (params.isFullSpan != item.fullSpan) {
                    params.isFullSpan = item.fullSpan
                }
            }
            
            // 3. Execute declarative binding logic
            item.bind(holder, item.data)
            
            // 4. Update interactivity
            val isClickable = item.onClick != null
            if (holder.itemView.isClickable != isClickable) {
                holder.itemView.isClickable = isClickable
            }
        } catch (e: Exception) {
            // Guard against accidental crashes in user-provided bind blocks
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
        // Clean up nested adapters to prevent ghosting artifacts in horizontal lists.
        cleanupNestedRecyclerViews(holder.itemView)
    }

    private fun cleanupNestedRecyclerViews(view: android.view.View) {
        when (view) {
            is RecyclerView -> view.adapter = null
            is android.view.ViewGroup -> {
                for (i in 0 until view.childCount) {
                    cleanupNestedRecyclerViews(view.getChildAt(i))
                }
            }
        }
    }

    /**
     * Determines the span size for a given position.
     * Used by [androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup].
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
     * Relies on [ItemWrapper.equals] which safely excludes function properties.
     */
    object WrapperDiffCallback : DiffUtil.ItemCallback<ItemWrapper>() {
        override fun areItemsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            return oldItem.id == newItem.id
        }

        @android.annotation.SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            return oldItem == newItem
        }
    }
}
