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
 *
 * VerseScope provides a set of high-level builder methods to define items, lists, 
 * and custom rendering logic. It utilizes Kotlin's reified type parameters to 
 * automatically manage ViewType safety without requiring manual ID registration.
 *
 * This scope is transient and is intended to be used only during the [submit] phase.
 */
@VerseDsl
class VerseScope @PublishedApi internal constructor(
    /**
     * The backing adapter where [ItemWrapper] units are registered.
     */
    @PublishedApi internal val adapter: VerseAdapter
) {

    /**
     * Accumulates the [ItemWrapper] units generated during the current DSL execution.
     */
    @PublishedApi
    internal val newWrappers = mutableListOf<ItemWrapper>()

    /**
     * Context data for advanced mode (items + render iteration).
     */
    @PublishedApi internal var currentData: Any? = null
    
    /**
     * Context ID for advanced mode.
     */
    @PublishedApi internal var currentId: Any? = null

    // ============================================================================================
    //  Group 1: Standard List (1:1 Mapping)
    // ============================================================================================

    /**
     * Renders a list of items using ViewBinding.
     * 
     * @param T The business data type.
     * @param VB The ViewBinding class generated from an XML layout.
     * @param items The source data list.
     * @param inflate Method reference to the Binding's inflate function (e.g., ItemUserBinding::inflate).
     * @param key A lambda to provide a stable ID for DiffUtil. If null, item index is used (discouraged).
     * @param onBind The binding block where UI properties are applied.
     */
    inline fun <T : Any, reified VB : ViewBinding> items(
        items: List<T>,
        noinline inflate: Inflate<VB>,
        noinline key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: ((T) -> Unit)? = null,
        noinline onAttach: ((T) -> Unit)? = null,
        noinline onDetach: ((T) -> Unit)? = null,
        crossinline onBind: VB.(T) -> Unit
    ) {
        val stableKey = VB::class.java
        items.forEachIndexed { index, item ->
            // WARNING: Using 'index' as a key is discouraged for dynamic lists.
            // It can cause full-item rebinds and loss of state during insertions/deletions.
            // ALWAYS provide a stable key (e.g., item.id) for production lists.
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
     * Renders a list of items using a Custom View (programmatic UI).
     */
    inline fun <T : Any, reified V : View> items(
        items: List<T>,
        noinline create: ViewCreator<V>,
        noinline key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: ((T) -> Unit)? = null,
        noinline onAttach: ((T) -> Unit)? = null,
        noinline onDetach: ((T) -> Unit)? = null,
        crossinline onBind: V.(T) -> Unit
    ) {
        val stableKey = V::class.java
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
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        crossinline onBind: VB.() -> Unit = {}
    ) {
        val stableKey = VB::class.java
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
            id = key ?: "single_vb_${stableKey.name}",
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
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        crossinline onBind: V.() -> Unit = {}
    ) {
        val stableKey = V::class.java
        internalRender(
            factory = { p -> createSafeViewHolder(p, create) },
            bind = { 
                @Suppress("UNCHECKED_CAST")
                (view as V).onBind()
            },
            key = stableKey,
            data = data ?: Unit,
            id = key ?: "single_view_${stableKey.name}",
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

    /**
     * Starts an iteration scope for advanced scenarios involving conditional logic.
     * Use [render] inside this block to define the actual UI units.
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
     * Renders a UI unit within an advanced [items] block using ViewBinding.
     */
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

    /**
     * Renders a UI unit within an advanced [items] block using a Custom View.
     */
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

    // ============================================================================================
    //  Internal Implementation (Private)
    // ============================================================================================

    /**
     * Ensures the custom view has valid [RecyclerView.LayoutParams].
     */
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

    /**
     * Internal factory method to package parameters into an [ItemWrapper] and 
     * register it with the current DSL state.
     */
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
