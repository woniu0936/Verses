package com.woniu0936.verses.core

import android.util.Log

/**
 * Internal logger used by Verses to provide diagnostic information.
 */
internal object VersesLogger {

    fun d(message: String) {
        if (Verses.isDebug) {
            Log.d(Verses.logTag, message)
        }
    }

    fun i(message: String) {
        Log.i(Verses.logTag, message)
    }

    fun w(message: String) {
        Log.w(Verses.logTag, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(Verses.logTag, message, throwable)
    }

    /**
     * Specialized logging for lifecycle events to make them stand out.
     */
    fun lifecycle(event: String, details: String) {
        if (Verses.isDebug) {
            Log.d(Verses.logTag, "🔄 [Lifecycle] $event >> $details")
        }
    }

    /**
     * Specialized logging for diff results.
     */
    fun diff(details: String) {
        if (Verses.isDebug) {
            Log.d(Verses.logTag, "⚖️ [Diff] $details")
        }
    }
}
