package com.woniu0936.verses.dsl

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.model.Inflate
import com.woniu0936.verses.model.ItemWrapper
import com.woniu0936.verses.model.SmartViewHolder

class VerseScope(private val adapter: VerseAdapter) {

    internal val newWrappers = mutableListOf<ItemWrapper>()

    // Temporary context variables (only for advanced mode)
    private var currentData: Any? = null
    private var currentId: Any? = null

    // =======================================================
    //  API 1.0: Simple Mode (Direct Inflate)
    // =======================================================

    /**
     * Render list data
     */
    fun <T : Any, VB : ViewBinding> items(
        items: List<T>,
        inflate: Inflate<VB>,
        key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        onBind: (VB, T) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            internalRender(
                inflate = inflate,
                contentType = null,
                data = item,
                id = key?.invoke(item) ?: index,
                span = span,
                fullSpan = fullSpan,
                onBind = { vb -> onBind(vb, item) }
            )
        }
    }

    /**
     * Render single Item (e.g. Header/Footer)
     */
    fun <VB : ViewBinding> item(
        inflate: Inflate<VB>,
        data: Any? = Unit,
        key: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        onBind: (VB) -> Unit = {}
    ) {
        internalRender(
            inflate = inflate,
            contentType = null,
            data = data ?: Unit,
            id = key ?: "single_${inflate.hashCode()}",
            span = span,
            fullSpan = fullSpan,
            onBind = onBind
        )
    }

    // =======================================================
    //  API 2.0: Advanced Mode (Control Flow + Render)
    // =======================================================

    /**
     * Traverse data, use with render
     */
    fun <T : Any> items(
        items: List<T>,
        key: ((T) -> Any)? = null,
        block: VerseScope.(T) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            currentData = item
            currentId = key?.invoke(item) ?: index
            block(item)
        }
    }

    /**
     * Call inside items block for conditional rendering
     */
    fun <VB : ViewBinding> render(
        inflate: Inflate<VB>,
        contentType: Any? = null, // If inflate is dynamic lambda, this Key must be provided
        span: Int = 1,
        fullSpan: Boolean = false,
        onBind: (VB) -> Unit
    ) {
        internalRender(
            inflate = inflate,
            contentType = contentType,
            data = currentData ?: Unit,
            id = currentId ?: System.identityHashCode(currentData),
            span = span,
            fullSpan = fullSpan,
            onBind = onBind
        )
    }

    // =======================================================
    //  Internal Implementation
    // =======================================================

    private fun <VB : ViewBinding> internalRender(
        inflate: Inflate<VB>,
        contentType: Any?,
        data: Any,
        id: Any,
        span: Int,
        fullSpan: Boolean,
        onBind: (VB) -> Unit
    ) {
        // Core de-duplication logic: prioritize contentType, otherwise use inflate function reference
        val cacheKey = contentType ?: inflate
        val viewType = adapter.getOrCreateViewType(cacheKey)

        newWrappers.add(ItemWrapper(
            id = id,
            viewType = viewType,
            data = data,
            spanSize = span,
            fullSpan = fullSpan,
            factory = { parent -> 
                SmartViewHolder(inflate(LayoutInflater.from(parent.context), parent, false)) 
            },
            bind = { holder -> 
                @Suppress("UNCHECKED_CAST")
                onBind(holder.binding as VB) 
            }
        ))
    }
}
