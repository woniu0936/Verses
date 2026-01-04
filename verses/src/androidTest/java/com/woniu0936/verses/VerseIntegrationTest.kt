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
        // Give it a fixed size so it can layout its children
        recyclerView.layout(0, 0, 1080, 1920)
    }

    /**
     * Triggers a manual layout pass on the RecyclerView.
     */
    private fun triggerLayout() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
            )
            recyclerView.layout(0, 0, 1080, 1920)
        }
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
     * Verifies that [composeRow] correctly sets the [RecyclerView.HORIZONTAL] orientation.
     */
    @Test
    fun testComposeRow() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeRow {
                item(::testInflate, data = "Row Item")
            }
        }
        Thread.sleep(100)
        val lm = recyclerView.layoutManager as LinearLayoutManager
        assertEquals(RecyclerView.HORIZONTAL, lm.orientation)
    }

    /**
     * Verifies that [composeVerticalGrid] correctly configures the span count and vertical orientation.
     */
    @Test
    fun testComposeVerticalGrid() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeVerticalGrid(spanCount = 3) {
                items(listOf(1, 2, 3, 4), ::testInflate) { _ -> }
            }
        }
        Thread.sleep(100)
        val lm = recyclerView.layoutManager as GridLayoutManager
        assertEquals(3, lm.spanCount)
        assertEquals(RecyclerView.VERTICAL, lm.orientation)
    }

    /**
     * Verifies that [composeVerticalStaggeredGrid] correctly configures a [StaggeredGridLayoutManager].
     */
    @Test
    fun testComposeVerticalStaggeredGrid() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeVerticalStaggeredGrid(spanCount = 2) {
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
            recyclerView.composeColumn {
                item(::testInflate)
            }
            firstLM = recyclerView.layoutManager as LinearLayoutManager
            
            // Re-configuring with different parameters (orientation, reverse)
            recyclerView.composeRow(reverseLayout = true) {
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

    /**
     * Verifies that Custom Views (programmatic UI) are correctly created and bound.
     */
    @Test
    fun testCustomViewSupport() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.compose {
                items(
                    items = listOf("Custom 1", "Custom 2"),
                    create = { context -> TextView(context) }
                ) { text ->
                    // 'this' is TextView
                    this.text = text
                }
            }
        }
        
        Thread.sleep(100)
        triggerLayout()
        
        val adapter = recyclerView.adapter
        assertEquals(2, adapter?.itemCount)
        
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(0)
        assertNotNull(viewHolder)
        assertTrue(viewHolder?.itemView is TextView)
        assertEquals("Custom 1", (viewHolder?.itemView as TextView).text.toString())
    }

    /**
     * Verifies that [VerseSpacingDecoration] is correctly applied when spacing is provided.
     */
    @Test
    fun testSpacingDecorationIsApplied() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeColumn(spacing = 16) {
                item(::testInflate)
            }
        }

        val decorationCount = recyclerView.itemDecorationCount
        assertTrue("Decoration should be applied when spacing > 0", decorationCount > 0)
        
        var foundSpacingDecoration = false
        for (i in 0 until decorationCount) {
            if (recyclerView.getItemDecorationAt(i).javaClass.simpleName == "VerseSpacingDecoration") {
                foundSpacingDecoration = true
                break
            }
        }
        assertTrue("VerseSpacingDecoration should be present", foundSpacingDecoration)
    }
}
