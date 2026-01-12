package com.woniu0936.verses.core

import com.woniu0936.verses.model.VerseModel
import com.woniu0936.verses.model.SmartViewHolder
import android.view.ViewGroup
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureNanoTime

/**
 * A micro-benchmark to quantify the performance lift of the Kernel Upgrade.
 */
class VersesBenchmarkTest {

    // --- Simulation of OLD Logic (Before Upgrade) ---
    private val oldRegistryMap = ConcurrentHashMap<Class<*>, Int>()
    
    private fun oldGetViewTypeLogic(model: VerseModel<*>): Int {
        // Old logic: Check map, maybe put, return int
        // Simulating the overhead of VerseTypeRegistry.getViewType(clazz)
        return oldRegistryMap.getOrPut(model.javaClass) { model.hashCode() }
    }

    // --- Simulation of NEW Logic (After Upgrade) ---
    // The model itself now has caching. We use a real model implementation.
    private class BenchmarkModel(id: Int) : VerseModel<Int>(id, id) {
        override val layoutRes: Int = 0
        override fun resolveViewType(): Int = 1000 // Fixed type
        override fun createHolder(parent: ViewGroup): SmartViewHolder = throw NotImplementedError()
        override fun bind(holder: SmartViewHolder) {}
    }

    @Test
    fun `Benchmark Hot Path Performance`() {
        // Setup
        val iterations = 1_000_000 // Simulate scrolling through a VERY long list multiple times
        val model = BenchmarkModel(1)
        
        // Warmup JVM (to avoid JIT bias)
        repeat(10_000) { oldGetViewTypeLogic(model) }
        repeat(10_000) { model.getViewType() }

        // --- Run OLD Benchmark ---
        val oldTimeNs = measureNanoTime {
            repeat(iterations) {
                oldGetViewTypeLogic(model)
            }
        }

        // --- Run NEW Benchmark ---
        val newTimeNs = measureNanoTime {
            repeat(iterations) {
                // This now uses the cached field access
                model.getViewType()
            }
        }

        // --- Report Results ---
        val oldMs = oldTimeNs / 1_000_000.0
        val newMs = newTimeNs / 1_000_000.0
        val improvement = oldMs / newMs
        
        println("\n===========================================")
        println("🔥 Verses Kernel Benchmark (1,000,000 ops)")
        println("===========================================")
        println("🔴 Old Kernel (Map Lookup):  ${String.format("%.4f", oldMs)} ms")
        println("🟢 New Kernel (Cached Int):  ${String.format("%.4f", newMs)} ms")
        println("-------------------------------------------")
        println("🚀 Speedup Factor:           ${String.format("%.2fx", improvement)} FASTER")
        println("⚡ Latency Reduction:        ${String.format("%.2f%%", (1 - newMs/oldMs) * 100)}")
        println("===========================================\n")
    }
}
