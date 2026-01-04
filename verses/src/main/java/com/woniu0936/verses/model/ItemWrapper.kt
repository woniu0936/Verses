package com.woniu0936.verses.model

import android.view.ViewGroup

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
 * @property onAttach Triggered when view enters the screen.
 * @property onDetach Triggered when view leaves the screen.
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

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + viewType
        result = 31 * result + data.hashCode()
        result = 31 * result + span
        result = 31 * result + fullSpan.hashCode()
        return result
    }
}