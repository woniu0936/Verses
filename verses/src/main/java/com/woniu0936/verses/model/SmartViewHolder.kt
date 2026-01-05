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
 * Unlike traditional ViewHolders that often contain complex binding logic, SmartViewHolder
 * acts as a pure container for the [view] and [binding], providing a safe scope for
 * declarative item access.
 *
 * @param view The root view of the item layout.
 * @param binding An optional [ViewBinding] instance for type-safe view access.
 */
class SmartViewHolder(
    val view: View,
    val binding: ViewBinding? = null
) : RecyclerView.ViewHolder(view) {

    /**
     * The business data object associated with the current binding cycle.
     * This reference is updated in [onBindViewHolder] before the user's bind block is executed.
     */
    internal var currentData: Any? = null

    /**
     * Prepares the holder for a new binding cycle.
     * Must be called on the main thread during the binding process.
     *
     * @param data The latest data instance to be held by this view.
     */
    fun prepare(data: Any) {
        this.currentData = data
    }

    /**
     * Retrieves the business data object, cast to the expected type [T].
     * 
     * This is useful for accessing the current item's state within nested callbacks 
     * or event listeners defined inside the DSL block.
     *
     * @return The current data cast to [T].
     * @throws ClassCastException if the data is not of type [T].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> itemData(): T = currentData as T
}

/**
 * Functional interface for inflating a [ViewBinding] instance.
 * Aligned with the generated `inflate` methods in the Android ViewBinding library.
 */
typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB

/**
 * Functional interface for creating a custom [View] programmatically.
 * Used for items that do not rely on XML layouts.
 */
typealias ViewCreator<V> = (Context) -> V