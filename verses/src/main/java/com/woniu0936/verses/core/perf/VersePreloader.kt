package com.woniu0936.verses.core.perf

import android.content.Context
import android.view.Choreographer
import android.view.ViewGroup
import android.widget.FrameLayout
import com.woniu0936.verses.core.VersesLogger
import com.woniu0936.verses.core.pool.VerseRecycledViewPool
import com.woniu0936.verses.core.pool.VerseTypeRegistry
import com.woniu0936.verses.model.SmartViewHolder
import com.woniu0936.verses.model.VerseModel
import java.util.concurrent.ConcurrentHashMap

/**
 * A proactive performance engine that manages background View inflation and pool warming.
 *
 * It features a "Burst Mode" for rapid recovery when the pool is empty and 
 * "Interleaved Production" to maintain steady-state performance without UI jank.
 */
object VersePreloader {

    private val pendingTasks = ConcurrentHashMap<Int, Boolean>()

    /**
     * Schedules proactive pre-inflation of the provided [models].
     */
    fun preload(context: Context, models: List<VerseModel<*>>, countPerType: Int = 5) {
        val dummyParent = FrameLayout(context)
        val pool = VerseRecycledViewPool.GLOBAL
        
        val uniqueModels = models.distinctBy { it.getViewType() }
        
        for (model in uniqueModels) {
            val viewType = model.getViewType()
            
            // [Deterministic Scaling] Ensure pool can hold the warmed items
            if (pool.getPoolSize(viewType) < countPerType) {
                pool.setPoolSize(viewType, countPerType)
            }
            
            if (pendingTasks.putIfAbsent(viewType, true) != null) continue

            val currentCount = pool.getRecycledViewCount(viewType)
            val needed = (countPerType - currentCount).coerceAtLeast(0)
            
            if (needed <= 0) {
                pendingTasks.remove(viewType)
                continue
            }

            // Standard production budget (8ms)
            scheduleInterleavedProduction(dummyParent, model, needed, countPerType) {
                pendingTasks.remove(viewType)
            }
        }
    }

    private fun optimizeViewHolder(holder: SmartViewHolder) {
        val view = holder.itemView
        if (view is ViewGroup) {
            findAndCacheNestedRv(holder, view)
        }
        holder.isOptimized = true
    }

    private fun findAndCacheNestedRv(holder: SmartViewHolder, view: ViewGroup) {
        if (view is androidx.recyclerview.widget.RecyclerView) {
            holder.nestedRv = view
            return
        }
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is ViewGroup) {
                findAndCacheNestedRv(holder, child)
                if (holder.nestedRv != null) return
            }
        }
    }

    private fun scheduleInterleavedProduction(
        parent: ViewGroup,
        model: VerseModel<*>,
        needed: Int,
        targetCount: Int,
        onComplete: () -> Unit
    ) {
        var produced = 0
        val pool = VerseRecycledViewPool.GLOBAL
        val viewType = model.getViewType()
        val baseBudgetNs = 8_000_000L 

        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                val startNs = System.nanoTime()
                
                while (produced < needed && pool.getRecycledViewCount(viewType) < targetCount) {
                    try {
                        // [Safety] Use standard factory to guarantee ViewHolder consistency
                        val holder = model.createHolder(parent)
                        optimizeViewHolder(holder)
                        pool.putRecycledView(holder)
                        produced++
                        VersesLogger.i("Proactive Preload: ${model.javaClass.simpleName} ($produced/$needed)")
                    } catch (e: Exception) {
                        VersesLogger.e("Preload failed", e)
                        onComplete()
                        return
                    }

                    if (System.nanoTime() - startNs > baseBudgetNs) break
                }

                if (produced < needed && pool.getRecycledViewCount(viewType) < targetCount) {
                    Choreographer.getInstance().postFrameCallback(this)
                }
                else {
                    onComplete()
                }
            }
        }
        Choreographer.getInstance().postFrameCallback(callback)
    }
}
