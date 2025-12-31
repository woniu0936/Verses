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

    // Manual ViewBinding implementation for testing
    class TestBinding(private val _root: View, val textView: TextView) : ViewBinding {
        override fun getRoot(): View = _root
    }

    private fun testInflate(inflater: LayoutInflater, parent: ViewGroup, attach: Boolean): TestBinding {
        val view = TextView(parent.context)
        if (attach) parent.addView(view)
        return TestBinding(view, view)
    }

    @Test
    fun testComposeLinearPopulatesAdapter() {
        val items = listOf("One", "Two", "Three")

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeLinear {
                items(items, ::testInflate, key = { it }) { binding, item ->
                    binding.textView.text = item
                }
            }
        }

        // Allow some time for AsyncListDiffer
        Thread.sleep(200)

        val adapter = recyclerView.adapter
        assertNotNull(adapter)
        assertEquals(3, adapter?.itemCount)
        assertTrue(recyclerView.layoutManager is LinearLayoutManager)
    }

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

    @Test
    fun testComposeGridColumn() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeGridColumn(spanCount = 3) {
                items(listOf(1, 2, 3, 4), ::testInflate) { _, _ -> }
            }
        }
        Thread.sleep(100)
        val lm = recyclerView.layoutManager as GridLayoutManager
        assertEquals(3, lm.spanCount)
        assertEquals(RecyclerView.VERTICAL, lm.orientation)
    }

    @Test
    fun testComposeStaggeredRow() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeStaggeredRow(spanCount = 2) {
                item(::testInflate)
            }
        }
        Thread.sleep(100)
        val lm = recyclerView.layoutManager as StaggeredGridLayoutManager
        assertEquals(2, lm.spanCount)
        assertEquals(RecyclerView.HORIZONTAL, lm.orientation)
    }

    @Test
    fun testLayoutManagerUpdateWithoutRecreation() {
        var firstLM: LinearLayoutManager? = null
        
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeLinearColumn {
                item(::testInflate)
            }
            firstLM = recyclerView.layoutManager as LinearLayoutManager
            
            // Call again with different params
            recyclerView.composeLinearRow(reverseLayout = true) {
                item(::testInflate)
            }
        }
        
        Thread.sleep(100)
        val secondLM = recyclerView.layoutManager as LinearLayoutManager
        
        // Should be the same instance
        assertTrue(firstLM === secondLM)
        assertEquals(RecyclerView.HORIZONTAL, secondLM.orientation)
        assertEquals(true, secondLM.reverseLayout)
    }

    @Test
    fun testMultipleViewTypes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeLinear {
                item(::testInflate, data = "Header") { }
                items(listOf("A"), ::testInflate) { _, _ -> }
            }
        }
        
        Thread.sleep(100)
        
        val adapter = recyclerView.adapter as VerseAdapter
        assertEquals(2, adapter.itemCount)
        // Since both use ::testInflate, they should share the viewType ID (which is cached by function ref)
        assertEquals(adapter.getItemViewType(0), adapter.getItemViewType(1))
    }
}