package com.woniu0936.verses.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.*
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.core.VerseSpacingDecoration
import com.woniu0936.verses.dsl.VerseScope

// 1. Linear Layout
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

fun RecyclerView.composeLinearColumn(
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    block: VerseScope.() -> Unit
) = compose(RecyclerView.VERTICAL, reverseLayout, spacing, contentPadding, horizontalPadding, verticalPadding, block)

fun RecyclerView.composeLinearRow(
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    block: VerseScope.() -> Unit
) = compose(RecyclerView.HORIZONTAL, reverseLayout, spacing, contentPadding, horizontalPadding, verticalPadding, block)

// 2. Grid Layout
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

// 3. Staggered Grid Layout
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
    this.setRecycledViewPool(VerseAdapter.globalPool)
    updateDecoration(hS, vS, hP, vP)
    (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    (context as? LifecycleOwner)?.lifecycle?.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            this.adapter = null
        }
    })
    return newAdapter
}

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

@PublishedApi
internal inline fun submit(adapter: VerseAdapter, block: VerseScope.() -> Unit) {
    val scope = VerseScope(adapter)
    scope.block()
    adapter.submitList(scope.newWrappers)
}