package com.woniu0936.verses

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.ext.*
import com.woniu0936.verses.model.SmartViewHolder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VersePerformanceIntegrationTest {

    private lateinit var context: Context
    private lateinit var recyclerView: RecyclerView

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        recyclerView = RecyclerView(context)
        recyclerView.layout(0, 0, 1080, 1920)
    }

    private fun triggerLayout() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
            )
            recyclerView.layout(0, 0, 1080, 1920)
        }
    }

    class TestBinding(private val _root: View, val textView: TextView) : ViewBinding {
        override fun getRoot(): View = _root
    }

    private fun testInflate(inflater: LayoutInflater, parent: ViewGroup, attach: Boolean): TestBinding {
        val view = TextView(parent.context)
        if (attach) parent.addView(view)
        return TestBinding(view, view)
    }

    /**
     * ARCHITECTURAL TEST: Nested Adapter Persistence.
     * Verifies that the nested adapter is NOT destroyed during recycling.
     * This is the 'Silver Bullet' for smooth nested scrolling.
     */
    @Test
    fun testNestedAdapterPersistence() {
        var nestedRecyclerView: RecyclerView? = null
        var firstAdapterInstance: RecyclerView.Adapter<*>? = null

        // 1. Initial Render
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeColumn {
                item("nested_list", { ctx -> 
                    RecyclerView(ctx).also { nestedRecyclerView = it }
                }) {
                    // This is the bind block for the container
                    this.composeRow {
                        items(listOf(1, 2, 3), key = { it }, inflate = ::testInflate) { }
                    }
                }
            }
        }
        
        Thread.sleep(300)
        triggerLayout()

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertNotNull("Nested RV should be created", nestedRecyclerView)
            firstAdapterInstance = nestedRecyclerView?.adapter
            assertNotNull("Nested Adapter should be present", firstAdapterInstance)
            
            // 2. Simulate Recycling
            val holder = recyclerView.findViewHolderForAdapterPosition(0) as SmartViewHolder
            val adapter = recyclerView.adapter as VerseAdapter
            adapter.onViewRecycled(holder)
            
            // ARCHITECTURAL CHECK: In the new balanced architecture, 
            // the adapter should STAY attached during recycling to avoid re-inflation cost.
            assertNotNull("Adapter should PERSIST during recycling", nestedRecyclerView?.adapter)
            
            // 3. Re-bind
            adapter.onBindViewHolder(holder, 0)
            
            // FINAL VERIFICATION
            assertTrue("Adapter instance must be identical after re-bind to avoid 30ms latency",
                nestedRecyclerView?.adapter === firstAdapterInstance)
        }
    }

    /**
     * PERFORMANCE TEST: Incremental Decoration Update.
     * Verifies that the library doesn't trigger requestLayout() when spacing is unchanged.
     */
    @Test
    fun testIncrementalDecorationStability() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.composeColumn(spacing = 20) {
                item(1, ::testInflate)
            }
        }
        Thread.sleep(200)
        triggerLayout()

        val firstDeco = recyclerView.getItemDecorationAt(0)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // Re-render with same spacing
            recyclerView.composeColumn(spacing = 20) {
                item(1, ::testInflate)
            }
        }
        
        Thread.sleep(200)
        triggerLayout()
        
        val secondDeco = recyclerView.getItemDecorationAt(0)
        
        // If they are identical instances, it means we successfully skipped the destructive update
        assertTrue("Decoration should be reused if spacing is identical", firstDeco === secondDeco)
    }
}
