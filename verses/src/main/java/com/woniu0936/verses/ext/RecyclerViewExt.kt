package com.woniu0936.verses.ext

import androidx.recyclerview.widget.*
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.dsl.VerseScope

/**
 * Configures the [androidx.recyclerview.widget.RecyclerView] with a [androidx.recyclerview.widget.LinearLayoutManager] and builds its content declaratively.
 *
 * @param orientation The layout orientation ([androidx.recyclerview.widget.RecyclerView.VERTICAL] or [androidx.recyclerview.widget.RecyclerView.HORIZONTAL]).
 * @param reverseLayout Whether to reverse the layout.
 * @param block The DSL block for defining list content.
 */
fun RecyclerView.compose(
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
) {
    val adapter = getOrCreateAdapter { 
        LinearLayoutManager(context, orientation, reverseLayout) 
    }
    submit(adapter, block)
}

/**
 * Configures the [androidx.recyclerview.widget.RecyclerView] with a [androidx.recyclerview.widget.GridLayoutManager] and builds its content declaratively.
 *
 * This function automatically sets up a [androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup] to support
 * items with dynamic span sizes (e.g., headers or multi-column items).
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
    submit(adapter, block)
}

/**
 * Configures the [androidx.recyclerview.widget.RecyclerView] with a [androidx.recyclerview.widget.StaggeredGridLayoutManager] and builds its content declaratively.
 *
 * Support for full-span items is automatically managed by the [VerseAdapter].
 *
 * @param spanCount The number of spans.
 * @param orientation The layout orientation ([androidx.recyclerview.widget.RecyclerView.VERTICAL] or [androidx.recyclerview.widget.RecyclerView.HORIZONTAL]).
 * @param gapStrategy The gap handling strategy.
 * @param block The DSL block for defining list content.
 */
fun RecyclerView.composeStaggered(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL,
    gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE,
    block: VerseScope.() -> Unit
) {
    val adapter = getOrCreateAdapter {
        StaggeredGridLayoutManager(spanCount, orientation).apply {
            this.gapStrategy = gapStrategy
        }
    }
    submit(adapter, block)
}

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
    if (currentAdapter != null) return currentAdapter

    val newAdapter = VerseAdapter()
    this.adapter = newAdapter
    this.layoutManager = createLayoutManager()
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
    // Submit list to ListAdapter to calculate Diff on a background thread.
    adapter.submitList(scope.newWrappers)
}
