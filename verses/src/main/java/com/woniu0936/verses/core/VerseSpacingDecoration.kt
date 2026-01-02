package com.woniu0936.verses.core

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

@PublishedApi
internal class VerseSpacingDecoration(
    private val hSpacing: Int,
    private val vSpacing: Int,
    private val hPadding: Int,
    private val vPadding: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val lm = parent.layoutManager
        val itemCount = state.itemCount

        if (lm is GridLayoutManager) {
            val spanCount = lm.spanCount
            val lp = view.layoutParams as GridLayoutManager.LayoutParams
            val spanIndex = lp.spanIndex
            
            // 1. Horizontal Logic (Grid)
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