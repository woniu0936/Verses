package com.woniu0936.verses.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

// Generic ViewHolder
class SmartViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

// ViewBinding factory function type definition
typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB

/**
 * Flattened rendering unit
 * @param id Used by DiffUtil to identify if the item is the same object
 * @param viewType Used as Key for RecyclerView reuse pool
 * @param data Original data, used by DiffUtil to compare content changes
 * @param spanSize Columns occupied in Grid layout
 * @param fullSpan Whether to force full width (for Grid and Staggered)
 * @param factory ViewHolder creation factory
 * @param bind Data binding logic
 */
data class ItemWrapper(
    val id: Any,
    val viewType: Int,
    val data: Any,
    val spanSize: Int,
    val fullSpan: Boolean,
    val factory: (ViewGroup) -> SmartViewHolder,
    val bind: (SmartViewHolder) -> Unit
)
