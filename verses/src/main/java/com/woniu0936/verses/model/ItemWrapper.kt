package com.woniu0936.verses.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

@PublishedApi
internal class SmartViewHolder(
    val view: View,
    val binding: ViewBinding? = null
) : RecyclerView.ViewHolder(view)

typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB
typealias ViewCreator<V> = (Context) -> V

@PublishedApi
internal data class ItemWrapper(
    val id: Any,
    val viewType: Int,
    val data: Any,
    val span: Int,
    val fullSpan: Boolean,
    val factory: (ViewGroup) -> SmartViewHolder,
    val bind: (SmartViewHolder) -> Unit,
    val onClick: (() -> Unit)? = null
)