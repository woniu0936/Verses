package com.woniu0936.verses.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.pool.VerseTypeRegistry

/**
 * Represents a piece of UI in a [androidx.recyclerview.widget.RecyclerView].
 * It encapsulates the data, the View creation logic, and the binding logic.
 */
abstract class VerseModel<T : Any>(
    val id: Any,
    val data: T
) {
    /**
     * The layout resource ID associated with this model.
     * Required for asynchronous pre-inflation support.
     * Use 0 if the view is created programmatically without XML.
     */
    @get:LayoutRes
    abstract val layoutRes: Int

    /**
     * Returns a stable ViewType ID.
     */
    abstract fun getViewType(): Int

    /**
     * Creates a new [SmartViewHolder] for this model.
     */
    abstract fun createHolder(parent: ViewGroup): SmartViewHolder

    /**
     * Optional callback executed when the view is created, useful for 
     * initializing complex custom views or one-time styling.
     */
    open fun onViewCreated(view: View) {}

    /**
     * Binds the [data] to the [holder].
     */
    abstract fun bind(holder: SmartViewHolder)

    /**
     * Binds the [data] to the [holder] with partial updates.
     */
    open fun bind(holder: SmartViewHolder, payloads: List<Any>) {
        bind(holder)
    }

    /**
     * Returns the span size for this item in a grid layout.
     */
    open fun getSpanSize(totalSpan: Int, position: Int): Int = 1

    /**
     * Optional click listener for the item.
     */
    open val onClick: (() -> Unit)? = null

    /**
     * Optional callback when the item view is attached to the window.
     */
    open val onAttach: (() -> Unit)? = null

    /**
     * Optional callback when the item view is detached from the window.
     */
    open val onDetach: (() -> Unit)? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VerseModel<*>) return false
        return id == other.id && data == other.data
    }

    override fun hashCode(): Int {
        return id.hashCode() * 31 + (data.hashCode())
    }
}

/**
 * A specialized [VerseModel] for ViewBinding.
 */
abstract class ViewBindingModel<VB : ViewBinding, T : Any>(
    id: Any,
    data: T
) : VerseModel<T>(id, data) {

    abstract fun inflate(inflater: LayoutInflater, parent: ViewGroup): VB
    abstract fun bind(binding: VB, item: T)

    override fun getViewType(): Int = VerseTypeRegistry.getViewType(this.javaClass)

    override fun createHolder(parent: ViewGroup): SmartViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = inflate(inflater, parent)
        onViewCreated(binding.root)
        return SmartViewHolder(binding.root, binding)
    }

    @Suppress("UNCHECKED_CAST")
    override fun bind(holder: SmartViewHolder) {
        holder.prepare(data)
        bind(holder.binding as VB, data)
    }
}