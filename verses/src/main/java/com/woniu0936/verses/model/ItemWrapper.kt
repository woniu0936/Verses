package com.woniu0936.verses.model

import android.view.ViewGroup

/**
 * The fundamental atomic rendering unit of Verses.
 *
 * ItemWrapper encapsulates the identity, data, and presentation logic for a single item 
 * in the list. By flattening the complex Adapter/ViewHolder relationship into this immutable 
 * data structure, the library can perform optimized [androidx.recyclerview.widget.DiffUtil] 
 * calculations on a background thread.
 *
 * @property id A unique identifier used by [androidx.recyclerview.widget.DiffUtil.ItemCallback.areItemsTheSame]. 
 *              Must be stable across render cycles to avoid unnecessary view recreations.
 * @property viewType An integer ID assigned by the global registry. Used by RecyclerView 
 *                    to identify the layout and reuse ViewHolders.
 * @property data The raw business data object. Used for content equality checks.
 * @property span The column span count for this item in a [androidx.recyclerview.widget.GridLayoutManager].
 * @property fullSpan If true, this item will occupy the entire width of the grid, ignoring [span].
 * @property factory A lambda used to instantiate the [SmartViewHolder]. Executed during 
 *                   [androidx.recyclerview.widget.RecyclerView.Adapter.onCreateViewHolder].
 * @property bind A lambda that applies [data] to the view. Executed during 
 *                 [androidx.recyclerview.widget.RecyclerView.Adapter.onBindViewHolder].
 * @property onClick Stateless click callback invoked via a proxy listener to prevent memory leaks.
 * @property onAttach Triggered when the view enters the screen (onViewAttachedToWindow).
 * @property onDetach Triggered when the view leaves the screen (onViewDetachedFromWindow).
 */
@PublishedApi
internal data class ItemWrapper(
    val id: Any,
    val viewType: Int,
    val data: Any,
    val span: Int,
    val fullSpan: Boolean,
    val factory: (ViewGroup) -> SmartViewHolder,
    val bind: SmartViewHolder.(Any) -> Unit,
    val onClick: (() -> Unit)? = null,
    val onAttach: (() -> Unit)? = null,
    val onDetach: (() -> Unit)? = null
) {
    /**
     * Standard equality implementation that excludes function properties.
     * 
     * Since [factory], [bind], and [onClick] are typically re-instantiated on every
     * DSL execution, including them in equality would cause [DiffUtil] to detect 
     * changes on every frame, leading to UI flickering.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ItemWrapper
        if (id != other.id) return false
        if (viewType != other.viewType) return false
        if (data != other.data) return false
        if (span != other.span) return false
        if (fullSpan != other.fullSpan) return false
        return true
    }

    /**
     * Consistent with [equals], ignoring lambda instances to preserve DiffUtil stability.
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + viewType
        result = 31 * result + data.hashCode()
        result = 31 * result + span
        result = 31 * result + fullSpan.hashCode()
        return result
    }
}
