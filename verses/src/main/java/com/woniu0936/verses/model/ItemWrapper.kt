package com.woniu0936.verses.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * A specialized [RecyclerView.ViewHolder] that serves as a bridge between the library's
 * internal rendering logic and the underlying Android View system.
 * 
 * It supports both [ViewBinding] and standard programmatic [View] instances.
 *
 * @property view The root view of the item.
 * @property binding The ViewBinding instance, if available (null for custom View-based items).
 */
@PublishedApi
internal class SmartViewHolder(
    val view: View,
    val binding: ViewBinding? = null
) : RecyclerView.ViewHolder(view)

/**
 * Functional interface for inflating a [ViewBinding] instance.
 *
 * This is designed to be used with method references such as `ItemUserBinding::inflate`,
 * allowing the DSL to handle layout inflation internally without boilerplate.
 */
typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB

/**
 * Functional interface for creating a custom [View] programmatically.
 *
 * Used in programmatic mode where XML layouts are not employed.
 */
typealias ViewCreator<V> = (Context) -> V

/**
 * The fundamental rendering unit of Verses.
 * 
 * Encapsulates the identity, data, and presentation logic for a single item in the list.
 * By flattening the complex Adapter/ViewHolder pattern into this simple data class, 
 * the library can perform optimized [androidx.recyclerview.widget.DiffUtil] calculations.
 *
 * @property id Stable identifier for DiffUtil (calculated from provided keys or item index).
 * @property viewType The unique integer ID assigned by the global registry for view recycling.
 * @property data The raw business data associated with this item.
 * @property span The number of columns this item occupies in a grid layout (default is 1).
 * @property fullSpan Whether this item should ignore [span] and occupy the full width of the list.
 * @property factory Lambda that creates the [SmartViewHolder] when needed by the RecyclerView.
 * @property bind Lambda that applies [data] to the View or ViewBinding.
 * @property onClick High-performance, stateless click callback.
 */
@PublishedApi
internal data class ItemWrapper(
    val id: Any,
    val viewType: Int,
    val data: Any,
    val span: Int,
    val fullSpan: Boolean,
    val factory: (ViewGroup) -> SmartViewHolder,
    val bind: (SmartViewHolder) -> Unit,
    val onClick: (() -> Unit)? = null,
    val onAttach: (() -> Unit)? = null,
    val onDetach: (() -> Unit)? = null
)
