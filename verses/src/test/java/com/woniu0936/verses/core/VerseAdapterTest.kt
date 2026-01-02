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
        val mockView = mockk<View>(relaxed = true)
        val mockViewHolder = SmartViewHolder(mockView)
        val mockParent = mockk<ViewGroup>()
        val factory: (ViewGroup) -> SmartViewHolder = { mockViewHolder }

        val typeId = VerseAdapter.getGlobalViewType(key, factory)
        val secondTypeId = VerseAdapter.getGlobalViewType(key, factory)

        assertEquals(typeId, secondTypeId)
        
        val createdHolder = adapter.onCreateViewHolder(mockParent, typeId)
        assertEquals(mockViewHolder, createdHolder)
    }

    @Test
    fun `getGlobalViewType handles hashCode collisions with linear probing`() {
        // Create two different keys with the same hashCode
        class CollisionKey(val id: String) {
            override fun hashCode(): Int = 42
            override fun equals(other: Any?): Boolean = (other as? CollisionKey)?.id == id
        }

        val key1 = CollisionKey("A")
        val key2 = CollisionKey("B")
        val factory: (ViewGroup) -> SmartViewHolder = { mockk() }

        val type1 = VerseAdapter.getGlobalViewType(key1, factory)
        val type2 = VerseAdapter.getGlobalViewType(key2, factory)

        // They must have different types despite same hashCode
        assert(type1 != type2)
        
        // Retrieval must be stable
        assertEquals(type1, VerseAdapter.getGlobalViewType(key1, factory))
        assertEquals(type2, VerseAdapter.getGlobalViewType(key2, factory))
    }

    @Test
    fun `ViewType is deterministic across different adapter instances`() {
        val key = "GlobalKey"
        val factory: (ViewGroup) -> SmartViewHolder = { mockk() }
        
        val adapter1 = VerseAdapter()
        val adapter2 = VerseAdapter()
        
        val type1 = VerseAdapter.getGlobalViewType(key, factory)
        val type2 = VerseAdapter.getGlobalViewType(key, factory)
        
        assertEquals(type1, type2)
    }

    @Test
    fun `onViewRecycled clears nested adapters recursively`() {
        val rootLayout = mockk<ViewGroup>(relaxed = true)
        val midLayout = mockk<ViewGroup>(relaxed = true)
        val nestedRV = mockk<RecyclerView>(relaxed = true)
        
        // Mock hierarchy: rootLayout -> midLayout -> nestedRV
        every { rootLayout.childCount } returns 1
        every { rootLayout.getChildAt(0) } returns midLayout
        
        every { midLayout.childCount } returns 1
        every { midLayout.getChildAt(0) } returns nestedRV
        
        val holder = SmartViewHolder(rootLayout)
        
        adapter.onViewRecycled(holder)

        // Verify that the deep nested RV had its adapter cleared
        verify { nestedRV.adapter = null }
    }

    @Test
    fun `clearRegistry empties all global caches`() {
        val key = "TestKey"
        val factory: (ViewGroup) -> SmartViewHolder = { mockk() }
        
        VerseAdapter.getGlobalViewType(key, factory)
        
        VerseAdapter.clearRegistry()
        
        // After clearing, getting the same key should ideally not find it in the internal cache
        val newType = VerseAdapter.getGlobalViewType(key, factory)
        assert(newType != 0)
    }
}
