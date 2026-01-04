package com.woniu0936.verses.core

import android.view.View
import com.woniu0936.verses.model.ItemWrapper
import com.woniu0936.verses.model.SmartViewHolder
import com.woniu0936.verses.model.currentProcessingHolder
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerseAdapterTest {

    @Test
    fun `onBindViewHolder manages global reference with re-entrancy support`() {
        val adapter = spyk(VerseAdapter())
        val holderA = SmartViewHolder(mockk(relaxed = true))
        val holderB = SmartViewHolder(mockk(relaxed = true))
        
        val bindB: SmartViewHolder.(Any) -> Unit = {
            assertEquals(holderB, currentProcessingHolder.get())
        }

        val bindA: SmartViewHolder.(Any) -> Unit = {
            assertEquals(holderA, currentProcessingHolder.get())
            adapter.onBindViewHolder(holderB, 1)
            assertEquals(holderA, currentProcessingHolder.get())
        }

        val itemA = ItemWrapper("A", 1, "DataA", 1, false, { mockk() }, bindA)
        val itemB = ItemWrapper("B", 1, "DataB", 1, false, { mockk() }, bindB)

        every { adapter.getItem(0) } returns itemA
        every { adapter.getItem(1) } returns itemB

        adapter.onBindViewHolder(holderA, 0)
        assertNull(currentProcessingHolder.get())
    }

    @Test
    fun `onBindViewHolder correctly updates isClickable based on item onClick`() {
        val adapter = spyk(VerseAdapter())
        val mockView = mockk<View>()
        val isClickableSlot = slot<Boolean>()
        every { mockView.isClickable } returns false
        every { mockView.isClickable = capture(isClickableSlot) } just Runs
        every { mockView.layoutParams } returns mockk()

        val holder = SmartViewHolder(mockView)
        
        val itemWithClick = ItemWrapper("1", 1, "D", 1, false, { mockk() }, { }, onClick = { })
        every { adapter.getItem(0) } returns itemWithClick
        
        adapter.onBindViewHolder(holder, 0)
        assertEquals(true, isClickableSlot.captured)

        val itemNoClick = ItemWrapper("2", 1, "D", 1, false, { mockk() }, { }, onClick = null)
        every { adapter.getItem(0) } returns itemNoClick
        every { mockView.isClickable } returns true
        
        adapter.onBindViewHolder(holder, 0)
        assertEquals(false, isClickableSlot.captured)
    }
}
