package com.woniu0936.verses.core

import android.content.Context
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
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * An industrial-grade [ListAdapter] that orchestrates the rendering of [ItemWrapper] units.
 *
 * VerseAdapter is designed for maximum performance and memory safety. It leverages a global
 * static registry for ViewType IDs to enable seamless [RecyclerView.RecycledViewPool] sharing,
 * while keeping View factories instance-local to prevent Context leaks.
 */
@PublishedApi
internal class VerseAdapter : ListAdapter<ItemWrapper, SmartViewHolder>(
    AsyncDifferConfig.Builder(WrapperDiffCallback)
        .setBackgroundThreadExecutor(Dispatchers.Default.asExecutor())
        .build()
) {
    /**
     * Instance-local registry for ViewHolder factories.
     * Storing factories here ensures they use the correct Context associated with this Adapter's host.
     */
    private val localTypeToFactory = ConcurrentHashMap<Int, (ViewGroup) -> SmartViewHolder>()

    companion object {
        /**
         * Global cache for ViewType IDs. Ensures consistent IDs for the same keys across pages.
         */
        private val globalViewTypeCache = ConcurrentHashMap<Any, Int>()
        
        /**
         * Atomic counter for generating unique ViewType IDs.
         */
        private val nextViewType = AtomicInteger(1)

        /**
         * Stores RecycledViewPool instances scoped to specific Contexts.
         */
        private val contextPools = WeakHashMap<Context, RecyclerView.RecycledViewPool>()

        /**
         * Returns a [RecyclerView.RecycledViewPool] tied to the provided [context].
         */
        fun getPool(context: Context): RecyclerView.RecycledViewPool {
            return synchronized(contextPools) {
                contextPools.getOrPut(context) { RecyclerView.RecycledViewPool() }
            }
        }

        /**
         * Explicitly purges all global registries and context-scoped pools.
         */
        fun clearRegistry() {
            globalViewTypeCache.clear()
            nextViewType.set(1)
            synchronized(contextPools) {
                contextPools.clear()
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).viewType

    /**
     * Obtains a global ViewType ID and registers the factory to the local instance.
     */
    fun getOrCreateViewType(key: Any, factory: (ViewGroup) -> SmartViewHolder): Int {
        var isNew = false
        val viewType = globalViewTypeCache.getOrPut(key) {
            isNew = true
            nextViewType.getAndIncrement()
        }
        if (isNew) {
            VersesLogger.i("New ViewType generated for key: $key (ID: $viewType). Total types: ${nextViewType.get() - 1}")
        }
        localTypeToFactory[viewType] = factory
        return viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartViewHolder {
        val factory = localTypeToFactory[viewType] 
            ?: throw IllegalStateException(
                "Verses Error: No factory registered for viewType $viewType in this instance. " +
                "This usually happens if the registry was cleared while the RecyclerView was still active."
            )
        
        val startTime = System.currentTimeMillis()
        val holder = factory(parent)
        val duration = System.currentTimeMillis() - startTime
        VersesLogger.perf("CreateViewHolder", duration, "ViewType: $viewType")
        
        holder.itemView.setOnClickListener {
            val currentAdapter = holder.bindingAdapter as? VerseAdapter ?: return@setOnClickListener
            val pos = holder.bindingAdapterPosition
            // Safety: Check range to prevent crashes during rapid animations or data removals
            if (pos != RecyclerView.NO_POSITION && pos < currentAdapter.itemCount) {
                currentAdapter.getItem(pos).onClick?.invoke()
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: SmartViewHolder, position: Int) {
        val item = getItem(position)
        val startTime = System.currentTimeMillis()
        
        try {
            holder.prepare(item.data)
            val params = holder.itemView.layoutParams
            if (params is StaggeredGridLayoutManager.LayoutParams) {
                if (params.isFullSpan != item.fullSpan) {
                    params.isFullSpan = item.fullSpan
                }
            }
            item.bind(holder, item.data)
            val isClickable = item.onClick != null
            if (holder.itemView.isClickable != isClickable) {
                holder.itemView.isClickable = isClickable
            }
            
            val duration = System.currentTimeMillis() - startTime
            VersesLogger.perf("BindViewHolder", duration, "Pos: $position, ID: ${item.id}")
        } catch (e: Exception) {
            VersesLogger.e("Fatal error during binding at position $position. Item ID: ${item.id}", e)
            throw e // Re-throw to make it visible during development
        }
    }

    override fun onViewAttachedToWindow(holder: SmartViewHolder) {
        super.onViewAttachedToWindow(holder)
        val pos = holder.bindingAdapterPosition
        if (pos != RecyclerView.NO_POSITION && pos < itemCount) {
            val item = getItem(pos)
            VersesLogger.lifecycle("Attach", "Pos: $pos, ID: ${item.id}")
            item.onAttach?.invoke()
        }
    }

    override fun onViewDetachedFromWindow(holder: SmartViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val pos = holder.bindingAdapterPosition
        if (pos != RecyclerView.NO_POSITION && pos < itemCount) {
            val item = getItem(pos)
            VersesLogger.lifecycle("Detach", "Pos: $pos, ID: ${item.id}")
            item.onAttach?.invoke()
        }
    }

    override fun onViewRecycled(holder: SmartViewHolder) {
        super.onViewRecycled(holder)
        cleanupNestedRecyclerViews(holder.itemView)
    }

    private fun cleanupNestedRecyclerViews(view: android.view.View) {
        when (view) {
            is RecyclerView -> view.adapter = null
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    cleanupNestedRecyclerViews(view.getChildAt(i))
                }
            }
        }
    }

    fun getSpanSize(position: Int, totalSpan: Int): Int {
        if (position !in 0 until itemCount) return 1
        val item = getItem(position)
        return if (item.fullSpan) totalSpan else item.span
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun getItem(position: Int): ItemWrapper = super.getItem(position)

    object WrapperDiffCallback : DiffUtil.ItemCallback<ItemWrapper>() {
        override fun areItemsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            val same = oldItem.id == newItem.id
            if (!same) {
                VersesLogger.diff("ID Change: [${oldItem.id}] -> [${newItem.id}]")
            }
            return same
        }
        
        @android.annotation.SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            val same = oldItem == newItem
            if (!same) {
                VersesLogger.diff("Content Change for ID [${newItem.id}]: Data modified or factory changed.")
            }
            return same
        }
    }
}