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
import com.woniu0936.verses.core.pool.VerseRecycledViewPool
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
    nestedScrollingEnabled: Boolean? = null,
    block: VerseScope.() -> Unit
) {
    val hP = horizontalPadding ?: contentPadding
    val vP = verticalPadding ?: contentPadding
    
    // [Internal Heuristic] Rows are typically nested and require sync diff to prevent flickering.
    // Columns are typically root and require async diff for performance.
    val useSync = orientation == RecyclerView.HORIZONTAL
    
    val adapter = getOrCreateAdapter(spacing, spacing, hP, vP, useSync) {
        LinearLayoutManager(context, orientation, reverseLayout).apply {
            initialPrefetchItemCount = 4
        }
    }

    (layoutManager as? LinearLayoutManager)?.let {
        if (it.orientation != orientation) it.orientation = orientation
        if (it.reverseLayout != reverseLayout) it.reverseLayout = reverseLayout
    }
    
    nestedScrollingEnabled?.let { this.isNestedScrollingEnabled = it }

    submit(this, adapter, block)
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
    nestedScrollingEnabled: Boolean? = null,
    block: VerseScope.() -> Unit
) = compose(RecyclerView.VERTICAL, reverseLayout, spacing, contentPadding, horizontalPadding, verticalPadding, nestedScrollingEnabled, block)

/**
 * Convenience DSL for horizontal row lists (LazyRow).
 */
fun RecyclerView.composeRow(
    reverseLayout: Boolean = false,
    spacing: Int = 0,
    contentPadding: Int = 0,
    horizontalPadding: Int? = null,
    verticalPadding: Int? = null,
    nestedScrollingEnabled: Boolean? = null,
    block: VerseScope.() -> Unit
) = compose(RecyclerView.HORIZONTAL, reverseLayout, spacing, contentPadding, horizontalPadding, verticalPadding, nestedScrollingEnabled, block)

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
    nestedScrollingEnabled: Boolean? = null,
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
        nestedScrollingEnabled = nestedScrollingEnabled,
        useSynchronousDiff = false, // Grids are usually root
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
    nestedScrollingEnabled: Boolean? = null,
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
        nestedScrollingEnabled = nestedScrollingEnabled,
        useSynchronousDiff = true, // Horizontal grids often nested
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
    nestedScrollingEnabled: Boolean? = null,
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
        nestedScrollingEnabled = nestedScrollingEnabled,
        useSynchronousDiff = false,
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
    nestedScrollingEnabled: Boolean? = null,
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
        nestedScrollingEnabled = nestedScrollingEnabled,
        useSynchronousDiff = true,
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
    nestedScrollingEnabled: Boolean?,
    useSynchronousDiff: Boolean,
    block: VerseScope.() -> Unit
) {
    val hS = horizontalSpacing ?: spacing
    val vS = verticalSpacing ?: spacing
    val hP = horizontalPadding ?: contentPadding
    val vP = verticalPadding ?: contentPadding

    val adapter = getOrCreateAdapter(hS, vS, hP, vP, useSynchronousDiff) {
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
    
    nestedScrollingEnabled?.let { this.isNestedScrollingEnabled = it }

    submit(this, adapter, block)
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
    nestedScrollingEnabled: Boolean?,
    useSynchronousDiff: Boolean,
    block: VerseScope.() -> Unit
) {
    val hS = horizontalSpacing ?: spacing
    val vS = verticalSpacing ?: spacing
    val hP = horizontalPadding ?: contentPadding
    val vP = verticalPadding ?: contentPadding

    val adapter = getOrCreateAdapter(hS, vS, hP, vP, useSynchronousDiff) {
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
    
    nestedScrollingEnabled?.let { this.isNestedScrollingEnabled = it }

    submit(this, adapter, block)
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
    useSynchronousDiff: Boolean,
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

    val newAdapter = VerseAdapter(useSynchronousDiff)
    this.layoutManager = newLM
    this.adapter = newAdapter

    // Unlock registry for new session
    com.woniu0936.verses.core.pool.VerseStateRegistry.unlock()

    // Capture context for background preloading
    com.woniu0936.verses.core.VerseAdapterRegistry.latestContext = context

    // Performance optimizations for nested and complex lists
    this.setHasFixedSize(true)
    this.setRecycledViewPool(VerseRecycledViewPool.GLOBAL)
    
    // [Implicit Optimization] Increase primary cache size to handle scrolling micro-adjustments.
    this.setItemViewCacheSize(5)

    updateDecoration(hS, vS, hP, vP)

    // [Flicker-Free Architecture] Disable change animations to prevent white flashes 
    // during item updates, while still allowing Move/Add/Remove animations.
    if (this.itemAnimator == null) {
        this.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
    }
    (this.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false

    val cleanup = { isFinishing: Boolean ->
        com.woniu0936.verses.core.VersesLogger.d("Adapter: Cleanup called (isFinishing=$isFinishing)")
        this.adapter = null
        this.setRecycledViewPool(null)
        // [State Management] Clear saved states only when the activity is actually finishing.
        // This preserves state during rotation/backgrounding but clears it on true exit.
        if (isFinishing) {
            com.woniu0936.verses.core.pool.VerseStateRegistry.clear()
        }
    }

    val observer = LifecycleEventObserver { owner, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            val activity = (owner as? android.app.Activity) ?: (context as? android.app.Activity)
            val isFinishing = activity?.isFinishing ?: true
            cleanup(isFinishing)
        }
    }

    val lifecycle = context.lifecycleOwner()?.lifecycle
    lifecycle?.addObserver(observer)

    this.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            // [Safety] Clean up the view-local listener, but let LifecycleObserver 
            // handle the global destruction to avoid skipping cleanup for off-screen views.
            this@getOrCreateAdapter.removeOnAttachStateChangeListener(this)
        }
    })

    return newAdapter
}

