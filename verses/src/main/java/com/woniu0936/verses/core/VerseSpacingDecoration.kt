package com.woniu0936.verses.core

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A sophisticated [RecyclerView.ItemDecoration] that implements declarative spacing and padding.
 */
@PublishedApi
internal class VerseSpacingDecoration(
    internal val horizontalSpacing: Int,
    internal val verticalSpacing: Int,
    internal val horizontalPadding: Int,
    internal val verticalPadding: Int
) : RecyclerView.ItemDecoration() {

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
            outRect.left = horizontalPadding + spanIndex * horizontalSpacing / spanCount
            outRect.right = horizontalSpacing - (spanIndex + 1) * horizontalSpacing / spanCount + horizontalPadding
            
            // 2. Vertical Logic (Grid)
            // Use spanGroupIndex to accurately detect rows even with variable span sizes
            val spanGroupIndex = lm.spanSizeLookup.getSpanGroupIndex(position, spanCount)
            val isFirstRow = spanGroupIndex == 0
            
            // Note: detecting the last row accurately in variable-span grids is computationally 
            // expensive. We maintain symmetrical internal gaps for now.
            outRect.top = if (isFirstRow) verticalPadding else verticalSpacing / 2
            outRect.bottom = verticalSpacing / 2
        } else if (lm is LinearLayoutManager) {
            val isVertical = lm.orientation == RecyclerView.VERTICAL
            if (isVertical) {
                outRect.left = horizontalPadding
                outRect.right = horizontalPadding
                outRect.top = if (position == 0) verticalPadding else verticalSpacing / 2
                outRect.bottom = if (position == itemCount - 1) verticalPadding else verticalSpacing / 2
            } else {
                outRect.top = verticalPadding
                outRect.bottom = verticalPadding
                outRect.left = if (position == 0) horizontalPadding else horizontalSpacing / 2
                outRect.right = if (position == itemCount - 1) horizontalPadding else horizontalSpacing / 2
            }
        }
    }
}
