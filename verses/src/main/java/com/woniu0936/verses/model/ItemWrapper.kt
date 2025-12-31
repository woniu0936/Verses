package com.woniu0936.verses.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * A generic [androidx.recyclerview.widget.RecyclerView.ViewHolder] that holds a [androidx.viewbinding.ViewBinding] instance.
 *
 * @property binding The ViewBinding instance for this ViewHolder's root view.
 */
class SmartViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

/**
 * Typealias for ViewBinding's inflate function.
 */
typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB

/**
 * A flattened rendering unit that encapsulates data and its layout strategy.
 *
 * Each [ItemWrapper] corresponds to a single item in the [androidx.recyclerview.widget.RecyclerView]. It defines
 * how the item is identified, its view type, the data it carries, and how it should
 * be laid out and bound.
 *
 * @property id Unique identifier used by [androidx.recyclerview.widget.DiffUtil] to determine if two items are the same.
 * @property viewType The view type ID used for [androidx.recyclerview.widget.RecyclerView] view recycling.
 * @property data The actual data object. Used by [androidx.recyclerview.widget.DiffUtil] to check for content changes.
 * @property spanSize The number of columns this item occupies in a grid layout.
 * @property fullSpan If true, forces the item to occupy the full width in Grid or Staggered layouts.
 * @property factory A lambda to create a [SmartViewHolder] for this item.
 * @property bind A lambda to bind data to the [SmartViewHolder].
 */
data class ItemWrapper(
    val id: Any,
    val viewType: Int,
    val data: Any,
    val spanSize: Int,
    val fullSpan: Boolean,
    val factory: (ViewGroup) -> SmartViewHolder,
    val bind: (SmartViewHolder) -> Unit
)
