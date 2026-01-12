package com.woniu0936.verses.dsl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.core.pool.VerseTypeRegistry
import com.woniu0936.verses.model.*

/**
 * The core DSL scope for building RecyclerView content declaratively.
 */
@VerseDsl
class VerseScope @PublishedApi internal constructor(
    @PublishedApi internal val adapter: VerseAdapter
) {

    @PublishedApi
    internal val newModels = mutableListOf<VerseModel<*>>()

    @PublishedApi internal var currentData: Any? = null
    @PublishedApi internal var currentId: Any? = null

    // ============================================================================================
    //  Group 1: Standard List (1:1 Mapping)
    // ============================================================================================

    /**
     * Renders a list of items using ViewBinding.
     * @param key A function to extract a stable ID from each item. Mandatory for DiffUtil and State Saving.
     */
    inline fun <T : Any, reified VB : ViewBinding> items(
        items: List<T>,
        noinline key: (T) -> Any,
        noinline inflate: Inflate<VB>,
        @LayoutRes layoutRes: Int = 0,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: ((T) -> Unit)? = null,
        noinline onAttach: ((T) -> Unit)? = null,
        noinline onDetach: ((T) -> Unit)? = null,
        noinline onCreate: (VB.(SmartViewHolder) -> Unit)? = null,
        crossinline onBind: VB.(T) -> Unit
    ) {
        val layoutKey = contentType ?: VB::class.java
        items.forEach { item ->
            internalRender(
                factory = { p -> 
                    val binding = inflate(LayoutInflater.from(p.context), p, false)
                    SmartViewHolder(binding.root, binding)
                },
                bind = { data ->
                    @Suppress("UNCHECKED_CAST")
                    (binding as VB).onBind(data as T)
                },
                onCreate = onCreate?.let { block ->
                    { 
                        @Suppress("UNCHECKED_CAST")
                        (binding as VB).block(this)
                    }
                },
                layoutRes = layoutRes,
                layoutKey = layoutKey,
                data = item,
                id = key(item),
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
     * @param key A function to extract a stable ID from each item. Mandatory for DiffUtil and State Saving.
     */
    inline fun <T : Any, reified V : View> items(
        items: List<T>,
        noinline key: (T) -> Any,
        noinline create: ViewCreator<V>,
        @LayoutRes layoutRes: Int = 0,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: ((T) -> Unit)? = null,
        noinline onAttach: ((T) -> Unit)? = null,
        noinline onDetach: ((T) -> Unit)? = null,
        noinline onCreate: (V.(SmartViewHolder) -> Unit)? = null,
        crossinline onBind: V.(T) -> Unit
    ) {
        val layoutKey = contentType ?: V::class.java
        items.forEach { item ->
            internalRender(
                factory = { p -> createSafeViewHolder(p, create) },
                bind = { data ->
                    @Suppress("UNCHECKED_CAST")
                    (view as V).onBind(data as T)
                },
                onCreate = onCreate?.let { block ->
                    { 
                        @Suppress("UNCHECKED_CAST")
                        (view as V).block(this)
                    }
                },
                layoutRes = layoutRes,
                layoutKey = layoutKey,
                data = item,
                id = key(item),
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
        @LayoutRes layoutRes: Int = 0,
        data: Any? = Unit,
        key: Any? = null,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        noinline onCreate: (VB.(SmartViewHolder) -> Unit)? = null,
        crossinline onBind: VB.() -> Unit = {}
    ) {
        val layoutKey = contentType ?: VB::class.java
        // Intelligent Default Key: Use explicit key OR data OR layout class
        val finalKey = key ?: data.takeIf { it != Unit && it != null } ?: VB::class.java
        
        internalRender(
            factory = { p -> 
                val binding = inflate(LayoutInflater.from(p.context), p, false)
                SmartViewHolder(binding.root, binding)
            },
            bind = { 
                @Suppress("UNCHECKED_CAST")
                (binding as VB).onBind()
            },
            onCreate = onCreate?.let { block ->
                { 
                    @Suppress("UNCHECKED_CAST")
                    (binding as VB).block(this)
                }
            },
            layoutRes = layoutRes,
            layoutKey = layoutKey,
            data = data ?: Unit,
            id = finalKey,
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
        @LayoutRes layoutRes: Int = 0,
        data: Any? = Unit,
        key: Any? = null,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        noinline onCreate: (V.(SmartViewHolder) -> Unit)? = null,
        crossinline onBind: V.() -> Unit = {}
    ) {
        val layoutKey = contentType ?: V::class.java
        // Intelligent Default Key: Use explicit key OR data OR layout class
        val finalKey = key ?: data.takeIf { it != Unit && it != null } ?: V::class.java

        internalRender(
            factory = { p -> createSafeViewHolder(p, create) },
            bind = { 
                @Suppress("UNCHECKED_CAST")
                (view as V).onBind()
            },
            onCreate = onCreate?.let { block ->
                { 
                    @Suppress("UNCHECKED_CAST")
                    (view as V).block(this)
                }
            },
            layoutRes = layoutRes,
            layoutKey = layoutKey,
            data = data ?: Unit,
            id = finalKey,
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
        key: (T) -> Any,
        block: VerseScope.(T) -> Unit
    ) {
        items.forEach { item ->
            currentData = item
            currentId = key(item)
            block(item)
        }
    }

    inline fun <reified VB : ViewBinding> render(
        noinline inflate: Inflate<VB>,
        @LayoutRes layoutRes: Int = 0,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        noinline onCreate: (VB.(SmartViewHolder) -> Unit)? = null,
        crossinline onBind: VB.() -> Unit
    ) {
        val layoutKey = contentType ?: VB::class.java
        val data = currentData ?: Unit
        // In advanced 'items' block, currentId is guaranteed to be set via 'key' param
        val finalId = currentId ?: throw IllegalStateException("Verses Error: 'render' called outside of 'items' scope or key is missing.")
        
        internalRender(
            factory = { p -> 
                val binding = inflate(LayoutInflater.from(p.context), p, false)
                SmartViewHolder(binding.root, binding)
            },
            bind = { 
                @Suppress("UNCHECKED_CAST")
                (binding as VB).onBind()
            },
            onCreate = onCreate?.let { block ->
                { 
                    @Suppress("UNCHECKED_CAST")
                    (binding as VB).block(this)
                }
            },
            layoutRes = layoutRes,
            layoutKey = layoutKey,
            data = data,
            id = finalId,
            span = span,
            fullSpan = fullSpan,
            onClick = onClick,
            onAttach = onAttach,
            onDetach = onDetach
        )
    }

    inline fun <reified V : View> render(
        noinline create: ViewCreator<V>,
        @LayoutRes layoutRes: Int = 0,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        noinline onCreate: (V.(SmartViewHolder) -> Unit)? = null,
        crossinline onBind: V.() -> Unit
    ) {
        val layoutKey = contentType ?: V::class.java
        val data = currentData ?: Unit
        val finalId = currentId ?: throw IllegalStateException("Verses Error: 'render' called outside of 'items' scope or key is missing.")

        internalRender(
            factory = { p -> createSafeViewHolder(p, create) },
            bind = { 
                @Suppress("UNCHECKED_CAST")
                (view as V).onBind()
            },
            onCreate = onCreate?.let { block ->
                { 
                    @Suppress("UNCHECKED_CAST")
                    (view as V).block(this)
                }
            },
            layoutRes = layoutRes,
            layoutKey = layoutKey,
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
        onCreate: (SmartViewHolder.() -> Unit)? = null,
        @LayoutRes layoutRes: Int,
        layoutKey: Any,
        data: Any,
        id: Any,
        span: Int,
        fullSpan: Boolean,
        onClick: (() -> Unit)?,
        onAttach: (() -> Unit)?,
        onDetach: (() -> Unit)?
    ) {
        val model = DslVerseModel(
            id = id,
            data = data,
            layoutRes = layoutRes,
            layoutKey = layoutKey,
            factory = factory,
            onCreateBlock = onCreate,
            bindBlock = bind,
            span = span,
            fullSpan = fullSpan,
            onClick = onClick,
            onAttach = onAttach,
            onDetach = onDetach
        )
        
        VerseTypeRegistry.registerPrototype(model)
        newModels.add(model)
    }
}
