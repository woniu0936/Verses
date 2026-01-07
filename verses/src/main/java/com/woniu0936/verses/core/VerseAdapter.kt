package com.woniu0936.verses.core

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woniu0936.verses.core.perf.VersePreloader
import com.woniu0936.verses.core.pool.VerseRecycledViewPool
import com.woniu0936.verses.core.pool.VerseTypeRegistry
import com.woniu0936.verses.model.SmartViewHolder
import com.woniu0936.verses.model.VerseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * An industrial-grade [ListAdapter] that orchestrates the rendering of [VerseModel] units.
 *
 * VerseAdapter implements an autonomous performance engine with Bind Locking,
 * One-time view scanning, and dynamic prefetch depth.
 */
@PublishedApi
internal class VerseAdapter : ListAdapter<VerseModel<*>, SmartViewHolder>(
    AsyncDifferConfig.Builder(VerseDiffCallback)
        .setBackgroundThreadExecutor(Dispatchers.Default.asExecutor())
        .build()
) {

    init {
        setHasStableIds(true)
    }

    override fun getItemViewType(position: Int): Int {
        val model = getItem(position)
        VerseTypeRegistry.registerPrototype(model)
        return model.getViewType()
    }

    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    override fun onCurrentListChanged(previousList: List<VerseModel<*>>, currentList: List<VerseModel<*>>) {
        super.onCurrentListChanged(previousList, currentList)
        if (currentList.isNotEmpty()) {
            VerseAdapterRegistry.latestContext?.let { context ->
                VersePreloader.autoPreload(context)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartViewHolder {
        val template = currentList.firstOrNull { it.getViewType() == viewType }
            ?: VerseTypeRegistry.getPrototype(viewType)
            ?: throw IllegalStateException("Verses Error: No model prototype found for viewType $viewType")

        val startTime = System.currentTimeMillis()
        val holder = template.createHolder(parent)
        val duration = System.currentTimeMillis() - startTime
        
        if (duration > 10) {
            VersesLogger.perf("CreateViewHolder (SLOW)", duration, "ViewType: $viewType")
            
            val currentMax = VerseRecycledViewPool.GLOBAL.getPoolSize(viewType)
            if (currentMax < 20) {
                // Autonomous Scaling: Idempotent expansion
                VerseRecycledViewPool.GLOBAL.setPoolSize(viewType, 20)
                
                // Pre-emptive Production: If we're slow, we need a larger buffer immediately.
                VersePreloader.preload(parent.context, listOf(template), countPerType = 8)
            }
        } else {
            VersesLogger.perf("CreateViewHolder", duration, "ViewType: $viewType")
        }

        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < itemCount) {
                getItem(pos).onClick?.invoke()
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: SmartViewHolder, position: Int) {
        val model = try { getItem(position) } catch (e: Exception) { null } ?: return
        
        // [Advanced Bind Lock] Skip redundant DSL execution if model content is identical.
        if (holder.lastBoundModel == model) {
            VersesLogger.d("Bind Lock: Skipping redundant binding for ID ${model.id}")
            return
        }

        val startTime = System.currentTimeMillis()

        try {
            model.bind(holder)
            holder.lastBoundModel = model
            
            // [One-time Optimization Scan] Lock the hierarchy after the first bind.
            if (!holder.isOptimized) {
                val view = holder.itemView
                if (view is ViewGroup) {
                    optimizeNestedRecyclerViews(view)
                }
                holder.isOptimized = true
            }

            val duration = System.currentTimeMillis() - startTime
            VersesLogger.perf("BindViewHolder", duration, "Pos: $position, ID: ${model.id}")
        } catch (e: Exception) {
            VersesLogger.e("Fatal error during binding at position $position. Item ID: ${model.id}", e)
            throw e
        }
    }

    private fun optimizeNestedRecyclerViews(view: ViewGroup) {
        if (view is RecyclerView) {
            applyRvOptimizations(view)
        }
        
        try {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is ViewGroup) {
                    optimizeNestedRecyclerViews(child)
                }
            }
        } catch (e: Throwable) {}
    }

    private fun applyRvOptimizations(rv: RecyclerView) {
        try {
            // A. Shared Global Pool
            if (rv.recycledViewPool != VerseRecycledViewPool.GLOBAL) {
                rv.setRecycledViewPool(VerseRecycledViewPool.GLOBAL)
            }
            
            // B. [Layout Lock] Freeze size to prevent child updates from re-measuring the parent.
            rv.setHasFixedSize(true)

            // C. Proactive Prefetch Calculation
            val lm = rv.layoutManager
            if (lm is LinearLayoutManager && lm.initialPrefetchItemCount <= 0) {
                val span = (lm as? GridLayoutManager)?.spanCount ?: 1
                // Amortize the production by prefetching 2 full rows.
                lm.initialPrefetchItemCount = if (span > 1) span * 2 else 4
            }
            
            // D. Interaction Tuning
            if (lm is LinearLayoutManager && lm.orientation == RecyclerView.HORIZONTAL) {
                rv.isNestedScrollingEnabled = false
            }
        } catch (e: Throwable) {}
    }

    override fun onBindViewHolder(holder: SmartViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            getItem(position).bind(holder, payloads)
        }
    }

    override fun onFailedToRecycleView(holder: SmartViewHolder): Boolean = true

    override fun onViewAttachedToWindow(holder: SmartViewHolder) {
        super.onViewAttachedToWindow(holder)
        val pos = holder.bindingAdapterPosition
        if (pos != RecyclerView.NO_POSITION && pos < itemCount) {
            getItem(pos).onAttach?.invoke()
        }
    }

    override fun onViewDetachedFromWindow(holder: SmartViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val pos = holder.bindingAdapterPosition
        if (pos != RecyclerView.NO_POSITION && pos < itemCount) {
            getItem(pos).onDetach?.invoke()
        }
    }

    override fun onViewRecycled(holder: SmartViewHolder) {
        super.onViewRecycled(holder)
        holder.lastBoundModel = null
        cleanupNestedRecyclerViews(holder.itemView)
    }

    private fun cleanupNestedRecyclerViews(view: android.view.View) {
        if (view is RecyclerView) {
            view.adapter = null
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                cleanupNestedRecyclerViews(view.getChildAt(i))
            }
        }
    }

    fun getSpanSize(position: Int, totalSpan: Int): Int {
        if (position !in 0 until itemCount) return 1
        return getItem(position).getSpanSize(totalSpan, position)
    }

    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PROTECTED)
    public override fun getItem(position: Int): VerseModel<*> = super.getItem(position)

    private object VerseDiffCallback : DiffUtil.ItemCallback<VerseModel<*>>() {
        override fun areItemsTheSame(oldItem: VerseModel<*>, newItem: VerseModel<*>): Boolean =
            oldItem.id == newItem.id

        @Suppress("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: VerseModel<*>, newItem: VerseModel<*>): Boolean =
            oldItem == newItem
    }
}

/**
 * A small utility to capture the latest context for preloading.
 * Uses WeakReference to prevent memory leaks if the Activity/Fragment is destroyed.
 */
internal object VerseAdapterRegistry {
    private var contextRef: java.lang.ref.WeakReference<android.content.Context>? = null

    var latestContext: android.content.Context?
        get() = contextRef?.get()
        set(value) {
            contextRef = if (value != null) java.lang.ref.WeakReference(value) else null
        }
}
