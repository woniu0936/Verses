package com.woniu0936.verses.model

import android.view.View
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SmartViewHolderTest {

    private val mockView: View = mockk(relaxed = true)
    private lateinit var holder: SmartViewHolder

    @Before
    fun setup() {
        holder = SmartViewHolder(mockView)
        currentProcessingHolder.set(holder)
    }

    @After
    fun tearDown() {
        currentProcessingHolder.remove()
    }

    @Test
    fun `bind triple value correctly tracks three slots`() {
        val counter = AtomicInteger(0)
        holder.prepare("ID1", "Data1")

        // First call
        mockView.bind("A", 1, true) { _, _, _ -> counter.incrementAndGet() }
        assertEquals(1, counter.get())

        // Same values
        holder.pointer = 0
        mockView.bind("A", 1, true) { _, _, _ -> counter.incrementAndGet() }
        assertEquals(1, counter.get())

        // One value changed
        holder.pointer = 0
        mockView.bind("A", 1, false) { _, _, _ -> counter.incrementAndGet() }
        assertEquals(2, counter.get())
    }

    @Test
    fun `bind single value only triggers when changed`() {
        val counter = AtomicInteger(0)
        holder.prepare("ID1", "Data1")

        // First call: Should trigger
        mockView.bind("A") { counter.incrementAndGet() }
        assertEquals(1, counter.get())

        // Second call with same value: Should NOT trigger
        holder.pointer = 0 // Reset pointer for second bind cycle
        mockView.bind("A") { counter.incrementAndGet() }
        assertEquals(1, counter.get())

        // Third call with new value: Should trigger
        holder.pointer = 0
        mockView.bind("B") { counter.incrementAndGet() }
        assertEquals(2, counter.get())
    }

    @Test
    fun `bind multi value correctly tracks multiple slots`() {
        val counter = AtomicInteger(0)
        holder.prepare("ID1", "Data1")

        // First call: Should trigger
        mockView.bind("A", 1) { _, _ -> counter.incrementAndGet() }
        assertEquals(1, counter.get())

        // Same values: Should skip
        holder.pointer = 0
        mockView.bind("A", 1) { _, _ -> counter.incrementAndGet() }
        assertEquals(1, counter.get())

        // One value changed: Should trigger
        holder.pointer = 0
        mockView.bind("A", 2) { _, _ -> counter.incrementAndGet() }
        assertEquals(2, counter.get())
    }

    @Test
    fun `once hook only runs once per ID`() {
        val counter = AtomicInteger(0)
        
        // Setup ID1
        holder.prepare("ID1", "Data1")
        once { counter.incrementAndGet() }
        once { counter.incrementAndGet() }
        assertEquals(2, counter.get())

        // Re-bind same ID: once should NOT run again
        holder.prepare("ID1", "Data1")
        once { counter.incrementAndGet() }
        assertEquals(2, counter.get())

        // Change to ID2: once SHOULD run again
        holder.prepare("ID2", "Data2")
        once { counter.incrementAndGet() }
        assertEquals(3, counter.get())
    }

    @Test
    fun `itemData returns the latest data reference`() {
        val data1 = "Data1"
        val data2 = "Data2"
        
        holder.prepare("ID1", data1)
        assertEquals(data1, holder.itemData<String>())

        holder.prepare("ID1", data2)
        assertEquals(data2, holder.itemData<String>())
    }
}
