package com.woniu0936.verses.ext

import androidx.recyclerview.widget.*
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.dsl.VerseScope

/**
 * 1. Linear Layout (LinearLayoutManager)
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
 * 2. Grid Layout (GridLayoutManager)
 */
fun RecyclerView.composeGrid(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
) {
    val adapter = getOrCreateAdapter {
        GridLayoutManager(context, spanCount, orientation, reverseLayout).apply {
            // Auto-bind SpanLookup
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                // Note: Need to get Adapter again here because the adapter in closure might be old reference (though unlikely with getOrCreate)
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
 * 3. Staggered Grid Layout (StaggeredGridLayoutManager)
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

// --- Private Helpers ---

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

private fun submit(adapter: VerseAdapter, block: VerseScope.() -> Unit) {
    val scope = VerseScope(adapter)
    scope.block()
    // Submit list to ListAdapter to calculate Diff
    adapter.submitList(scope.newWrappers)
}
