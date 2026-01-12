package com.woniu0936.verses

import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.dsl.VerseScope
import com.woniu0936.verses.model.Inflate
import com.woniu0936.verses.model.DslVerseModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [VerseScope] to verify the DSL building logic and model creation.
 */
class VerseScopeTest {

    private val adapter: VerseAdapter = mockk(relaxed = true)
    private val scope = VerseScope(adapter)

    interface TestBinding : ViewBinding
    private val mockBinding: TestBinding = mockk(relaxed = true)
    private val mockInflate: Inflate<TestBinding> = { _, _, _ -> mockBinding }

    /**
     * Verifies that [VerseScope.item] correctly creates and adds a single [DslVerseModel]
     * to the internal list.
     */
    @Test
    fun `item() adds single model to list`() {
        val testData = "Test Data"
        val testKey = "Test Key"

        scope.item(
            inflate = mockInflate,
            layoutRes = 123,
            data = testData,
            key = testKey,
            span = 2,
            fullSpan = true
        )

        assertEquals(1, scope.newModels.size)
        val model = scope.newModels.first() as DslVerseModel

        assertEquals(testData, model.data)
        assertEquals(testKey, model.id)
        assertEquals(123, model.layoutRes)
        // For DslVerseModel, if fullSpan is true, getSpanSize returns totalSpan
        assertEquals(100, model.getSpanSize(100, 0))
    }

    /**
     * Verifies that the simple [VerseScope.items] API correctly maps a list of data objects.
     */
    @Test
    fun `items() adds multiple models`() {
        val list = listOf("A", "B", "C")

        scope.items(
            items = list,
            key = { it },
            inflate = mockInflate
        ) { _ -> }

        assertEquals(3, scope.newModels.size)
        assertEquals("A", scope.newModels[0].data)
        assertEquals("B", scope.newModels[1].data)
        assertEquals("C", scope.newModels[2].data)
    }

    /**
     * Verifies the Advanced Mode DSL where [VerseScope.items] provides a block for conditional
     * rendering using the [VerseScope.render] function.
     */
    @Test
    fun `render() inside items() with block adds models`() {
        val list = listOf(1, 2)

        scope.items(list, key = { it }) { item ->
            if (item == 1) {
                render(mockInflate) { }
            } else {
                render(mockInflate, fullSpan = true) { }
            }
        }

        assertEquals(2, scope.newModels.size)

        val first = scope.newModels[0] as DslVerseModel
        assertEquals(1, first.data)
        // Default span is 1, not full span
        assertEquals(1, first.getSpanSize(100, 0))

        val second = scope.newModels[1] as DslVerseModel
        assertEquals(2, second.data)
        assertEquals(100, second.getSpanSize(100, 0))
    }
}