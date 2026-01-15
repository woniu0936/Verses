package com.woniu0936.verses.dsl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.core.pool.VerseTypeRegistry
import com.woniu0936.verses.core.Verses
import com.woniu0936.verses.core.VersesLogger
import com.woniu0936.verses.model.*

/**
 * The core DSL scope for building RecyclerView content declaratively.
 */
@VerseDsl
class VerseScope @PublishedApi internal constructor(
    @PublishedApi internal val adapter: VerseAdapter
) {

    @PublishedApi
    internal val newModels = ArrayList<VerseModel<*>>(32)
    
    // Tracks keys within this scope to prevent duplicates.
    private val seenKeys = HashSet<Any>(32)

    @PublishedApi internal var currentData: Any? = null
    @PublishedApi internal var currentId: Any? = null

    /**
     * Resets the scope for reuse, avoiding new object allocations.
     */
    @PublishedApi
    internal fun clear() {
        newModels.clear()
        seenKeys.clear()
        currentData = null
        currentId = null
    }

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
     * @param key A stable unique ID for this item. Mandatory.
     */
    inline fun <reified VB : ViewBinding> item(
        key: Any,
        noinline inflate: Inflate<VB>,
        @LayoutRes layoutRes: Int = 0,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        noinline onCreate: (VB.(SmartViewHolder) -> Unit)? = null,
        crossinline onBind: VB.() -> Unit = {}
    ) {
        item<Unit, VB>(
            key = key,
            inflate = inflate,
            data = Unit,
            layoutRes = layoutRes,
            contentType = contentType,
            span = span,
            fullSpan = fullSpan,
            onClick = onClick?.let { { it() } },
            onAttach = onAttach?.let { { it() } },
            onDetach = onDetach?.let { { it() } },
            onCreate = onCreate,
            onBind = { onBind() }
        )
    }

    /**
     * Renders a single item using ViewBinding with a data object.
     * @param key A stable unique ID for this item. Mandatory.
     */
    inline fun <T : Any, reified VB : ViewBinding> item(
        key: Any,
        noinline inflate: Inflate<VB>,
        data: T,
        @LayoutRes layoutRes: Int = 0,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: ((T) -> Unit)? = null,
        noinline onAttach: ((T) -> Unit)? = null,
        noinline onDetach: ((T) -> Unit)? = null,
        noinline onCreate: (VB.(SmartViewHolder) -> Unit)? = null,
        crossinline onBind: VB.(T) -> Unit = {}
    ) {
        val layoutKey = contentType ?: VB::class.java
        internalRender(
            factory = { p -> 
                val binding = inflate(LayoutInflater.from(p.context), p, false)
                SmartViewHolder(binding.root, binding)
            },
            bind = { d -> 
                @Suppress("UNCHECKED_CAST")
                (binding as VB).onBind(d as T)
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
            id = key,
            span = span,
            fullSpan = fullSpan,
            onClick = onClick?.let { { it(data) } },
            onAttach = onAttach?.let { { it(data) } },
            onDetach = onDetach?.let { { it(data) } }
        )
    }

    /**
     * Renders a single item using a Custom View.
     * @param key A stable unique ID for this item. Mandatory.
     */
    inline fun <reified V : View> item(
        key: Any,
        noinline create: ViewCreator<V>,
        @LayoutRes layoutRes: Int = 0,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: (() -> Unit)? = null,
        noinline onAttach: (() -> Unit)? = null,
        noinline onDetach: (() -> Unit)? = null,
        noinline onCreate: (V.(SmartViewHolder) -> Unit)? = null,
        crossinline onBind: V.() -> Unit = {}
    ) {
        item<Unit, V>(
            key = key,
            create = create,
            data = Unit,
            layoutRes = layoutRes,
            contentType = contentType,
            span = span,
            fullSpan = fullSpan,
            onClick = onClick?.let { { it() } },
            onAttach = onAttach?.let { { it() } },
            onDetach = onDetach?.let { { it() } },
            onCreate = onCreate,
            onBind = { onBind() }
        )
    }

    /**
     * Renders a single item using a Custom View with a data object.
     * @param key A stable unique ID for this item. Mandatory.
     */
    inline fun <T : Any, reified V : View> item(
        key: Any,
        noinline create: ViewCreator<V>,
        data: T,
        @LayoutRes layoutRes: Int = 0,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onClick: ((T) -> Unit)? = null,
        noinline onAttach: ((T) -> Unit)? = null,
        noinline onDetach: ((T) -> Unit)? = null,
        noinline onCreate: (V.(SmartViewHolder) -> Unit)? = null,
        crossinline onBind: V.(T) -> Unit = {}
    ) {
        val layoutKey = contentType ?: V::class.java
        internalRender(
            factory = { p -> createSafeViewHolder(p, create) },
            bind = { d -> 
                @Suppress("UNCHECKED_CAST")
                (view as V).onBind(d as T)
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
            id = key,
            span = span,
            fullSpan = fullSpan,
            onClick = onClick?.let { { it(data) } },
            onAttach = onAttach?.let { { it(data) } },
            onDetach = onDetach?.let { { it(data) } }
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
        // [Smart Deduping] Prevent duplicate keys that cause DiffUtil inconsistencies.
        if (seenKeys.contains(id)) {
            val message = "Verses Error: Duplicate key detected: '$id'. " +
                    "Each item in a list must have a unique stable key. " +
                    "Check your 'items(..., key = { ... })' or 'item(key = ...)' calls."
            
            if (Verses.getConfig().isDebug) {
                throw IllegalArgumentException(message)
            } else {
                VersesLogger.e(message, IllegalStateException("Duplicate Key: $id"))
                return // Skip this item to prevent Crash in Release
            }
        }
        
        seenKeys.add(id)

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
