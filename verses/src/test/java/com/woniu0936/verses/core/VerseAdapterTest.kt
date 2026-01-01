package com.woniu0936.verses.core

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woniu0936.verses.model.SmartViewHolder
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Test

class VerseAdapterTest {

    private val adapter = VerseAdapter()

    @Test
    fun `getOrCreateViewType registers factory and returns stable ID`() {
        val key = "TestKey"
        val mockViewHolder = mockk<SmartViewHolder>()
        val mockParent = mockk<ViewGroup>()
        val factory: (ViewGroup) -> SmartViewHolder = { mockViewHolder }

        val typeId = VerseAdapter.getGlobalViewType(key, factory)
        val secondTypeId = VerseAdapter.getGlobalViewType(key, factory)

        assertEquals(typeId, secondTypeId)
        
        val createdHolder = adapter.onCreateViewHolder(mockParent, typeId)
        assertEquals(mockViewHolder, createdHolder)
    }

    @Test
    fun `onViewRecycled clears nested VerseAdapter`() {
        val nestedRecyclerView = mockk<RecyclerView>()
        val nestedAdapter = mockk<VerseAdapter>()
        
        every { nestedRecyclerView.adapter } returns nestedAdapter
        every { nestedAdapter.submitList(null) } just Runs

        val holder = SmartViewHolder(nestedRecyclerView)
        
        adapter.onViewRecycled(holder)

        verify { nestedAdapter.submitList(null) }
    }
}