package com.woniu0936.verses.core.pool

import androidx.recyclerview.widget.RecyclerView
import com.woniu0936.verses.core.VersesLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * An enhanced [RecyclerView.RecycledViewPool] that supports global sharing and 
 * idempotent capacity configuration.
 */
class VerseRecycledViewPool : RecyclerView.RecycledViewPool() {
    
    // Tracks the largest configured size per type to ensure scaling is idempotent.
    private val configuredSizes = ConcurrentHashMap<Int, Int>()

    companion object {
        val GLOBAL = VerseRecycledViewPool()
    }

    /**
     * Configures the maximum number of view holders to keep in the pool for a specific model class.
     */
    fun setMaxPoolSize(modelClass: Class<*>, size: Int) {
        val viewType = VerseTypeRegistry.getViewType(modelClass)
        setPoolSize(viewType, size)
    }

    /**
     * Internal helper to set pool size by view type.
     * This operation is thread-safe and idempotent.
     */
    fun setPoolSize(viewType: Int, size: Int) {
        // Use compute to atomize the check-and-update logic
        configuredSizes.compute(viewType) { _, current ->
            val existing = current ?: 5
            if (existing < size) {
                setMaxRecycledViews(viewType, size)
                VersesLogger.i("Autonomous Pool Scaling: Type $viewType expanded to $size")
                size
            } else {
                existing
            }
        }
    }

    /**
     * Returns the configured max size for a view type.
     */
    fun getPoolSize(viewType: Int): Int = configuredSizes[viewType] ?: 5
}