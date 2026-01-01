package com.woniu0936.verses.ext

import androidx.recyclerview.widget.*
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.dsl.VerseScope

// 1. Linear Layout
fun RecyclerView.compose(
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

// Convenience aliases (optional but helpful)
fun RecyclerView.composeLinearColumn(
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
) = compose(RecyclerView.VERTICAL, reverseLayout, block)

fun RecyclerView.composeLinearRow(
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
) = compose(RecyclerView.HORIZONTAL, reverseLayout, block)

// 2. Grid Layout
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

// 3. Staggered Grid Layout
fun RecyclerView.composeStaggered(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE,
    block: VerseScope.() -> Unit
) {
    val adapter = getOrCreateAdapter {
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

@PublishedApi
internal fun RecyclerView.getOrCreateAdapter(
    createLayoutManager: () -> RecyclerView.LayoutManager
): VerseAdapter {
    val currentAdapter = this.adapter as? VerseAdapter
    val newLM = createLayoutManager()
    
    if (currentAdapter != null && layoutManager != null) {
        if (layoutManager!!.javaClass == newLM.javaClass) {
            return currentAdapter
        }
    }

    val newAdapter = VerseAdapter()
    this.layoutManager = newLM
    this.adapter = newAdapter
    
    // Transparent optimization:
    // We disable ONLY change animations to prevent flickering during data updates.
    // This keeps 'Add' and 'Remove' animations, so the list still feels fluid.
    (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    
    return newAdapter
}

@PublishedApi
internal inline fun submit(adapter: VerseAdapter, block: VerseScope.() -> Unit) {
    val scope = VerseScope(adapter)
    scope.block()
    adapter.submitList(scope.newWrappers)
}
