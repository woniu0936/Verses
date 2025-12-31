package com.woniu0936.verses.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * A smart [androidx.recyclerview.widget.RecyclerView.ViewHolder] that supports both ViewBinding and Custom Views.
 *
 * @property view The root view.
 * @property binding Optional ViewBinding instance (null for Custom Views).
 */
@PublishedApi
internal class SmartViewHolder(
    val view: View,
    val binding: ViewBinding? = null
) : RecyclerView.ViewHolder(view)

/**
 * Typealias for ViewBinding's inflate function.
 */
typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB

/**
 * Typealias for programmatic View creation function.
 */
typealias ViewCreator<V> = (Context) -> V

/**
 * A flattened rendering unit that encapsulates data and its layout strategy.
 *
 * Each [ItemWrapper] corresponds to a single item in the [androidx.recyclerview.widget.RecyclerView].
 *
 * @property id Unique identifier used by [androidx.recyclerview.widget.DiffUtil].
 * @property viewType The view type ID used for recycling.
 * @property data The actual data object.
 * @property span The number of columns this item occupies in a grid layout.
 * @property fullSpan If true, forces the item to occupy the full width.
 * @property factory A lambda to create a [SmartViewHolder] for this item.
 * @property bind A lambda to bind data to the [SmartViewHolder].
 */
@PublishedApi
internal data class ItemWrapper(
    val id: Any,
    val viewType: Int,
    val data: Any,
    val span: Int,
    val fullSpan: Boolean,
    val factory: (ViewGroup) -> SmartViewHolder,
    val bind: (SmartViewHolder) -> Unit
)