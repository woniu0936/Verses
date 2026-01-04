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
 * This scope provides methods to define items, lists, and control flow structures.
 * It utilizes [reified] generics to ensure ViewType safety automatically.
 */
@VerseDsl
class VerseScope @PublishedApi internal constructor(
    @PublishedApi internal val adapter: VerseAdapter
) {

    @PublishedApi
    internal val newWrappers = mutableListOf<ItemWrapper>()

    // Context variables for Advanced Mode (items + render)
    @PublishedApi internal var currentData: Any? = null
    @PublishedApi internal var currentId: Any? = null

    // ============================================================================================
    //  Group 1: Standard List (1:1 Mapping)
    // ============================================================================================

    /**
     * Renders a list of items using [ViewBinding].
     *
     * @param items The data source list.
     * @param inflate The ViewBinding inflater reference (e.g., ItemUserBinding::inflate).
     * @param key A function to extract a stable ID for DiffUtil. Defaults to list index (not recommended for mutable lists).
     * @param span The number of columns this item occupies in a Grid layout. Default is 1.
     * @param fullSpan Whether this item should span the full width in Staggered layouts. Default is false.
     * @param onBind The binding logic block.
     */
    inline fun <T : Any, reified VB : ViewBinding> items(
        items: List<T>,
        noinline inflate: Inflate<VB>,
        noinline key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onBind: VB.(T) -> Unit
    ) {
        val stableKey = VB::class.java
        items.forEachIndexed { index, item ->
            internalRender(
                factory = { p -> 
                    val binding = inflate(LayoutInflater.from(p.context), p, false)
                    SmartViewHolder(binding.root, binding)
                },
                bind = { h -> (h.binding as VB).onBind(item) },
                key = stableKey,
                data = item,
                id = key?.invoke(item) ?: index,
                span = span,
                fullSpan = fullSpan
            )
        }
    }

    /**
     * Renders a list of items using a Custom [View].
     *
     * @param items The data source list.
     * @param create A factory function to create the View (e.g., ::MyView or { TextView(it) }).
     * @param key A function to extract a stable ID for DiffUtil.
     * @param span The number of columns this item occupies in a Grid layout.
     * @param fullSpan Whether this item should span the full width.
     * @param onBind The binding logic block.
     */
    inline fun <T : Any, reified V : View> items(
        items: List<T>,
        noinline create: ViewCreator<V>,
        noinline key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onBind: V.(T) -> Unit
    ) {
        val stableKey = V::class.java
        items.forEachIndexed { index, item ->
            internalRender(
                factory = { p -> createSafeViewHolder(p, create) },
                bind = { h -> (h.view as V).onBind(item) },
                key = stableKey,
                data = item,
                id = key?.invoke(item) ?: index,
                span = span,
                fullSpan = fullSpan
            )
        }
    }

    // ============================================================================================
    //  Group 2: Single Item (Header / Footer / Static)
    // ============================================================================================

    /**
     * Renders a single item using [ViewBinding].
     *
     * @param inflate The ViewBinding inflater reference.
     * @param data The data dependency. **Crucial**: If UI depends on external state, pass it here to trigger DiffUtil updates.
     * @param key A stable ID for DiffUtil. Defaults to a hash of the inflater.
     * @param span The span size. Default is 1.
     * @param fullSpan Whether to span full width. Default is true for single items.
     * @param onBind The binding logic block.
     */
    inline fun <reified VB : ViewBinding> item(
        noinline inflate: Inflate<VB>,
        data: Any? = Unit,
        key: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onBind: VB.() -> Unit = {}
    ) {
        val stableKey = VB::class.java
        internalRender(
            factory = { p -> 
                val binding = inflate(LayoutInflater.from(p.context), p, false)
                SmartViewHolder(binding.root, binding)
            },
            bind = { h -> (h.binding as VB).onBind() },
            key = stableKey,
            data = data ?: Unit,
            id = key ?: "single_vb_${stableKey.name}",
            span = span,
            fullSpan = fullSpan
        )
    }

    /**
     * Renders a single item using a Custom [View].
     *
     * @param create A factory function to create the View.
     * @param data The data dependency.
     * @param key A stable ID for DiffUtil.
     * @param span The span size.
     * @param fullSpan Whether to span full width. Default is true.
     * @param onBind The binding logic block.
     */
    inline fun <reified V : View> item(
        noinline create: ViewCreator<V>,
        data: Any? = Unit,
        key: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onBind: V.() -> Unit = {}
    ) {
        val stableKey = V::class.java
        internalRender(
            factory = { p -> createSafeViewHolder(p, create) },
            bind = { h -> (h.view as V).onBind() },
            key = stableKey,
            data = data ?: Unit,
            id = key ?: "single_view_${stableKey.name}",
            span = span,
            fullSpan = fullSpan
        )
    }

    // ============================================================================================
    //  Group 3: Advanced Control Flow (Iterator + Render)
    // ============================================================================================

    /**
     * Starts an iteration scope for advanced scenarios (e.g., mixed types, if/else logic).
     * Must be used in conjunction with [render].
     *
     * @param items The data source list.
     * @param key A function to extract a stable ID.
     * @param block The control flow block where you call [render].
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
     * Renders a UI unit within an advanced [items] block using [ViewBinding].
     *
     * @param inflate The ViewBinding inflater.
     * @param contentType An optional explicit key for ViewType pooling. Use only if needed (e.g., same binding, different pools).
     * @param span The span size.
     * @param fullSpan Whether to span full width.
     * @param onBind The binding logic.
     */
    inline fun <reified VB : ViewBinding> render(
        noinline inflate: Inflate<VB>,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onBind: VB.() -> Unit
    ) {
        val stableKey = contentType ?: VB::class.java
        val data = currentData ?: Unit
        
        internalRender(
            factory = { p -> 
                val binding = inflate(LayoutInflater.from(p.context), p, false)
                SmartViewHolder(binding.root, binding)
            },
            bind = { h -> (h.binding as VB).onBind() },
            key = stableKey,
            data = data,
            id = currentId ?: System.identityHashCode(data),
            span = span,
            fullSpan = fullSpan
        )
    }

    /**
     * Renders a UI unit within an advanced [items] block using a Custom [View].
     *
     * @param create The View creator.
     * @param contentType An optional explicit key for ViewType pooling.
     * @param span The span size.
     * @param fullSpan Whether to span full width.
     * @param onBind The binding logic.
     */
    inline fun <reified V : View> render(
        noinline create: ViewCreator<V>,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onBind: V.() -> Unit
    ) {
        val stableKey = contentType ?: V::class.java
        val data = currentData ?: Unit

        internalRender(
            factory = { p -> createSafeViewHolder(p, create) },
            bind = { h -> (h.view as V).onBind() },
            key = stableKey,
            data = data,
            id = currentId ?: System.identityHashCode(data),
            span = span,
            fullSpan = fullSpan
        )
    }

    // ============================================================================================
    //  Internal Implementation (Private)
    // ============================================================================================

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
        bind: (SmartViewHolder) -> Unit,
        key: Any,
        data: Any,
        id: Any,
        span: Int,
        fullSpan: Boolean
    ) {
        val viewType = adapter.getOrCreateViewType(key, factory)
        newWrappers.add(ItemWrapper(
            id = id,
            viewType = viewType,
            data = data,
            span = span,
            fullSpan = fullSpan,
            factory = factory,
            bind = bind
        ))
    }
}
