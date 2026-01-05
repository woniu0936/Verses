package com.woniu0936.verses.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * A stateful ViewHolder that serves as the root for Verses items.
 */
class SmartViewHolder(
    val view: View,
    val binding: ViewBinding? = null
) : RecyclerView.ViewHolder(view) {

    /**
     * The raw business data currently bound to this holder.
     */
    internal var currentData: Any? = null

    /**
     * Prepares the holder for a new binding cycle.
     */
    fun prepare(data: Any) {
        this.currentData = data
    }

    /**
     * Safely access current data.
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
