package com.woniu0936.verses.model

import android.view.View
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SmartViewHolderTest {

    private val mockView: View = mockk(relaxed = true)
    private lateinit var holder: SmartViewHolder

    @Before
    fun setup() {
        holder = SmartViewHolder(mockView)
    }

    @Test
    fun `itemData returns the latest data reference`() {
        val data1 = "Data1"
        val data2 = "Data2"
        
        holder.prepare(data1)
        assertEquals(data1, holder.itemData<String>())

        holder.prepare(data2)
        assertEquals(data2, holder.itemData<String>())
    }
}