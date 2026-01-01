package com.woniu0936.verses

import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.dsl.VerseScope
import com.woniu0936.verses.model.Inflate
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [VerseScope] to verify the DSL building logic and reified safety.
 */
class VerseScopeTest {

    private val adapter: VerseAdapter = mockk(relaxed = true)
    private val scope = VerseScope(adapter)

    // Mock ViewBinding and Inflate function
    // Use a concrete interface for reified testing
    interface TestBinding : ViewBinding
    private val mockBinding: TestBinding = mockk(relaxed = true)
    private val mockInflate: Inflate<TestBinding> = { _, _, _ -> mockBinding }

    /**
     * Verifies that [VerseScope.item] correctly creates and adds a single [com.woniu0936.verses.model.ItemWrapper]
     * using the reified Binding class as the key.
     */
    @Test
    fun `item() adds single wrapper to list`() {
        val testData = "Test Data"
        val testKey = "Test Key"

        mockkObject(VerseAdapter.Companion)
        every { VerseAdapter.getGlobalViewType(any(), any()) } returns 1

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
        assertEquals(2, wrapper.span)
        assertEquals(true, wrapper.fullSpan)
        assertEquals(1, wrapper.viewType)
        
        verify { VerseAdapter.getGlobalViewType(TestBinding::class.java, any()) }
        unmockkObject(VerseAdapter.Companion)
    }

    /**
     * Verifies that the simple [VerseScope.items] API correctly maps a list of data objects.
     */
    @Test
    fun `items() adds multiple wrappers`() {
        val list = listOf("A", "B", "C")

        mockkObject(VerseAdapter.Companion)
        every { VerseAdapter.getGlobalViewType(any(), any()) } returns 2

        scope.items(
            items = list,
            inflate = mockInflate,
            key = { it }
        ) { _ -> }

        assertEquals(3, scope.newWrappers.size)
        assertEquals("A", scope.newWrappers[0].data)
        assertEquals("B", scope.newWrappers[1].data)
        assertEquals("C", scope.newWrappers[2].data)
        
        verify(exactly = 3) { VerseAdapter.getGlobalViewType(TestBinding::class.java, any()) }
        unmockkObject(VerseAdapter.Companion)
    }

    /**
     * Verifies the Advanced Mode DSL where [VerseScope.items] provides a block for conditional
     * rendering using the [VerseScope.render] function.
     */
    @Test
    fun `render() inside items() with block adds wrappers`() {
        val list = listOf(1, 2)

        mockkObject(VerseAdapter.Companion)
        every { VerseAdapter.getGlobalViewType(any(), any()) } returns 3

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
        
        verify(exactly = 2) { VerseAdapter.getGlobalViewType(TestBinding::class.java, any()) }
        unmockkObject(VerseAdapter.Companion)
    }

    /**
     * Verifies that the ViewType caching mechanism uses the Class as the key by default.
     */
    @Test
    fun `adapter getOrCreateViewType is called with correct keys`() {
        mockkObject(VerseAdapter.Companion)
        every { VerseAdapter.getGlobalViewType(any(), any()) } returns 1

        // Case 1: Simple item should use the Class as the cache key
        scope.item(mockInflate)
        verify { VerseAdapter.getGlobalViewType(TestBinding::class.java, any()) }

        // Case 2: Rendering with an explicit contentType should use that key instead
        val contentType = "MY_TYPE"
        scope.items(listOf("A")) {
            render(mockInflate, contentType = contentType) {}
        }
        verify { VerseAdapter.getGlobalViewType(eq(contentType), any()) }
        unmockkObject(VerseAdapter.Companion)
    }
}
