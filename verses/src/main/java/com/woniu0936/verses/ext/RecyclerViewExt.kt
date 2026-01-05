package com.woniu0936.verses.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.*
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.core.VerseSpacingDecoration
import com.woniu0936.verses.dsl.VerseScope

/**
 * The foundational entry point for building linear layouts (Lists) using the Verses DSL.
 *
 * This function bridges the declarative DSL block with the imperative RecyclerView system.
 * It automatically reconciles the [LinearLayoutManager] and [VerseAdapter] state to ensure 
 * minimal re-layout overhead.
 *
 * @param orientation Layout direction (default [RecyclerView.VERTICAL]). 
 *                    Use [RecyclerView.VERTICAL] or [RecyclerView.HORIZONTAL].
 * @param reverseLayout Whether to layout items from end to start. Default is false.
 * @param spacing Inter-item gap in pixels between consecutive items.
 * @param contentPadding Uniform outer margin around all four sides of the entire list matrix.
 * @param horizontalPadding Overrides the horizontal (left and right) outer margin if provided.
 * @param verticalPadding Overrides the vertical (top and bottom) outer margin if provided.
 * @param block The declarative DSL block where items are defined using item() or items().
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
 * Semantic DSL for vertical linear lists, aligned with Jetpack Compose's LazyColumn.
 * 
 * @param reverseLayout Whether to layout items from bottom to top. Default is false.
 * @param spacing Vertical gap in pixels between consecutive items.
 * @param contentPadding Uniform outer margin around the entire list.
 * @param horizontalPadding Overrides the horizontal (left and right) outer margin.
 * @param verticalPadding Overrides the vertical (top and bottom) outer margin.
 * @param block The declarative DSL block for item definitions.
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
 * Semantic DSL for horizontal linear lists, aligned with Jetpack Compose's LazyRow.
 * 
 * @param reverseLayout Whether to layout items from right to left. Default is false.
 * @param spacing Horizontal gap in pixels between consecutive items.
 * @param contentPadding Uniform outer margin around the entire list.
 * @param horizontalPadding Overrides the horizontal (left and right) outer margin.
 * @param verticalPadding Overrides the vertical (top and bottom) outer margin.
 * @param block The declarative DSL block for item definitions.
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
 * Configures a vertical grid layout using the Verses DSL.
 * Supports automatic span size lookup for items marked with `fullSpan`.
 * 
 * @param spanCount The number of columns in the grid.
 * @param reverseLayout Whether to layout rows from bottom to top.
 * @param spacing Uniform gap in pixels between items (both horizontal and vertical).
 * @param horizontalSpacing Overrides the horizontal gap between columns if provided.
 * @param verticalSpacing Overrides the vertical gap between rows if provided.
 * @param contentPadding Uniform outer margin around the entire grid matrix.
 * @param horizontalPadding Overrides the horizontal outer margin.
 * @param verticalPadding Overrides the vertical outer margin.
 * @param block The DSL block where grid items are defined.
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
 * Configures a horizontal grid layout using the Verses DSL.
 * Items will flow horizontally across the specified number of rows.
 * 
 * @param spanCount The number of rows in the horizontal grid.
 * @param reverseLayout Whether to layout columns from right to left.
 * @param spacing Uniform gap in pixels between items.
 * @param horizontalSpacing Overrides the horizontal gap between columns.
 * @param verticalSpacing Overrides the vertical gap between rows.
 * @param contentPadding Uniform outer margin around the entire grid.
 * @param horizontalPadding Overrides the horizontal outer margin.
 * @param verticalPadding Overrides the vertical outer margin.
 * @param block The DSL block where grid items are defined.
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
 * Configures a vertical staggered grid layout (Waterfall flow).
 * Useful for flows with varying item heights.
 * 
 * @param spanCount The number of columns.
 * @param reverseLayout Whether to layout from bottom to top.
 * @param spacing Uniform gap in pixels between items.
 * @param horizontalSpacing Overrides the horizontal gap between columns.
 * @param verticalSpacing Overrides the vertical gap between rows.
 * @param contentPadding Uniform outer margin around the matrix.
 * @param horizontalPadding Overrides the horizontal outer margin.
 * @param verticalPadding Overrides the vertical outer margin.
 * @param gapStrategy [StaggeredGridLayoutManager] strategy for handling gaps (e.g. GAP_HANDLING_NONE).
 * @param block The DSL block for item definitions.
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
 * Configures a horizontal staggered grid layout.
 * 
 * @param spanCount The number of rows.
 * @param reverseLayout Whether to layout from right to left.
 * @param spacing Uniform gap in pixels.
 * @param horizontalSpacing Overrides horizontal gap.
 * @param verticalSpacing Overrides vertical gap.
 * @param contentPadding Uniform outer margin.
 * @param horizontalPadding Overrides horizontal outer margin.
 * @param verticalPadding Overrides vertical outer margin.
 * @param gapStrategy [StaggeredGridLayoutManager] gap strategy.
 * @param block The DSL block for item definitions.
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

/**
 * Internal bridge for Grid layout logic. 
 * Injects a custom [GridLayoutManager.SpanSizeLookup] that delegates to 
 * [VerseAdapter.getSpanSize] to support dynamic item widths.
 */
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

/**
 * Internal bridge for Staggered Grid logic.
 */
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
 * 
 * @param hS Final horizontal spacing between items.
 * @param vS Final vertical spacing between items.
 * @param hP Final horizontal outer padding.
 * @param vP Final vertical outer padding.
 * @param createLayoutManager Lambda to create a new [RecyclerView.LayoutManager] if needed.
 * @return The [VerseAdapter] instance assigned to this RecyclerView.
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
        this.setRecycledViewPool(null)
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
 * 
 * @param hS Horizontal gap in pixels.
 * @param vS Vertical gap in pixels.
 * @param hP Horizontal outer margin in pixels.
 * @param vP Vertical outer margin in pixels.
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
 * Orchestrates the execution of the DSL block and submits the resulting items.
 * 
 * @param adapter The target [VerseAdapter] to receive the items.
 * @param block The DSL block where users declare their UI structure.
 */
@PublishedApi
internal inline fun submit(adapter: VerseAdapter, block: VerseScope.() -> Unit) {
    val scope = VerseScope(adapter)
    scope.block()
    adapter.submitList(scope.newWrappers)
}