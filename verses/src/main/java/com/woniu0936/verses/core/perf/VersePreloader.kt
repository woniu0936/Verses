package com.woniu0936.verses.core.perf

import android.content.Context
import android.view.Choreographer
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
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
     * Automatically preloads all known model prototypes.
     */
    internal fun autoPreload(context: Context) {
        val prototypes = VerseTypeRegistry.getAllPrototypes()
        if (prototypes.isNotEmpty()) {
            preload(context, prototypes, countPerType = 3)
        }
    }

    /**
     * Schedules proactive pre-inflation of the provided [models].
     */
    fun preload(context: Context, models: List<VerseModel<*>>, countPerType: Int = 5) {
        val pool = VerseRecycledViewPool.GLOBAL
        val asyncInflater = AsyncLayoutInflater(context)
        val dummyParent = FrameLayout(context)
        
        val uniqueModels = models.distinctBy { it.getViewType() }
        
        for (model in uniqueModels) {
            val viewType = model.getViewType()
            
            // [Debouncing] Skip if a task for this ViewType is already in progress
            if (pendingTasks.putIfAbsent(viewType, true) != null) continue

            val currentCount = pool.getRecycledViewCount(viewType)
            val needed = (countPerType - currentCount).coerceAtLeast(0)
            
            if (needed <= 0) {
                pendingTasks.remove(viewType)
                continue
            }

            if (model.layoutRes != 0) {
                // Track A: Proactive Async XML Inflation
                var inflatedCount = 0
                repeat(needed) {
                    asyncInflater.inflate(model.layoutRes, dummyParent) { view, _, _ ->
                        try {
                            (view.parent as? ViewGroup)?.removeView(view)
                            val holder = SmartViewHolder(view)
                            model.onViewCreated(view)
                            pool.putRecycledView(holder)
                            VersesLogger.i("Proactive Preload (Async): ${model.javaClass.simpleName} type $viewType")
                        } catch (e: Exception) {
                            VersesLogger.e("Proactive async preload failed", e)
                        } finally {
                            inflatedCount++
                            if (inflatedCount >= needed) {
                                pendingTasks.remove(viewType)
                            }
                        }
                    }
                }
            } else {
                // Track B: Choreographer-Interleaved Production with Burst Mode
                scheduleInterleavedProduction(dummyParent, model, needed, countPerType) {
                    pendingTasks.remove(viewType)
                }
            }
        }
    }

    /**
     * Uses Choreographer to produce views between frames.
     */
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

        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (produced >= needed || pool.getRecycledViewCount(viewType) >= targetCount) {
                    onComplete()
                    return
                }
                
                val batchSize = if (pool.getRecycledViewCount(viewType) == 0) 2 else 1
                
                repeat(batchSize) {
                    if (produced < needed && pool.getRecycledViewCount(viewType) < targetCount) {
                        try {
                            val holder = model.createHolder(parent)
                            pool.putRecycledView(holder)
                            produced++
                            VersesLogger.i("Proactive Preload (Interleaved): ${model.javaClass.simpleName} ($produced/$needed)")
                        } catch (e: Exception) {
                            VersesLogger.e("Interleaved production failed", e)
                            onComplete()
                            return@doFrame
                        }
                    }
                }

                if (produced < needed) {
                    Choreographer.getInstance().postFrameCallback(this)
                } else {
                    onComplete()
                }
            }
        }
        Choreographer.getInstance().postFrameCallback(callback)
    }
}
