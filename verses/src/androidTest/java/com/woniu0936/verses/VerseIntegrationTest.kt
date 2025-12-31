package com.woniu0936.verses

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.ext.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests to verify that the DSL correctly configures real [RecyclerView] and [RecyclerView.LayoutManager] instances.
 */
@RunWith(AndroidJUnit4::class)
class VerseIntegrationTest {

    private lateinit var context: Context
    private lateinit var recyclerView: RecyclerView

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        recyclerView = RecyclerView(context)
        recyclerView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    /**
     * A simple [ViewBinding] implementation used for testing.
     */
    class TestBinding(private val _root: View, val textView: TextView) : ViewBinding {
        override fun getRoot(): View = _root
    }

    /**
     * Mock inflation logic to create [TestBinding] instances during tests.
     */
    private fun testInflate(inflater: LayoutInflater, parent: ViewGroup, attach: Boolean): TestBinding {
        val view = TextView(parent.context)
        if (attach) parent.addView(view)
        return TestBinding(view, view)
    }

    /**
     * Verifies that [compose] correctly attaches a [VerseAdapter] and sets up a [LinearLayoutManager].
     */
    @Test
    fun testComposePopulatesAdapter() {
        val items = listOf("One", "Two", "Three")

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.compose {
                items(items, ::testInflate, key = { it }) { item ->
                    textView.text = item
                }
            }
        }

        // ListAdapter/DiffUtil updates are asynchronous
        Thread.sleep(200)

        val adapter = recyclerView.adapter
        assertNotNull(adapter)
        assertEquals(3, adapter?.itemCount)
        assertTrue(recyclerView.layoutManager is LinearLayoutManager)
    }

    /**
     * Verifies that [composeLinearRow] correctly sets the [RecyclerView.HORIZONTAL] orientation.
     */
    @Test
    fun testComposeLinearRow() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeLinearRow {
                item(::testInflate, data = "Row Item")
            }
        }
        Thread.sleep(100)
        val lm = recyclerView.layoutManager as LinearLayoutManager
        assertEquals(RecyclerView.HORIZONTAL, lm.orientation)
    }

    /**
     * Verifies that [composeGrid] correctly configures the span count and vertical orientation.
     */
    @Test
    fun testComposeGrid() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeGrid(spanCount = 3) {
                items(listOf(1, 2, 3, 4), ::testInflate) { _ -> }
            }
        }
        Thread.sleep(100)
        val lm = recyclerView.layoutManager as GridLayoutManager
        assertEquals(3, lm.spanCount)
        assertEquals(RecyclerView.VERTICAL, lm.orientation)
    }

    /**
     * Verifies that [composeStaggered] correctly configures a [StaggeredGridLayoutManager].
     */
    @Test
    fun testComposeStaggered() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeStaggered(spanCount = 2) {
                item(::testInflate)
            }
        }
        Thread.sleep(100)
        val lm = recyclerView.layoutManager as StaggeredGridLayoutManager
        assertEquals(2, lm.spanCount)
        assertEquals(RecyclerView.VERTICAL, lm.orientation)
    }

    /**
     * Verifies that calling compose methods multiple times on the same [RecyclerView]
     * reuses the existing [RecyclerView.LayoutManager] if the type hasn't changed.
     */
    @Test
    fun testLayoutManagerUpdateWithoutRecreation() {
        var firstLM: LinearLayoutManager? = null
        
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeLinearColumn {
                item(::testInflate)
            }
            firstLM = recyclerView.layoutManager as LinearLayoutManager
            
            // Re-configuring with different parameters (orientation, reverse)
            recyclerView.composeLinearRow(reverseLayout = true) {
                item(::testInflate)
            }
        }
        
        Thread.sleep(100)
        val secondLM = recyclerView.layoutManager as LinearLayoutManager
        
        // Instance equality check
        assertTrue(firstLM === secondLM)
        assertEquals(RecyclerView.HORIZONTAL, secondLM.orientation)
        assertEquals(true, secondLM.reverseLayout)
    }

    /**
     * Verifies that different DSL items sharing the same ViewBinding type share the same ViewType ID.
     */
    @Test
    fun testMultipleItemsSameTypeShareViewType() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.compose {
                // Both items use TestBinding::class.java as the key
                item(::testInflate, data = "Header") { }
                items(listOf("A"), ::testInflate) { _ -> }
            }
        }
        
        Thread.sleep(100)
        
        val adapter = recyclerView.adapter as VerseAdapter
        assertEquals(2, adapter.itemCount)
        
        // Under the new "Reified Safety" rule, items sharing the same Binding class share the same ViewType
        assertEquals(adapter.getItemViewType(0), adapter.getItemViewType(1))
    }
}