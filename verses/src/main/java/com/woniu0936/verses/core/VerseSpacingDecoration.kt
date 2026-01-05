package com.woniu0936.verses.core

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A sophisticated [RecyclerView.ItemDecoration] that implements declarative spacing and padding.
 * 
 * Traditional ItemDecorations often cause issues in Grids where simple margins lead to 
 * unequal column widths or doubled spacing. VerseSpacingDecoration solves this using 
 * a proportional offset distribution algorithm.
 *
 * Mathematical Principles:
 * 1. **Grid Uniformity**: In a grid, total horizontal space ([hSpacing] * ([spanCount]-1)) 
 *    is shared among columns such that each item has the exact same usable width.
 * 2. **Edge Alignment**: [hPadding] and [vPadding] are applied only to the outermost 
 *    boundaries of the entire list matrix.
 * 3. **Gap Handling**: Internal gaps are divided symmetrically between adjacent items.
 *
 * @param hSpacing Horizontal space between items (column gap).
 * @param vSpacing Vertical space between items (row gap).
 * @param hPadding Outer horizontal padding between the list matrix and container edges.
 * @param vPadding Outer vertical padding between the list matrix and container edges.
 */
@PublishedApi
internal class VerseSpacingDecoration(
    private val hSpacing: Int,
    private val vSpacing: Int,
    private val hPadding: Int,
    private val vPadding: Int
) : RecyclerView.ItemDecoration() {

    /**
     * Calculates the inclusive offsets for a given item view.
     *
     * This method is called by RecyclerView for every item during every layout pass.
     * The algorithm used here ensures that grid columns remain perfectly symmetrical.
     */
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val lm = parent.layoutManager ?: return
        val itemCount = state.itemCount

        if (lm is GridLayoutManager) {
            val spanCount = lm.spanCount
            val lp = view.layoutParams as GridLayoutManager.LayoutParams
            val spanIndex = lp.spanIndex
            
            // 1. Horizontal Logic (Grid)
            // Offset = Padding + (Position * TotalGap / Count)
            // This formula ensures each column occupies exactly (Width - Gaps)/Count pixels.
            outRect.left = hPadding + spanIndex * hSpacing / spanCount
            outRect.right = hSpacing - (spanIndex + 1) * hSpacing / spanCount + hPadding
            
            // 2. Vertical Logic (Grid)
            val isFirstRow = position < spanCount
            val lastRowCount = itemCount % spanCount
            val actualLastRowCount = if (lastRowCount == 0) spanCount else lastRowCount
            val isLastRow = position >= itemCount - actualLastRowCount
            
            outRect.top = if (isFirstRow) vPadding else vSpacing / 2
            outRect.bottom = if (isLastRow) vPadding else vSpacing / 2
        } else if (lm is LinearLayoutManager) {
            val isVertical = lm.orientation == RecyclerView.VERTICAL
            if (isVertical) {
                outRect.left = hPadding
                outRect.right = hPadding
                outRect.top = if (position == 0) vPadding else vSpacing / 2
                outRect.bottom = if (position == itemCount - 1) vPadding else vSpacing / 2
            } else {
                outRect.top = vPadding
                outRect.bottom = vPadding
                outRect.left = if (position == 0) hPadding else hSpacing / 2
                outRect.right = if (position == itemCount - 1) hPadding else hSpacing / 2
            }
        }
    }
}