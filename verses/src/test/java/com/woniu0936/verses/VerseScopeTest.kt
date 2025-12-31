package com.woniu0936.verses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.dsl.VerseScope
import com.woniu0936.verses.model.Inflate
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class VerseScopeTest {

    private val adapter: VerseAdapter = mockk(relaxed = true)
    private val scope = VerseScope(adapter)

    // Mock ViewBinding and Inflate function
    private val mockBinding: ViewBinding = mockk(relaxed = true)
    private val mockInflate: Inflate<ViewBinding> = { _, _, _ -> mockBinding }

    @Test
    fun `item() adds single wrapper to list`() {
        val testData = "Test Data"
        val testKey = "Test Key"
        
        // Setup adapter behavior
        every { adapter.getOrCreateViewType(any()) } returns 1

        scope.item(
            inflate = mockInflate,
            data = testData,
            key = testKey,
            span = 2,
            fullSpan = true
        )

        assertEquals(1, scope.newWrappers.size)
        val wrapper = scope.newWrappers.first()
        
        assertEquals(testData, wrapper.data)
        assertEquals(testKey, wrapper.id)
        assertEquals(2, wrapper.spanSize)
        assertEquals(true, wrapper.fullSpan)
        assertEquals(1, wrapper.viewType)
    }

    @Test
    fun `items() adds multiple wrappers`() {
        val list = listOf("A", "B", "C")
        
        every { adapter.getOrCreateViewType(any()) } returns 2

        scope.items(
            items = list,
            inflate = mockInflate,
            key = { it }
        ) { _, _ -> }

        assertEquals(3, scope.newWrappers.size)
        assertEquals("A", scope.newWrappers[0].data)
        assertEquals("B", scope.newWrappers[1].data)
        assertEquals("C", scope.newWrappers[2].data)
    }

    @Test
    fun `render() inside items() with block adds wrappers`() {
        val list = listOf(1, 2)
        
        every { adapter.getOrCreateViewType(any()) } returns 3

        scope.items(list, key = { it }) { item ->
            if (item == 1) {
                render(mockInflate) { }
            } else {
                render(mockInflate, fullSpan = true) { }
            }
        }

        assertEquals(2, scope.newWrappers.size)
        
        val first = scope.newWrappers[0]
        assertEquals(1, first.data)
        assertEquals(false, first.fullSpan)

        val second = scope.newWrappers[1]
        assertEquals(2, second.data)
        assertEquals(true, second.fullSpan)
    }

    @Test
    fun `adapter getOrCreateViewType is called with correct keys`() {
        // Case 1: Simple item with function ref (simulated by mockInflate)
        scope.item(mockInflate)
        verify { adapter.getOrCreateViewType(mockInflate) }

        // Case 2: Render with explicit contentType
        val contentType = "MY_TYPE"
        scope.items(listOf("A")) {
            render(mockInflate, contentType = contentType) {}
        }
        verify { adapter.getOrCreateViewType(contentType) }
    }
}
