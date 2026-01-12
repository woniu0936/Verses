package com.woniu0936.verses.dsl

import android.content.Context
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.core.Verses
import com.woniu0936.verses.core.VersesConfig
import com.woniu0936.verses.model.Inflate
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VerseDedupingTest {

    private val context: Context = mockk(relaxed = true)
    private val adapter: VerseAdapter = mockk(relaxed = true)
    private lateinit var scope: VerseScope

    interface TestBinding : ViewBinding
    private val mockInflate: Inflate<TestBinding> = { _, _, _ -> mockk(relaxed = true) }

    @Before
    fun setup() {
        io.mockk.every { context.filesDir } returns java.io.File(".")
        io.mockk.every { context.getExternalFilesDir(any()) } returns java.io.File(".")
        scope = VerseScope(adapter)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `debug mode should throw exception on duplicate keys`() {
        // Given: Debug mode enabled
        Verses.initialize(context, VersesConfig.Builder().debug(true).build())

        // When: Adding items with duplicate keys
        scope.items(listOf("A", "B"), key = { "fixed_key" }, inflate = mockInflate) { }
    }

    @Test
    fun `release mode should skip duplicate items and not crash`() {
        // Given: Release mode (debug disabled)
        Verses.initialize(context, VersesConfig.Builder().debug(false).build())

        // When: Adding items with duplicate keys
        val list = listOf("A", "B", "C")
        // "A" and "B" will have the same key, "C" will have a unique key.
        scope.items(list, key = { if (it == "C") "unique" else "duplicate" }, inflate = mockInflate) { }

        // Then: Only 2 models should be added (the first 'duplicate' and 'unique')
        assertEquals(2, scope.newModels.size)
        assertEquals("A", scope.newModels[0].data)
        assertEquals("C", scope.newModels[1].data)
    }
}
