package com.woniu0936.verses.core

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Internal logger used by Verses to provide diagnostic information.
 */
internal object VersesLogger {

    private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()) }

    fun d(message: String) {
        if (Verses.isDebug) {
            Log.d(Verses.logTag, message)
            if (Verses.isLogToFile) writeToFile("DEBUG", message)
        }
    }

    fun i(message: String) {
        if (Verses.isDebug) {
            Log.i(Verses.logTag, message)
            if (Verses.isLogToFile) writeToFile("INFO ", message)
        }
    }

    fun w(message: String) {
        if (Verses.isDebug) {
            Log.w(Verses.logTag, message)
            if (Verses.isLogToFile) writeToFile("WARN ", message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        // Always log to console in Debug, but also respect production onError
        Log.e(Verses.logTag, message, throwable)
        
        if (Verses.isDebug && Verses.isLogToFile) {
            writeToFile("ERROR", "$message | ${throwable?.message}")
        }

        // Production telemetry: Send to global error handler
        throwable?.let {
            Verses.onError?.invoke(it, message)
        }
    }

    /**
     * Specialized logging for lifecycle events.
     */
    fun lifecycle(event: String, details: String) {
        if (Verses.isDebug) {
            val msg = "🔄 [Lifecycle] $event >> $details"
            Log.d(Verses.logTag, msg)
            if (Verses.isLogToFile) writeToFile("LIFE ", msg)
        }
    }

    /**
     * Specialized logging for diff results.
     */
    fun diff(details: String) {
        if (Verses.isDebug) {
            val msg = "⚖️ [Diff] $details"
            Log.d(Verses.logTag, msg)
            if (Verses.isLogToFile) writeToFile("DIFF ", msg)
        }
    }

    private fun writeToFile(level: String, message: String) {
        val path = Verses.logFilePath ?: return
        try {
            val timestamp = dateFormat.format(Date())
            val entry = "[$timestamp] $level: $message\n"
            File(path).appendText(entry)
        } catch (e: Exception) {
            // Silence logging errors to prevent recursive crashes
        }
    }
}