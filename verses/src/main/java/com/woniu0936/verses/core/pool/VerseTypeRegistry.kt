package com.woniu0936.verses.core.pool

import com.woniu0936.verses.model.VerseModel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A global registry for RecyclerView ViewTypes.
 * Ensures that identical Layouts or ViewBinding classes share a stable ViewType ID
 * across different [androidx.recyclerview.widget.RecyclerView] instances.
 *
 * It also maintains "prototype" models to enable automatic background pre-inflation.
 */
@PublishedApi
internal object VerseTypeRegistry {
    private val layoutIdToViewType = ConcurrentHashMap<Int, Int>()
    private val classToViewType = ConcurrentHashMap<Class<*>, Int>()
    private val viewTypeToPrototype = ConcurrentHashMap<Int, VerseModel<*>>()
    private val viewTypeGenerator = AtomicInteger(1000)

    /**
     * Obtains a stable ViewType ID for a given layout resource.
     */
    fun getViewType(layoutId: Int): Int {
        return layoutIdToViewType.getOrPut(layoutId) { layoutId }
    }

    /**
     * Obtains a stable ViewType ID for a given factory class (e.g., ViewBinding).
     */
    fun getViewType(clazz: Class<*>): Int {
        return classToViewType.getOrPut(clazz) { viewTypeGenerator.getAndIncrement() }
    }

    /**
     * Registers a model as a prototype for its ViewType if one doesn't exist.
     */
    fun registerPrototype(model: VerseModel<*>) {
        val type = model.getViewType()
        viewTypeToPrototype.putIfAbsent(type, model)
    }

    /**
     * Retrieves the prototype model for a specific ViewType.
     */
    fun getPrototype(viewType: Int): VerseModel<*>? = viewTypeToPrototype[viewType]

    /**
     * Returns all registered prototypes.
     */
    fun getAllPrototypes(): List<VerseModel<*>> = viewTypeToPrototype.values.toList()
}
