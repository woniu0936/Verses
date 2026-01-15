package com.woniu0936.verses.core.pool

import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.ConcurrentHashMap

/**
 * A registry to persist the scroll state of nested RecyclerViews.
 * 
 * This ensures that when a horizontal list scrolls off-screen and comes back,
 * it stays exactly where the user left it.
 */
internal object VerseStateRegistry {
    // Key: Model ID (String/Long/Int), Value: LayoutManager.onSaveInstanceState()
    private val states = ConcurrentHashMap<Any, Parcelable>()
    
    // [Race Condition Guard] Prevents saving state during Activity teardown
    @Volatile
    private var isLocked = false

    fun saveState(id: Any, recyclerView: RecyclerView) {
        if (isLocked) {
            com.woniu0936.verses.core.VersesLogger.d("StateRegistry: Ignore save for $id (Locked)")
            return
        }
        val state = recyclerView.layoutManager?.onSaveInstanceState()
        if (state != null) {
            com.woniu0936.verses.core.VersesLogger.d("StateRegistry: Saved state for $id")
            states[id] = state
        }
    }

    fun restoreState(id: Any, recyclerView: RecyclerView) {
        val state = states[id]
        if (state != null) {
            com.woniu0936.verses.core.VersesLogger.d("StateRegistry: Restored state for $id")
            recyclerView.layoutManager?.onRestoreInstanceState(state)
        }
    }

    /**
     * Clears all saved states. Call this when the parent page is destroyed.
     */
    fun clear() {
        com.woniu0936.verses.core.VersesLogger.d("StateRegistry: Clearing all states")
        states.clear()
        isLocked = true
    }
    
    /**
     * Unlocks the registry for a new session.
     */
    fun unlock() {
        isLocked = false
    }
}
