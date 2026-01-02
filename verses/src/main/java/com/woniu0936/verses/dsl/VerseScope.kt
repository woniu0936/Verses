package com.woniu0936.verses.dsl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.model.*

/**
 * Receiver scope for the Verses DSL.
 * 
 * Provides an expressive API for building heterogeneous lists. This scope acts as a 
 * temporary buffer that collects [ItemWrapper] units before they are submitted 
 * to the underlying [VerseAdapter].
 *
 * @property adapter The associated adapter instance for ViewType generation.
 */
@VerseDsl
class VerseScope @PublishedApi internal constructor(
    @PublishedApi internal val adapter: VerseAdapter
) {
    /**
     * Buffer holding the newly constructed wrappers for the current DSL execution cycle.
     */
    @PublishedApi
    internal val newWrappers = mutableListOf<ItemWrapper>()
    
    /**
     * Temporary storage for the current data item during control flow iterations.
     */
    @PublishedApi
    internal var currentData: Any? = null
    
    /**
     * Temporary storage for the current stable ID during control flow iterations.
     */
    @PublishedApi
    internal var currentId: Any? = null

    // =======================================================
    //  Part 1: ViewBinding Support
    // =======================================================

    /**
     * Declares a list of items using ViewBinding for layout and binding.
     * 
     * @param items The list of business data objects.
     * @param inflate The ViewBinding inflate function (e.g., `ItemUserBinding::inflate`).
     * @param key Optional unique key for stable DiffUtil identity. Defaults to item index.
     * @param span Grid span size (default 1).
     * @param fullSpan Whether to occupy full width in a grid or staggered layout.
     * @param onClick Strongly-typed click callback.
     * @param onBind The binding block where `this` refers to the [ViewBinding].
     */
    inline fun <T : Any, reified VB : ViewBinding> items(
        items: List<T>,
        noinline inflate: Inflate<VB>,
        noinline key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: ((T) -> Unit)? = null,
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
                fullSpan = fullSpan,
                onClick = onClick?.let { { it(item) } }
            )
        }
    }

    /**
     * Declares a single item (e.g., Header, Footer) using ViewBinding.
     */
    inline fun <reified VB : ViewBinding> item(
        noinline inflate: Inflate<VB>,
        data: Any? = Unit,
        key: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: ((Any) -> Unit)? = null,
        noinline onBind: VB.() -> Unit = {}
    ) {
        val stableKey = VB::class.java
        val actualData = data ?: Unit
        internalRender(
            factory = { p ->
                val binding = inflate(LayoutInflater.from(p.context), p, false)
                SmartViewHolder(binding.root, binding)
            },
            bind = { h -> (h.binding as VB).onBind() },
            key = stableKey,
            data = actualData,
            id = key ?: "single_vb_${stableKey.name}",
            span = span,
            fullSpan = fullSpan,
            onClick = onClick?.let { { it(actualData) } }
        )
    }

    // =======================================================
    //  Part 2: Custom View Support (Pure Programmatic)
    // =======================================================

    /**
     * Declares a list of items using programmatically created Views (no XML).
     * 
     * @param create Function to instantiate the View (e.g., `{ context -> TextView(context) }`).
     */
    inline fun <T : Any, reified V : View> items(
        items: List<T>,
        noinline create: ViewCreator<V>,
        noinline key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: ((T) -> Unit)? = null,
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
                fullSpan = fullSpan,
                onClick = onClick?.let { { it(item) } }
            )
        }
    }

    /**
     * Declares a single custom View item.
     */
    inline fun <reified V : View> item(
        noinline create: ViewCreator<V>,
        data: Any? = Unit,
        key: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: ((Any) -> Unit)? = null,
        noinline onBind: V.() -> Unit = {}
    ) {
        val stableKey = V::class.java
        val actualData = data ?: Unit
        internalRender(
            factory = { p -> createSafeViewHolder(p, create) },
            bind = { h -> (h.view as V).onBind() },
            key = stableKey,
            data = actualData,
            id = key ?: "single_view_${stableKey.name}",
            span = span,
            fullSpan = fullSpan,
            onClick = onClick?.let { { it(actualData) } }
        )
    }

    // =======================================================
    //  Part 3: Advanced Mode (Control Flow)
    // =======================================================

    /**
     * Opens a control flow block for conditional rendering within a list.
     * 
     * Inside [block], use `render()` to specify different layouts for different data conditions.
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
     * Renders a layout instruction for the current item in a loop.
     * Used inside [items] block.
     */
    inline fun <reified VB : ViewBinding> render(
        noinline inflate: Inflate<VB>,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: ((Any) -> Unit)? = null,
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
            fullSpan = fullSpan,
            onClick = onClick?.let { { it(data) } }
        )
    }

    /**
     * Renders a custom View instruction for the current item in a loop.
     */
    inline fun <reified V : View> render(
        noinline create: ViewCreator<V>,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: ((Any) -> Unit)? = null,
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
            fullSpan = fullSpan,
            onClick = onClick?.let { { it(data) } }
        )
    }

    // =======================================================
    //  Internal Helpers
    // =======================================================

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
        fullSpan: Boolean,
        onClick: (() -> Unit)? = null
    ) {
        val viewType = VerseAdapter.getGlobalViewType(key, factory)
        newWrappers.add(ItemWrapper(
            id = id,
            viewType = viewType,
            data = data,
            span = span,
            fullSpan = fullSpan,
            factory = factory,
            bind = bind,
            onClick = onClick
        ))
    }
}