package com.woniu0936.verses.dsl

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.model.Inflate
import com.woniu0936.verses.model.ItemWrapper
import com.woniu0936.verses.model.SmartViewHolder

/**
 * DSL scope for building a declarative [androidx.recyclerview.widget.RecyclerView] list.
 *
 * This scope provides two sets of APIs:
 * 1. **Simple Mode**: [items] and [item] for direct mapping between data and [androidx.viewbinding.ViewBinding].
 * 2. **Advanced Mode**: A nested [items] block combined with [render] for complex conditional logic.
 *
 * @property adapter The associated [VerseAdapter] used for ViewType management.
 */
@VerseDsl
class VerseScope @PublishedApi internal constructor(
    @PublishedApi internal val adapter: VerseAdapter
) {

    /**
     * Temporary storage for the rendering units built within this scope.
     */
    @PublishedApi
    internal val newWrappers = mutableListOf<ItemWrapper>()

    /**
     * Holds the data object currently being processed in an [items] block (Advanced Mode).
     */
    @PublishedApi
    internal var currentData: Any? = null

    /**
     * Holds the unique ID currently being processed in an [items] block (Advanced Mode).
     */
    @PublishedApi
    internal var currentId: Any? = null

    // =======================================================
    //  API 1.0: Simple Mode (Direct Inflate)
    // =======================================================

    /**
     * Renders a list of items using the same [androidx.viewbinding.ViewBinding] strategy.
     *
     * @param T The type of the data items.
     * @param VB The type of the [androidx.viewbinding.ViewBinding].
     * @param items The list of data objects to render.
     * @param inflate The [androidx.viewbinding.ViewBinding] inflate function reference.
     * @param key A lambda to extract a unique ID from each item. Highly recommended for animations.
     * @param span The number of columns each item occupies (for Grid layouts).
     * @param fullSpan Whether each item should occupy the full width.
     * @param onBind Callback for binding data to the [androidx.viewbinding.ViewBinding] instance.
     */
    fun <T : Any, VB : ViewBinding> items(
        items: List<T>,
        inflate: Inflate<VB>,
        key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        onBind: VB.(T) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            internalRender(
                inflate = inflate,
                contentType = null,
                data = item,
                id = key?.invoke(item) ?: index,
                span = span,
                fullSpan = fullSpan,
                onBind = { vb -> vb.onBind(item) }
            )
        }
    }

    /**
     * Renders a single item (e.g., a Header, Footer, or Banner).
     *
     * @param VB The type of the [androidx.viewbinding.ViewBinding].
     * @param inflate The [androidx.viewbinding.ViewBinding] inflate function reference.
     * @param data The data object for this item. Defaults to [Unit]. Pass a value to trigger [androidx.recyclerview.widget.DiffUtil] updates.
     * @param key A unique identifier for this item.
     * @param span The number of columns this item occupies.
     * @param fullSpan Whether this item should occupy the full width.
     * @param onBind Callback for binding data to the [androidx.viewbinding.ViewBinding] instance.
     */
    fun <VB : ViewBinding> item(
        inflate: Inflate<VB>,
        data: Any? = Unit,
        key: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        onBind: VB.() -> Unit = {}
    ) {
        internalRender(
            inflate = inflate,
            contentType = null,
            data = data ?: Unit,
            id = key ?: "single_${inflate.hashCode()}",
            span = span,
            fullSpan = fullSpan,
            onBind = { vb -> vb.onBind() }
        )
    }

    // =======================================================
    //  API 2.0: Advanced Mode (Control Flow + Render)
    // =======================================================

    /**
     * Iterates over a list of items to allow conditional rendering logic within the [block].
     *
     * Example:
     * ```
     * items(myList) { item ->
     *     if (item is User) {
     *         render(ItemUserBinding::inflate) { ... }
     *     } else {
     *         render(ItemAdBinding::inflate, fullSpan = true) { ... }
     *     }
     * }
     * ```
     *
     * @param T The type of the data items.
     * @param items The list of data objects.
     * @param key A lambda to extract a unique ID from each item.
     * @param block A DSL block where [render] is called.
     */
    inline fun <T : Any> items(
        items: List<T>,
        noinline key: ((T) -> Any)? = null,
        block: VerseScope.(T) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            currentData = item
            currentId = key?.invoke(item) ?: index
            block(item)
        }
    }

    /**
     * Declares a rendering strategy for the current item in a multi-type list.
     *
     * Must be called within the [items] block.
     *
     * @param VB The type of the [androidx.viewbinding.ViewBinding].
     * @param inflate The [androidx.viewbinding.ViewBinding] inflate function reference.
     * @param contentType A unique key for identifying the view type. Required if [inflate] is a dynamic lambda.
     * @param span The number of columns this item occupies.
     * @param fullSpan Whether this item should occupy the full width.
     * @param onBind Callback for binding data to the [androidx.viewbinding.ViewBinding] instance.
     */
    fun <VB : ViewBinding> render(
        inflate: Inflate<VB>,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        onBind: VB.() -> Unit
    ) {
        internalRender(
            inflate = inflate,
            contentType = contentType,
            data = currentData ?: Unit,
            id = currentId ?: System.identityHashCode(currentData),
            span = span,
            fullSpan = fullSpan,
            onBind = { vb -> vb.onBind() }
        )
    }

    /**
     * Internal helper to create and add an [ItemWrapper] to the current scope.
     *
     * @param inflate The view binding inflate function.
     * @param contentType Optional key for view type identification.
     * @param data The data object associated with this item.
     * @param id The unique identifier for DiffUtil.
     * @param span The column span size.
     * @param fullSpan Whether to force full width.
     * @param onBind The binding logic lambda.
     */
    private fun <VB : ViewBinding> internalRender(
        inflate: Inflate<VB>,
        contentType: Any?,
        data: Any,
        id: Any,
        span: Int,
        fullSpan: Boolean,
        onBind: (VB) -> Unit
    ) {
        // Decide which key to use for ViewType caching. 
        // Prioritize explicit contentType, otherwise use the inflate function reference.
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