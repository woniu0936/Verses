package com.woniu0936.verses.ext

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.core.VerseSpacingDecoration
import com.woniu0936.verses.dsl.VerseScope

/**
 * Recursively unwraps the Context to find the nearest LifecycleOwner.
 */
private fun Context.lifecycleOwner(): LifecycleOwner? {
    var curContext = this
    while (curContext is android.content.ContextWrapper) {
        if (curContext is LifecycleOwner) return curContext
        curContext = curContext.baseContext
    }
    return curContext as? LifecycleOwner
}

/**
 * Entry point for building a linear [RecyclerView] layout using the Verses DSL.
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

    this.setRecycledViewPool(VerseAdapter.getPool(context))
    updateDecoration(hS, vS, hP, vP)

    (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

    val cleanup = {
        this.adapter = null
        this.setRecycledViewPool(null)
    }

    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            cleanup()
        }
    }

    val lifecycle = context.lifecycleOwner()?.lifecycle
    lifecycle?.addObserver(observer)

    this.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
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
