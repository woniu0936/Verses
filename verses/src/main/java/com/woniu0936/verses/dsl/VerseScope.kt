package com.woniu0936.verses.dsl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.model.*

/**
 * The core DSL scope for building RecyclerView content declaratively.
 */
@VerseDsl
class VerseScope @PublishedApi internal constructor(
    @PublishedApi internal val adapter: VerseAdapter
) {

    @PublishedApi
    internal val newWrappers = mutableListOf<ItemWrapper>()

    @PublishedApi internal var currentData: Any? = null
    @PublishedApi internal var currentId: Any? = null

    // ============================================================================================
    //  Group 1: Standard List (1:1 Mapping)
    // ============================================================================================

    /**
     * Renders a list of items using ViewBinding.
     * 
     * @param contentType Optional key to differentiate layouts using the same Binding class.
     */
    inline fun <T : Any, reified VB : ViewBinding> items(
        items: List<T>,
        noinline inflate: Inflate<VB>,
        noinline key: ((T) -> Any)? = null,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: ((T) -> Unit)? = null,
        noinline onAttach: ((T) -> Unit)? = null,
        noinline onDetach: ((T) -> Unit)? = null,
        crossinline onBind: VB.(T) -> Unit
    ) {
        val stableKey = contentType ?: VB::class.java
        items.forEachIndexed { index, item ->
            internalRender(
                factory = { p -> 
                    val binding = inflate(LayoutInflater.from(p.context), p, false)
                    SmartViewHolder(binding.root, binding)
                },
                bind = { data ->
                    @Suppress("UNCHECKED_CAST")
                    (binding as VB).onBind(data as T)
                },
                key = stableKey,
                data = item,
                id = key?.invoke(item) ?: index,
                span = span,
                fullSpan = fullSpan,
                onClick = onClick?.let { { it(item) } },
                onAttach = onAttach?.let { { it(item) } },
                onDetach = onDetach?.let { { it(item) } }
            )
        }
    }

    /**
     * Renders a list of items using a Custom View.
     */
    inline fun <T : Any, reified V : View> items(
        items: List<T>,
        noinline create: ViewCreator<V>,
        noinline key: ((T) -> Any)? = null,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: ((T) -> Unit)? = null,
        noinline onAttach: ((T) -> Unit)? = null,
        noinline onDetach: ((T) -> Unit)? = null,
        crossinline onBind: V.(T) -> Unit
    ) {
        val stableKey = contentType ?: V::class.java
        items.forEachIndexed { index, item ->
            internalRender(
                factory = { p -> createSafeViewHolder(p, create) },
                bind = { data ->
                    @Suppress("UNCHECKED_CAST")
                    (view as V).onBind(data as T)
                },
                key = stableKey,
                data = item,
                id = key?.invoke(item) ?: index,
                span = span,
                fullSpan = fullSpan,
                onClick = onClick?.let { { it(item) } },
                onAttach = onAttach?.let { { it(item) } },
                onDetach = onDetach?.let { { it(item) } }
            )
        }
    }

    // ============================================================================================
    //  Group 2: Single Item (Header / Footer / Static)
    // ============================================================================================

    /**
     * Renders a single item using ViewBinding.
     */
    inline fun <reified VB : ViewBinding> item(
        noinline inflate: Inflate<VB>,
        data: Any? = Unit,
        key: Any? = null,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        crossinline onBind: VB.() -> Unit = {}
    ) {
        val stableKey = contentType ?: VB::class.java
        internalRender(
            factory = { p -> 
                val binding = inflate(LayoutInflater.from(p.context), p, false)
                SmartViewHolder(binding.root, binding)
            },
            bind = { 
                @Suppress("UNCHECKED_CAST")
                (binding as VB).onBind()
            },
            key = stableKey,
            data = data ?: Unit,
            id = key ?: "single_vb_${stableKey.hashCode()}",
            span = span,
            fullSpan = fullSpan,
            onClick = onClick,
            onAttach = onAttach,
            onDetach = onDetach
        )
    }

    /**
     * Renders a single item using a Custom View.
     */
    inline fun <reified V : View> item(
        noinline create: ViewCreator<V>,
        data: Any? = Unit,
        key: Any? = null,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        crossinline onBind: V.() -> Unit = {}
    ) {
        val stableKey = contentType ?: V::class.java
        internalRender(
            factory = { p -> createSafeViewHolder(p, create) },
            bind = { 
                @Suppress("UNCHECKED_CAST")
                (view as V).onBind()
            },
            key = stableKey,
            data = data ?: Unit,
            id = key ?: "single_view_${stableKey.hashCode()}",
            span = span,
            fullSpan = fullSpan,
            onClick = onClick,
            onAttach = onAttach,
            onDetach = onDetach
        )
    }

    // ============================================================================================
    //  Group 3: Advanced Control Flow (Iterator + Render)
    // ============================================================================================

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

    inline fun <reified VB : ViewBinding> render(
        noinline inflate: Inflate<VB>,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        crossinline onBind: VB.() -> Unit
    ) {
        val stableKey = contentType ?: VB::class.java
        val data = currentData ?: Unit
        
        internalRender(
            factory = { p -> 
                val binding = inflate(LayoutInflater.from(p.context), p, false)
                SmartViewHolder(binding.root, binding)
            },
            bind = { 
                @Suppress("UNCHECKED_CAST")
                (binding as VB).onBind()
            },
            key = stableKey,
            data = data,
            id = currentId ?: System.identityHashCode(data),
            span = span,
            fullSpan = fullSpan,
            onClick = onClick,
            onAttach = onAttach,
            onDetach = onDetach
        )
    }

    inline fun <reified V : View> render(
        noinline create: ViewCreator<V>,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        crossinline onBind: V.() -> Unit
    ) {
        val stableKey = contentType ?: V::class.java
        val data = currentData ?: Unit

        internalRender(
            factory = { p -> createSafeViewHolder(p, create) },
            bind = { 
                @Suppress("UNCHECKED_CAST")
                (view as V).onBind()
            },
            key = stableKey,
            data = data,
            id = currentId ?: System.identityHashCode(data),
            span = span,
            fullSpan = fullSpan,
            onClick = onClick,
            onAttach = onAttach,
            onDetach = onDetach
        )
    }

    @PublishedApi
    internal fun <V : View> createSafeViewHolder(parent: ViewGroup, create: ViewCreator<V>): SmartViewHolder {
        val view = create(parent.context)
        if (view.layoutParams == null) {
            view.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } else if (view.layoutParams !is RecyclerView.LayoutParams) {
            view.layoutParams = RecyclerView.LayoutParams(view.layoutParams)
        }
        return SmartViewHolder(view, null)
    }

    @PublishedApi
    internal fun internalRender(
        factory: (ViewGroup) -> SmartViewHolder,
        bind: SmartViewHolder.(Any) -> Unit,
        key: Any,
        data: Any,
        id: Any,
        span: Int,
        fullSpan: Boolean,
        onClick: (() -> Unit)?,
        onAttach: (() -> Unit)?,
        onDetach: (() -> Unit)?
    ) {
        val viewType = adapter.getOrCreateViewType(key, factory)
        newWrappers.add(ItemWrapper(
            id = id,
            viewType = viewType,
            data = data,
            span = span,
            fullSpan = fullSpan,
            factory = factory,
            bind = bind,
            onClick = onClick,
            onAttach = onAttach,
            onDetach = onDetach
        ))
    }
}