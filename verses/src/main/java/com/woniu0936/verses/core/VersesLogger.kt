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
        val config = Verses.getConfig()
        if (config.isDebug) {
            Log.d(config.logTag, message)
            if (config.isLogToFile) writeToFile("DEBUG", message)
        }
    }

    fun i(message: String) {
        val config = Verses.getConfig()
        if (config.isDebug) {
            Log.i(config.logTag, message)
            if (config.isLogToFile) writeToFile("INFO ", message)
        }
    }

    fun w(message: String) {
        val config = Verses.getConfig()
        if (config.isDebug) {
            Log.w(config.logTag, message)
            if (config.isLogToFile) writeToFile("WARN ", message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        val config = Verses.getConfig()
        Log.e(config.logTag, message, throwable)
        
        if (config.isDebug && config.isLogToFile) {
            writeToFile("ERROR", "$message | ${throwable?.message}")
        }

        // Production telemetry: Send to global error handler
        throwable?.let {
            config.errorHandler?.onError(it, message)
        }
    }

    /**
     * Specialized logging for lifecycle events.
     */
    fun lifecycle(event: String, details: String) {
        val config = Verses.getConfig()
        if (config.isDebug) {
            val msg = "🔄 [Lifecycle] $event >> $details"
            Log.d(config.logTag, msg)
            if (config.isLogToFile) writeToFile("LIFE ", msg)
        }
    }

    /**
     * Specialized logging for diff results.
     */
    fun diff(details: String) {
        val config = Verses.getConfig()
        if (config.isDebug) {
            val msg = "⚖️ [Diff] $details"
            Log.d(config.logTag, msg)
            if (config.isLogToFile) writeToFile("DIFF ", msg)
        }
    }

    /**
     * Specialized logging for performance tracking.
     */
    fun perf(action: String, durationMs: Long, details: String) {
        val config = Verses.getConfig()
        if (config.isDebug) {
            val icon = if (durationMs > 10) "⚠️ [PERF_SLOW]" else "⚡ [PERF]"
            val msg = "$icon $action took ${durationMs}ms | $details"
            if (durationMs > 10) Log.w(config.logTag, msg) else Log.d(config.logTag, msg)
            if (config.isLogToFile) writeToFile("PERF ", msg)
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