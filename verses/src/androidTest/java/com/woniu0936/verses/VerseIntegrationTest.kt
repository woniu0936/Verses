package com.woniu0936.verses

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.ext.compose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    class TestBinding(val root: View, val textView: TextView) : ViewBinding {
        override fun getRoot(): View = root
    }

    private fun testInflate(inflater: LayoutInflater, parent: ViewGroup, attach: Boolean): TestBinding {
        val view = TextView(parent.context)
        if (attach) parent.addView(view)
        return TestBinding(view, view)
    }

    @Test
    fun testComposePopulatesAdapter() {
        val items = listOf("One", "Two", "Three")
        val latch = CountDownLatch(1)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeLinear {
                items(items, ::testInflate, key = { it }) { binding, item ->
                    binding.textView.text = item
                }
            }
            
            // Wait for diff util (though usually synchronous on main thread for first submit if configured, 
            // ListAdapter submits are async on background thread by default but DiffUtil can be tricky.
            // VerseAdapter uses default AsyncDifferConfig which uses background thread for diffing.
            // So we strictly need to wait or use commitCallback if possible, but ListAdapter doesn't expose it easily in submitList.
            // However, since we are in test and list was empty, it might be fast.
            // Let's check adapter count directly.
        }

        // Allow some time for AsyncListDiffer
        Thread.sleep(200)

        val adapter = recyclerView.adapter
        assertNotNull(adapter)
        assertEquals(3, adapter?.itemCount)
        assertEquals(0, adapter?.getItemViewType(0)) // First type should be 0
    }
    
    @Test
    fun testMultipleViewTypes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.compose {
                item(::testInflate, data = "Header") { }
                items(listOf("A"), ::testInflate) { _, _ -> } // Same inflate, should be same type if VerseScope logic holds
            }
        }
        
        Thread.sleep(100)
        
        val adapter = recyclerView.adapter!!
        assertEquals(2, adapter.itemCount)
        // Since both use ::testInflate, they should share the viewType ID (which is cached by function ref)
        assertEquals(adapter.getItemViewType(0), adapter.getItemViewType(1))
    }
}
