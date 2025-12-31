package com.woniu0936.verses.ext

import androidx.recyclerview.widget.*
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.dsl.VerseScope

// ==========================================
//  1. Linear Layout (Linear)
// ==========================================

/**
 * Configures the [androidx.recyclerview.widget.RecyclerView] with a [androidx.recyclerview.widget.LinearLayoutManager].
 *
 * @param orientation The layout orientation ([androidx.recyclerview.widget.RecyclerView.VERTICAL] or [androidx.recyclerview.widget.RecyclerView.HORIZONTAL]).
 * @param reverseLayout Whether to reverse the layout.
 * @param block The DSL block for defining list content.
 */
fun RecyclerView.composeLinear(
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
) {
    val adapter = getOrCreateAdapter {
        LinearLayoutManager(context, orientation, reverseLayout)
    }
    
    (layoutManager as? LinearLayoutManager)?.let {
        if (it.orientation != orientation) it.orientation = orientation
        if (it.reverseLayout != reverseLayout) it.reverseLayout = reverseLayout
    }
    
    submit(adapter, block)
}

/**
 * Convenience entry for a horizontal linear list (similar to Compose's LazyRow).
 */
fun RecyclerView.composeLinearRow(
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
) = composeLinear(RecyclerView.HORIZONTAL, reverseLayout, block)

/**
 * Convenience entry for a vertical linear list (similar to Compose's LazyColumn).
 */
fun RecyclerView.composeLinearColumn(
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
) = composeLinear(RecyclerView.VERTICAL, reverseLayout, block)


// ==========================================
//  2. Grid Layout (Grid)
// ==========================================

/**
 * Configures the [androidx.recyclerview.widget.RecyclerView] with a [androidx.recyclerview.widget.GridLayoutManager].
 *
 * @param spanCount The total number of columns in the grid.
 * @param orientation The layout orientation ([androidx.recyclerview.widget.RecyclerView.VERTICAL] or [androidx.recyclerview.widget.RecyclerView.HORIZONTAL]).
 * @param reverseLayout Whether to reverse the layout.
 * @param block The DSL block for defining list content.
 */
fun RecyclerView.composeGrid(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
) {
    val adapter = getOrCreateAdapter {
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
 * Convenience entry for a horizontal grid list.
 */
fun RecyclerView.composeGridRow(
    spanCount: Int,
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
) = composeGrid(spanCount, RecyclerView.HORIZONTAL, reverseLayout, block)

/**
 * Convenience entry for a vertical grid list.
 */
fun RecyclerView.composeGridColumn(
    spanCount: Int,
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
) = composeGrid(spanCount, RecyclerView.VERTICAL, reverseLayout, block)


// ==========================================
//  3. Staggered Grid Layout (Staggered)
// ==========================================

/**
 * Configures the [androidx.recyclerview.widget.RecyclerView] with a [androidx.recyclerview.widget.StaggeredGridLayoutManager].
 *
 * @param spanCount The number of spans.
 * @param orientation The layout orientation ([androidx.recyclerview.widget.RecyclerView.VERTICAL] or [androidx.recyclerview.widget.RecyclerView.HORIZONTAL]).
 * @param reverseLayout Whether to reverse the layout.
 * @param gapStrategy The gap handling strategy.
 * @param block The DSL block for defining list content.
 */
fun RecyclerView.composeStaggered(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE,
    block: VerseScope.() -> Unit
) {
    val adapter = getOrCreateAdapter {
        StaggeredGridLayoutManager(spanCount, orientation).apply {
            this.reverseLayout = reverseLayout
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
 * Convenience entry for a horizontal staggered grid list.
 */
fun RecyclerView.composeStaggeredRow(
    spanCount: Int,
    reverseLayout: Boolean = false,
    gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE,
    block: VerseScope.() -> Unit
) = composeStaggered(spanCount, RecyclerView.HORIZONTAL, reverseLayout, gapStrategy, block)

/**
 * Convenience entry for a vertical staggered grid list.
 */
fun RecyclerView.composeStaggeredColumn(
    spanCount: Int,
    reverseLayout: Boolean = false,
    gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE,
    block: VerseScope.() -> Unit
) = composeStaggered(spanCount, RecyclerView.VERTICAL, reverseLayout, gapStrategy, block)


// ==========================================
//  Private Helpers
// ==========================================

/**
 * Retrieves the existing [VerseAdapter] or creates a new one and attaches it to this [RecyclerView].
 *
 * @param createLayoutManager A lambda that returns a new [RecyclerView.LayoutManager] instance.
 * @return The [VerseAdapter] instance.
 */
private fun RecyclerView.getOrCreateAdapter(
    createLayoutManager: () -> RecyclerView.LayoutManager
): VerseAdapter {
    val currentAdapter = this.adapter as? VerseAdapter
    val currentLayoutManager = this.layoutManager
    
    if (currentAdapter != null && currentLayoutManager != null) {
        val tempLM = createLayoutManager()
        if (currentLayoutManager::class.java == tempLM::class.java) {
            return currentAdapter
        }
    }

    val newAdapter = VerseAdapter()
    this.layoutManager = createLayoutManager()
    this.adapter = newAdapter
    return newAdapter
}

/**
 * Executes the DSL block to build the new state and submits it to the adapter.
 *
 * @param adapter The [VerseAdapter] to submit the list to.
 * @param block The DSL block to execute.
 */
private fun submit(adapter: VerseAdapter, block: VerseScope.() -> Unit) {
    val scope = VerseScope(adapter)
    scope.block()
    adapter.submitList(scope.newWrappers)
}