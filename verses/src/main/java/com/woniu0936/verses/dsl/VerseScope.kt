package com.woniu0936.verses.dsl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.model.Inflate
import com.woniu0936.verses.model.ItemWrapper
import com.woniu0936.verses.model.SmartViewHolder
import com.woniu0936.verses.model.ViewCreator

@VerseDsl
class VerseScope @PublishedApi internal constructor(
    @PublishedApi internal val adapter: VerseAdapter
) {
    @PublishedApi
    internal val newWrappers = mutableListOf<ItemWrapper>()

    @PublishedApi
    internal var currentData: Any? = null

    @PublishedApi
    internal var currentId: Any? = null

    // =======================================================
    //  Part 1: ViewBinding Support
    // =======================================================

    /**
     * [List] ViewBinding list.
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
     * [Single] ViewBinding item.
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
    //  Part 2: Custom View Support
    // =======================================================

    /**
     * [List] Custom View list.
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
     * [Single] Custom View item.
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
    //  Part 3: Advanced Mode (If/Else Render)
    // =======================================================

    /**
     * Iterator for custom control flow.
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
     * [Render] instruction for ViewBinding.
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
     * [Render] instruction for Custom View.
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
        newWrappers.add(
            ItemWrapper(
                id = id,
                viewType = viewType,
                data = data,
                span = span,
                fullSpan = fullSpan,
                factory = factory,
                bind = bind,
                onClick = onClick
            )
        )
    }
}