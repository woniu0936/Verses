package com.woniu0936.verses.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.pool.VerseTypeRegistry

/**
 * Represents a piece of UI in a [androidx.recyclerview.widget.RecyclerView].
 */
abstract class VerseModel<T : Any>(
    val id: Any,
    val data: T
) {
    private var _cachedViewType: Int = -1

    @get:LayoutRes
    abstract val layoutRes: Int

    /**
     * Returns the stable ViewType ID for this model.
     * Uses instance-level caching to ensure O(1) performance during scrolling.
     */
    fun getViewType(): Int {
        if (_cachedViewType == -1) {
            _cachedViewType = resolveViewType()
        }
        return _cachedViewType
    }

    /**
     * Internal strategy to resolve the ViewType ID.
     */
    protected abstract fun resolveViewType(): Int

    /**
     * Creates a new [SmartViewHolder] for this model.
     */
    abstract fun createHolder(parent: ViewGroup): SmartViewHolder

    /**
     * One-time initialization callback executed immediately after [createHolder].
     * Use this to set up click listeners or one-time styling.
     */
    open fun onCreate(holder: SmartViewHolder) {}

    /**
     * Optional callback executed when the view is created.
     */
    open fun onViewCreated(view: View) {}

    /**
     * Binds the [data] to the [holder].
     */
    abstract fun bind(holder: SmartViewHolder)

    open fun bind(holder: SmartViewHolder, payloads: List<Any>) {
        bind(holder)
    }

    open fun getSpanSize(totalSpan: Int, position: Int): Int = 1

    open val onClick: (() -> Unit)? = null
    open val onAttach: (() -> Unit)? = null
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

    override fun resolveViewType(): Int = VerseTypeRegistry.getViewType(this.javaClass)

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
