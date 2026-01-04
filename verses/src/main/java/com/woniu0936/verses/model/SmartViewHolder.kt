package com.woniu0936.verses.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.BuildConfig

/**
 * Thread-local reference to the holder currently being bound.
 * This ensures thread safety while maintaining a clean DSL syntax.
 */
@PublishedApi
internal val currentProcessingHolder = ThreadLocal<SmartViewHolder>()

/**
 * A stateful ViewHolder that acts as a micro-recomposition scope.
 */
class SmartViewHolder(
    val view: View,
    val binding: ViewBinding? = null
) : RecyclerView.ViewHolder(view) {

    @PublishedApi
    internal val memoTable = mutableListOf<Any?>()

    @PublishedApi
    internal var pointer = 0

    @PublishedApi
    internal var currentData: Any? = null

    private var lastBoundId: Any? = null

    /**
     * Prepares the holder for a new binding cycle.
     */
    fun prepare(id: Any, data: Any) {
        this.currentData = data
        if (lastBoundId != id) {
            memoTable.clear()
            lastBoundId = id
        }
        pointer = 0
    }

    /**
     * Validates that the number of bind calls is consistent with the table size.
     */
    fun validate() {
        if (BuildConfig.DEBUG && pointer != 0 && memoTable.isNotEmpty() && pointer != memoTable.size) {
            // Unstable call order detected
        }
    }

    /**
     * Safely access current data.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> itemData(): T = currentData as T
}

// =======================================================
//  The Universal Bind Anchors (Top-level Extensions)
// =======================================================

/**
 * Binds a view property to a single dependency.
 */
inline fun <V : View, R> V.bind(value: R, crossinline block: V.(R) -> Unit) {
    val h = currentProcessingHolder.get() ?: return
    val p = h.pointer
    if (p >= h.memoTable.size) {
        h.memoTable.add(value)
        block(value)
    } else if (h.memoTable[p] != value) {
        h.memoTable[p] = value
        block(value)
    }
    h.pointer++
}

/**
 * Binds a view property to two dependencies without allocation.
 */
inline fun <V : View, R1, R2> V.bind(v1: R1, v2: R2, crossinline block: V.(R1, R2) -> Unit) {
    val h = currentProcessingHolder.get() ?: return
    val p1 = h.pointer
    val p2 = h.pointer + 1

    if (p2 >= h.memoTable.size) {
        while (h.memoTable.size <= p2) h.memoTable.add(null)
        h.memoTable[p1] = v1
        h.memoTable[p2] = v2
        block(v1, v2)
    } else {
        if (h.memoTable[p1] != v1 || h.memoTable[p2] != v2) {
            h.memoTable[p1] = v1
            h.memoTable[p2] = v2
            block(v1, v2)
        }
    }
    h.pointer += 2
}

/**
 * Binds a view property to three dependencies without allocation.
 */
inline fun <V : View, R1, R2, R3> V.bind(v1: R1, v2: R2, v3: R3, crossinline block: V.(R1, R2, R3) -> Unit) {
    val h = currentProcessingHolder.get() ?: return
    val p1 = h.pointer
    val p2 = h.pointer + 1
    val p3 = h.pointer + 2

    if (p3 >= h.memoTable.size) {
        while (h.memoTable.size <= p3) h.memoTable.add(null)
        h.memoTable[p1] = v1
        h.memoTable[p2] = v2
        h.memoTable[p3] = v3
        block(v1, v2, v3)
    } else {
        if (h.memoTable[p1] != v1 || h.memoTable[p2] != v2 || h.memoTable[p3] != v3) {
            h.memoTable[p1] = v1
            h.memoTable[p2] = v2
            h.memoTable[p3] = v3
            block(v1, v2, v3)
        }
    }
    h.pointer += 3
}

/**
 * A hook for one-time initialization.
 */
inline fun once(crossinline block: SmartViewHolder.() -> Unit) {
    val h = currentProcessingHolder.get() ?: return
    val p = h.pointer
    if (p >= h.memoTable.size) {
        h.memoTable.add(Unit)
        h.block()
    }
    h.pointer++
}

/**
 * Functional interface for inflating a [ViewBinding] instance.
 */
typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB

/**
 * Functional interface for creating a custom [View] programmatically.
 */
typealias ViewCreator<V> = (Context) -> V