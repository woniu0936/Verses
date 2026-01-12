package com.woniu0936.verses.model

import android.view.ViewGroup
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class VerseModelCacheTest {

    private val resolveCount = AtomicInteger(0)

    private inner class TestModel(id: Any, data: String) : VerseModel<String>(id, data) {
        override val layoutRes: Int = 100
        override fun resolveViewType(): Int {
            resolveCount.incrementAndGet()
            return 1000
        }
        override fun createHolder(parent: ViewGroup): SmartViewHolder = mockk()
        override fun bind(holder: SmartViewHolder) {}
    }

    @Test
    fun `getViewType should cache the result and only call resolveViewType once`() {
        val model = TestModel("id", "data")
        
        assertEquals(0, resolveCount.get())
        
        val type1 = model.getViewType()
        assertEquals(1000, type1)
        assertEquals(1, resolveCount.get())
        
        val type2 = model.getViewType()
        assertEquals(1000, type2)
        assertEquals(1, resolveCount.get()) // Should NOT increment
        
        val type3 = model.getViewType()
        assertEquals(1000, type3)
        assertEquals(1, resolveCount.get()) // Still 1
    }
}
