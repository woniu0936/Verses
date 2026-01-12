package com.woniu0936.verses.model

import android.view.View
import android.view.ViewGroup
import com.woniu0936.verses.core.VersesLogger
import com.woniu0936.verses.core.pool.VerseTypeRegistry

/**
 * A concrete implementation of [VerseModel] used by the DSL to wrap items.
 */
internal class DslVerseModel(
    id: Any,
    data: Any,
    override val layoutRes: Int,
    private val layoutKey: Any,
    private val factory: (ViewGroup) -> SmartViewHolder,
    private val onCreateBlock: (SmartViewHolder.() -> Unit)? = null,
    private val bindBlock: SmartViewHolder.(Any) -> Unit,
    private val span: Int = 1,
    private val fullSpan: Boolean = false,
    override val onClick: (() -> Unit)? = null,
    override val onAttach: (() -> Unit)? = null,
    override val onDetach: (() -> Unit)? = null
) : VerseModel<Any>(id, data) {

    override fun resolveViewType(): Int {
        val key = layoutKey
        return if (key is Class<*>) {
            VerseTypeRegistry.getViewType(key)
        } else {
            VerseTypeRegistry.getViewType(key.hashCode())
        }
    }

    override fun createHolder(parent: ViewGroup): SmartViewHolder {
        val holder = factory(parent)
        onViewCreated(holder.view)
        return holder
    }

    override fun onCreate(holder: SmartViewHolder) {
        onCreateBlock?.invoke(holder)
        
        // [DX Guard] Detect if user manually set a listener on the root view.
        // Since VerseAdapter sets its own listener AFTER this method returns, 
        // the user's manual listener would be silently overwritten.
        if (holder.itemView.hasOnClickListeners()) {
            VersesLogger.w(
                "⚠️ [Anti-Pattern] Manual 'setOnClickListener' detected on root view in 'onCreate'. " +
                "It WILL BE OVERWRITTEN by Verses. Please use the 'onClick' parameter in the DSL."
            )
        }
    }

    override fun bind(holder: SmartViewHolder) {
        holder.prepare(data)
        holder.bindBlock(data)
    }

    override fun getSpanSize(totalSpan: Int, position: Int): Int {
        return if (fullSpan) totalSpan else span
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DslVerseModel) return false
        if (!super.equals(other)) return false
        if (layoutKey != other.layoutKey) return false
        if (span != other.span) return false
        if (fullSpan != other.fullSpan) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + layoutKey.hashCode()
        result = 31 * result + span
        result = 31 * result + fullSpan.hashCode()
        return result
    }
}
