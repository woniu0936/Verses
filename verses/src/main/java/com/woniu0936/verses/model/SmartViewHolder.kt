package com.woniu0936.verses.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * A lightweight, stateful [RecyclerView.ViewHolder] that serves as the root for all Verses items.
 *
 * It maintains state for performance optimizations like the "Bind Lock" and 
 * "One-time Optimization Flag".
 */
class SmartViewHolder(
    val view: View,
    val binding: ViewBinding? = null
) : RecyclerView.ViewHolder(view) {

    /**
     * The business data object associated with the current binding cycle.
     */
    internal var currentData: Any? = null

    /**
     * Stores the last bound model to implement a precise "Bind Lock".
     * Prevents redundant DSL execution if the model content hasn't changed.
     */
    internal var lastBoundModel: VerseModel<*>? = null

    /**
     * Cached reference to a nested RecyclerView to avoid recursive searches.
     */
    internal var nestedRv: RecyclerView? = null

    /**
     * Flag indicating if this ViewHolder's view hierarchy has already been 
     * scanned and optimized for nested RecyclerViews.
     */
    internal var isOptimized: Boolean = false

    /**
     * Prepares the holder for a new binding cycle.
     */
    fun prepare(data: Any) {
        this.currentData = data
    }

    /**
     * Retrieves the business data object, cast to the expected type [T].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> itemData(): T = currentData as T
}

/**
 * Functional interface for inflating a [ViewBinding] instance.
 */
typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB

/**
 * Functional interface for creating a custom [View] programmatically.
 */
typealias ViewCreator<V> = (Context) -> V