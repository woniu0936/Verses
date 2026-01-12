package com.woniu0936.verses.core.pool

import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class VerseStateTest {

    private lateinit var mockRecyclerView: RecyclerView
    private lateinit var mockLayoutManager: RecyclerView.LayoutManager
    private lateinit var mockState: Parcelable

    @Before
    fun setup() {
        VerseStateRegistry.clear()
        mockRecyclerView = mockk(relaxed = true)
        mockLayoutManager = mockk(relaxed = true)
        mockState = mockk()

        every { mockRecyclerView.layoutManager } returns mockLayoutManager
    }

    @Test
    fun `saveState should persist LayoutManager state`() {
        // Given
        val id = "test_id"
        every { mockLayoutManager.onSaveInstanceState() } returns mockState

        // When
        VerseStateRegistry.saveState(id, mockRecyclerView)

        // Then
        // We verify indirectly by trying to restore it immediately
        VerseStateRegistry.restoreState(id, mockRecyclerView)
        verify { mockLayoutManager.onRestoreInstanceState(mockState) }
    }

    @Test
    fun `restoreState should do nothing if no state saved`() {
        // Given
        val id = "unknown_id"

        // When
        VerseStateRegistry.restoreState(id, mockRecyclerView)

        // Then
        verify(exactly = 0) { mockLayoutManager.onRestoreInstanceState(any()) }
    }
}