/**
 * Manages the singleton [VerseSpacingDecoration] for the [RecyclerView].
 */
@PublishedApi
internal fun RecyclerView.updateDecoration(hS: Int, vS: Int, hP: Int, vP: Int) {
    var found = false
    for (i in 0 until itemDecorationCount) {
        val deco = getItemDecorationAt(i)
        if (deco is VerseSpacingDecoration) {
            // [Performance Guard] Only update if values have changed to avoid unnecessary requestLayout()
            if (deco.horizontalSpacing != hS || deco.verticalSpacing != vS || 
                deco.horizontalPadding != hP || deco.verticalPadding != vP) {
                removeItemDecorationAt(i)
                addItemDecoration(VerseSpacingDecoration(hS, vS, hP, vP))
            }
            found = true
            break
        }
    }
    
    if (!found && (hS > 0 || vS > 0 || hP > 0 || vP > 0)) {
        addItemDecoration(VerseSpacingDecoration(hS, vS, hP, vP))
    }
}

/**
 * A simple pool for VerseScope objects to minimize allocations in nested lists.
 */
@PublishedApi
internal val scopePool = java.util.ArrayDeque<VerseScope>(8)

/**
 * Orchestrates the DSL execution and item submission.
 */
@PublishedApi
internal inline fun submit(recyclerView: RecyclerView, adapter: VerseAdapter, block: VerseScope.() -> Unit) {
    // 1. Get a scope from pool or create new
    val scope = synchronized(scopePool) {
        if (scopePool.isNotEmpty()) scopePool.pop() else VerseScope(adapter)
    }
    
    try {
        scope.clear()
        // 2. Build the model tree
        scope.block()
        
        // 3. Early preloading (Safe to do before submission)
        if (scope.newModels.isNotEmpty()) {
            com.woniu0936.verses.core.perf.VersePreloader.preload(adapter.latestContext ?: recyclerView.context, scope.newModels)
        }
        
        // 4. Synchronous submission to guarantee correct measurement and layout
        val models = ArrayList(scope.newModels)
        if (recyclerView.isComputingLayout) {
            recyclerView.post {
                adapter.submitList(models)
            }
        } else {
            adapter.submitList(models)
        }
    } finally {
        // 5. Return to pool for reuse
        synchronized(scopePool) {
            if (scopePool.size < 8) scopePool.push(scope)
        }
    }
}
