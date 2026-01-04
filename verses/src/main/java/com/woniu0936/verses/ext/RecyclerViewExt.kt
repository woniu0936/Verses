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
 * Convenience DSL for vertical column lists (LazyColumn).
 */
fun RecyclerView.composeColumn(
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    block: VerseScope.() -> Unit
) = compose(RecyclerView.VERTICAL, reverseLayout, spacing, contentPadding, horizontalPadding, verticalPadding, block)

/**
 * Convenience DSL for horizontal row lists (LazyRow).
 */
fun RecyclerView.composeRow(
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    block: VerseScope.() -> Unit
) = compose(RecyclerView.HORIZONTAL, reverseLayout, spacing, contentPadding, horizontalPadding, verticalPadding, block)

/**
 * Entry point for vertical grid layouts (LazyVerticalGrid).
 */
fun RecyclerView.composeVerticalGrid(
    spanCount: Int,
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    horizontalSpacing: Int? = null,
    verticalSpacing: Int? = null,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    block: VerseScope.() -> Unit
) {
    internalComposeGrid(
        spanCount = spanCount,
        orientation = RecyclerView.VERTICAL,
        reverseLayout = reverseLayout,
        spacing = spacing,
        horizontalSpacing = horizontalSpacing,
        verticalSpacing = verticalSpacing,
        contentPadding = contentPadding,
        horizontalPadding = horizontalPadding,
        verticalPadding = verticalPadding,
        block = block
    )
}

/**
 * Entry point for horizontal grid layouts (LazyHorizontalGrid).
 */
fun RecyclerView.composeHorizontalGrid(
    spanCount: Int,
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    horizontalSpacing: Int? = null,
    verticalSpacing: Int? = null,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    block: VerseScope.() -> Unit
) {
    internalComposeGrid(
        spanCount = spanCount,
        orientation = RecyclerView.HORIZONTAL,
        reverseLayout = reverseLayout,
        spacing = spacing,
        horizontalSpacing = horizontalSpacing,
        verticalSpacing = verticalSpacing,
        contentPadding = contentPadding,
        horizontalPadding = horizontalPadding,
        verticalPadding = verticalPadding,
        block = block
    )
}

/**
 * Entry point for vertical staggered grid layouts (LazyVerticalStaggeredGrid).
 */
fun RecyclerView.composeVerticalStaggeredGrid(
    spanCount: Int,
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
    internalComposeStaggered(
        spanCount = spanCount,
        orientation = RecyclerView.VERTICAL,
        reverseLayout = reverseLayout,
        spacing = spacing,
        horizontalSpacing = horizontalSpacing,
        verticalSpacing = verticalSpacing,
        contentPadding = contentPadding,
        horizontalPadding = horizontalPadding,
        verticalPadding = verticalPadding,
        gapStrategy = gapStrategy,
        block = block
    )
}

/**
 * Entry point for horizontal staggered grid layouts (LazyHorizontalStaggeredGrid).
 */
fun RecyclerView.composeHorizontalStaggeredGrid(
    spanCount: Int,
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
    internalComposeStaggered(
        spanCount = spanCount,
        orientation = RecyclerView.HORIZONTAL,
        reverseLayout = reverseLayout,
        spacing = spacing,
        horizontalSpacing = horizontalSpacing,
        verticalSpacing = verticalSpacing,
        contentPadding = contentPadding,
        horizontalPadding = horizontalPadding,
        verticalPadding = verticalPadding,
        gapStrategy = gapStrategy,
        block = block
    )
}

// Private Helpers

@PublishedApi
internal fun RecyclerView.internalComposeGrid(
    spanCount: Int,
    orientation: Int,
    reverseLayout: Boolean,
    spacing: Int,
    horizontalSpacing: Int?,
    verticalSpacing: Int?,
    contentPadding: Int,
    horizontalPadding: Int?,
    verticalPadding: Int?,
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
                    val currentAdapter = this@internalComposeGrid.adapter as? VerseAdapter
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

@PublishedApi
internal fun RecyclerView.internalComposeStaggered(
    spanCount: Int,
    orientation: Int,
    reverseLayout: Boolean,
    spacing: Int,
    horizontalSpacing: Int?,
    verticalSpacing: Int?,
    contentPadding: Int,
    horizontalPadding: Int?,
    verticalPadding: Int?,
    gapStrategy: Int,
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

/**
 * Reconciles the [RecyclerView]'s adapter and layout manager state.
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
    
    // Automatically inject the context-scoped shared pool for transparent performance optimization.
    this.setRecycledViewPool(VerseAdapter.getPool(context))
    updateDecoration(hS, vS, hP, vP)
    
    // Disable flickering 'change' animations by default.
    (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    
    // Industrial-Grade Lifecycle & Attachment Safety
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
        }
    })
    
    return newAdapter
}

/**
 * Manages the singleton [VerseSpacingDecoration] for the [RecyclerView].
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
 */
@PublishedApi
internal inline fun submit(adapter: VerseAdapter, block: VerseScope.() -> Unit) {
    val scope = VerseScope(adapter)
    scope.block()
    adapter.submitList(scope.newWrappers)
}