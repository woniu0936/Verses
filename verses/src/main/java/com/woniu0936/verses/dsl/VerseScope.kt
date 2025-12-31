package com.woniu0936.verses.dsl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.model.*

@VerseDsl
class VerseScope @PublishedApi internal constructor(
    @PublishedApi internal val adapter: VerseAdapter
) {

    @PublishedApi
    internal val newWrappers = mutableListOf<ItemWrapper>()

    // Context variables (for advanced mode render)
    @PublishedApi
    internal var currentData: Any? = null
    @PublishedApi
    internal var currentId: Any? = null

    // =======================================================
    //  Part 1: ViewBinding Support (Reified Safety)
    // =======================================================

    /**
     * [List] ViewBinding list.
     * @param noinline inflate: Must be noinline to be stored in the lambda.
     */
    inline fun <T : Any, reified VB : ViewBinding> items(
        items: List<T>,
        noinline inflate: Inflate<VB>,
        noinline key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onBind: VB.(T) -> Unit
    ) {
        // ✨ MAGIC: Use ViewBinding Class as stable Key
        val stableKey = VB::class.java
        
        items.forEachIndexed { index, item ->
            internalRender(
                factory = { p -> 
                    val binding = inflate(LayoutInflater.from(p.context), p, false)
                    SmartViewHolder(binding.root, binding) 
                },
                bind = { h -> (h.binding as VB).onBind(item) },
                key = stableKey, // Pass Class Key
                data = item,
                id = key?.invoke(item) ?: index,
                span = span,
                fullSpan = fullSpan
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
        fullSpan: Boolean = true, // Single item defaults to full span usually
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

    // =======================================================
    //  Part 2: Custom View Support (Reified Safety)
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
        noinline onBind: V.(T) -> Unit
    ) {
        // ✨ MAGIC: Use View Class as stable Key
        val stableKey = V::class.java
        
        items.forEachIndexed { index, item ->
            internalRender(
                factory = { p -> createSafeViewHolder(p, create) },
                bind = { h -> (h.view as V).onBind(item) },
                key = stableKey, // Pass Class Key
                data = item,
                id = key?.invoke(item) ?: index,
                span = span,
                fullSpan = fullSpan
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

    // =======================================================
    //  Part 3: Advanced Mode (If/Else Render)
    // =======================================================

    /**
     * Iterator: Only used to open control flow.
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
     * [Render] instruction: ViewBinding support.
     */
    inline fun <reified VB : ViewBinding> render(
        noinline inflate: Inflate<VB>,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onBind: VB.() -> Unit
    ) {
        // Prefer contentType (escape hatch), otherwise Class
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
     * [Render] instruction: Custom View support.
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

    // =======================================================
    //  Internal Helpers
    // =======================================================

    @PublishedApi
    internal fun <V : View> createSafeViewHolder(parent: ViewGroup, create: ViewCreator<V>): SmartViewHolder {
        val view = create(parent.context)
        // Automatically ensure LayoutParams to prevent crashes
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
        // Convert Class Key to Int ID
        val viewType = adapter.getOrCreateViewType(key)
        
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
