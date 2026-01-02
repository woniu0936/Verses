package com.woniu0936.verses.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.*
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.core.VerseSpacingDecoration
import com.woniu0936.verses.dsl.VerseScope

/**
 * Entry point for building a linear [RecyclerView] layout using the Verses DSL.
 * 
 * Automatically configures the [LinearLayoutManager] and [VerseAdapter].
 *
 * @param orientation Layout direction (default [RecyclerView.VERTICAL]).
 * @param reverseLayout Whether to layout from end to start.
 * @param spacing Inter-item gap in pixels (auto-mapped to the scroll axis).
 * @param contentPadding Outer margin around the entire list in pixels.
 * @param horizontalPadding Overrides horizontal content padding if provided.
 * @param verticalPadding Overrides vertical content padding if provided.
 * @param block The DSL block to define list items.
 */
fun RecyclerView.compose(
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    block: VerseScope.() -> Unit
) {
    val hP = horizontalPadding ?: contentPadding
    val vP = verticalPadding ?: contentPadding
    val adapter = getOrCreateAdapter(spacing, spacing, hP, vP) {
        LinearLayoutManager(context, orientation, reverseLayout)
    }
    
    (layoutManager as? LinearLayoutManager)?.let {
        if (it.orientation != orientation) it.orientation = orientation
        if (it.reverseLayout != reverseLayout) it.reverseLayout = reverseLayout
    }
    
    submit(adapter, block)
}

/**
 * Convenience DSL for vertical column lists.
 */
fun RecyclerView.composeLinearColumn(
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    block: VerseScope.() -> Unit
) = compose(RecyclerView.VERTICAL, reverseLayout, spacing, contentPadding, horizontalPadding, verticalPadding, block)

/**
 * Convenience DSL for horizontal row lists.
 */
fun RecyclerView.composeLinearRow(
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    block: VerseScope.() -> Unit
) = compose(RecyclerView.HORIZONTAL, reverseLayout, spacing, contentPadding, horizontalPadding, verticalPadding, block)

/**
 * Entry point for building a grid [RecyclerView] layout.
 * 
 * Automatically handles [GridLayoutManager.SpanSizeLookup] to support [ItemWrapper.fullSpan].
 *
 * @param spanCount Number of columns.
 * @param spacing Shorthand for both horizontal and vertical item gaps.
 * @param horizontalSpacing Overrides column gap if provided.
 * @param verticalSpacing Overrides row gap if provided.
 */
fun RecyclerView.composeGrid(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    horizontalSpacing: Int? = null,
    verticalSpacing: Int? = null,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    block: VerseScope.() -> Unit
) {
    val hS = horizontalSpacing ?: spacing
    val vS = verticalSpacing ?: spacing
    val hP = horizontalPadding ?: contentPadding
    val vP = verticalPadding ?: contentPadding
    
    val adapter = getOrCreateAdapter(hS, vS, hP, vP) {
        GridLayoutManager(context, spanCount, orientation, reverseLayout).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val currentAdapter = this@composeGrid.adapter as? VerseAdapter
                    return currentAdapter?.getSpanSize(position, spanCount) ?: 1
                }
            }
        }
    }
    
    (layoutManager as? GridLayoutManager)?.let {
        if (it.spanCount != spanCount) it.spanCount = spanCount
        if (it.orientation != orientation) it.orientation = orientation
        if (it.reverseLayout != reverseLayout) it.reverseLayout = reverseLayout
    }
    
    submit(adapter, block)
}

/**
 * Entry point for staggered grid [RecyclerView] layouts.
 */
