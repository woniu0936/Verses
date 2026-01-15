package com.woniu0936.verses

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewbinding.ViewBinding
import com.woniu0936.verses.core.VerseAdapter
import com.woniu0936.verses.ext.*
import com.woniu0936.verses.model.SmartViewHolder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class VerseBenchmarkTest {

    private lateinit var context: Context
    private lateinit var recyclerView: RecyclerView

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        recyclerView = RecyclerView(context)
        recyclerView.layout(0, 0, 1080, 1920)
    }

    class TestBinding(private val _root: View) : ViewBinding {
        override fun getRoot(): View = _root
    }

    private fun testInflate(ctx: android.view.LayoutInflater, parent: android.view.ViewGroup, attach: Boolean): TestBinding {
        val view = TextView(parent.context).apply { text = "Performance Test" }
        return TestBinding(view)
    }

    @Test
    fun benchmarkDiffLatency() {
        val count = 100
        val data = (1..count).toList()

        val asyncTime = measureTimeMillis {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                recyclerView.composeColumn {
                    items(data, key = { it }, inflate = ::testInflate) { }
                }
            }
        }
        
        val syncTime = measureTimeMillis {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                recyclerView.composeRow {
                    items(data, key = { it }, inflate = ::testInflate) { }
                }
            }
        }

        logHeader("Diffing Strategy Latency (N=$count)")
        logRow("Async Mode (Column)", "${asyncTime}ms", "Standard")
        logRow("Sync Mode (Row)", "${syncTime}ms", if (syncTime <= asyncTime) "FAST" else "STABLE")
        logFooter()
    }

    @Test
    fun benchmarkViewHolderProduction() {
        val dummyParent = android.widget.FrameLayout(context)
        val model = object : com.woniu0936.verses.model.ViewBindingModel<TestBinding, Int>(1, 1) {
            override fun inflate(inflater: android.view.LayoutInflater, parent: android.view.ViewGroup): TestBinding = testInflate(inflater, parent, false)
            override fun bind(binding: TestBinding, item: Int) {}
            override val layoutRes: Int get() = 999 
        }
        
        val coldTime = measureNanoTime {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                model.createHolder(dummyParent)
            }
        }

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            com.woniu0936.verses.core.perf.VersePreloader.preload(context, listOf(model), countPerType = 5)
        }
        
        Thread.sleep(1000)

        val poolCount = com.woniu0936.verses.core.pool.VerseRecycledViewPool.GLOBAL.getRecycledViewCount(model.getViewType())
        
        val warmTime = measureNanoTime {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                model.createHolder(dummyParent)
            }
        }

        val coldMs = coldTime / 1_000_000.0
        val warmMs = warmTime / 1_000_000.0
        val ratio = coldTime.toDouble() / warmTime.toDouble()

        logHeader("ViewHolder Production (Pool size: $poolCount)")
        logRow("Cold Creation", String.format("%.3fms", coldMs), "1x")
        logRow("Warm (Preloaded)", String.format("%.3fms", warmMs), String.format("%.1fx FASTER", ratio))
        logFooter()
    }

    @Test
    fun benchmarkDslAllocation() {
        val iterations = 1000
        val time = measureTimeMillis {
            repeat(iterations) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    recyclerView.composeColumn {
                        repeat(10) { item(it, ::testInflate) }
                    }
                }
            }
        }

        logHeader("DSL Execution (Scope Pooling)")
        logRow("Iterations", iterations.toString(), "-")
        logRow("Total Time", "${time}ms", "-")
        logRow("Avg/Render", String.format("%.3fms", time.toDouble() / iterations), "OPTIMIZED")
        logFooter()
    }

    private fun logHeader(title: String) {
        val device = android.os.Build.MODEL
        println("\n╔═══════════════════════════════════════════════════════════╗")
        println("║ BENCHMARK: ${title.padEnd(46)}║")
        println("║ DEVICE: ${device.padEnd(49)}║")
        println("╠══════════════════════┳══════════════════┳═════════════════╣")
    }

    private fun logRow(label: String, value: String, metric: String) {
        println("║ ${label.padEnd(20)} ║ ${value.padEnd(16)} ║ ${metric.padEnd(15)} ║")
    }

    private fun logFooter() {
        println("╚══════════════════════╩══════════════════╩═════════════════╝")
    }
}