fun RecyclerView.composeStaggered(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    horizontalSpacing: Int? = null,
    verticalSpacing: Int? = null,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE,
    block: VerseScope.() -> Unit
) {
    val hS = horizontalSpacing ?: spacing
    val vS = verticalSpacing ?: spacing
    val hP = horizontalPadding ?: contentPadding
    val vP = verticalPadding ?: contentPadding
    
    val adapter = getOrCreateAdapter(hS, vS, hP, vP) {
        StaggeredGridLayoutManager(spanCount, orientation).apply {
            this.gapStrategy = gapStrategy
        }
    }
    
    (layoutManager as? StaggeredGridLayoutManager)?.let {
        if (it.spanCount != spanCount) it.spanCount = spanCount
        if (it.orientation != orientation) it.orientation = orientation
        if (it.reverseLayout != reverseLayout) it.reverseLayout = reverseLayout
        if (it.gapStrategy != gapStrategy) it.gapStrategy = gapStrategy
    }
    
    submit(adapter, block)
}

// Private Helpers

/**
 * Reconciles the [RecyclerView]'s adapter and layout manager state.
 *
 * If a [VerseAdapter] already exists and the layout manager type matches, it updates
 * existing decorations. Otherwise, it performs a full setup including:
 * 1. Global ViewPool injection for cross-instance optimization.
 * 2. Automatic lifecycle-aware disposal to prevent memory leaks.
 * 3. Default animation tuning (disabling change animations).
 */
@PublishedApi
internal fun RecyclerView.getOrCreateAdapter(
    hS: Int,
    vS: Int,
    hP: Int,
    vP: Int,
    createLayoutManager: () -> RecyclerView.LayoutManager
): VerseAdapter {
    val currentAdapter = this.adapter as? VerseAdapter
    val newLM = createLayoutManager()
    
    if (currentAdapter != null && layoutManager != null) {
        if (layoutManager!!.javaClass == newLM.javaClass) {
            updateDecoration(hS, vS, hP, vP)
            return currentAdapter
        }
    }

    val newAdapter = VerseAdapter()
    this.layoutManager = newLM
    this.adapter = newAdapter
    
    // Automatically inject the global shared pool for transparent performance optimization.
    this.setRecycledViewPool(VerseAdapter.globalPool)
    updateDecoration(hS, vS, hP, vP)
    
    // Disable flickering 'change' animations by default.
    (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    
    // Industrial-Grade Lifecycle & Attachment Safety:
    // We ensure the adapter is cleared when either the Lifecycle is destroyed 
    // OR the View is detached from the window, providing double protection against leaks.
    val cleanup = {
        this.adapter = null
        this.setRecycledViewPool(null) // Disconnect from global pool
    }

    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            cleanup()
        }
    }

    val lifecycle = (context as? LifecycleOwner)?.lifecycle
    lifecycle?.addObserver(observer)

    this.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: android.view.View) {}
        override fun onViewDetachedFromWindow(v: android.view.View) {
            this@getOrCreateAdapter.removeOnAttachStateChangeListener(this)
            lifecycle?.removeObserver(observer)
            // Note: We don't call cleanup() here because a View can be detached/reattached (e.g. Fragment).
            // But we remove the observer to prevent it from piling up if compose() is called again.
        }
    })
    
    return newAdapter
}

/**
 * Manages the singleton [VerseSpacingDecoration] for the [RecyclerView].
 *
 * This function is idempotent; it removes existing decorations before adding a new one
 * to ensure that spacing does not accumulate when `compose` is called multiple times.
 */
@PublishedApi
internal fun RecyclerView.updateDecoration(hS: Int, vS: Int, hP: Int, vP: Int) {
    for (i in itemDecorationCount - 1 downTo 0) {
        if (getItemDecorationAt(i) is VerseSpacingDecoration) {
            removeItemDecorationAt(i)
        }
    }
    if (hS > 0 || vS > 0 || hP > 0 || vP > 0) {
        addItemDecoration(VerseSpacingDecoration(hS, vS, hP, vP))
    }
}

/**
 * Orchestrates the DSL execution and item submission.
 *
 * This function creates a transient [VerseScope] and submits the collected [ItemWrapper]
 * list to the adapter.
 */
@PublishedApi
internal inline fun submit(adapter: VerseAdapter, block: VerseScope.() -> Unit) {
    val scope = VerseScope(adapter)
    scope.block()
    adapter.submitList(scope.newWrappers)
}
