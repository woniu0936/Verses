package com.woniu0936.verses.core

import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * High-performance, fault-tolerant logger.
 *
 * Features:
 * 1. Async IO with Backpressure (prevents OOM).
 * 2. Thread-safe date formatting.
 * 3. Buffered IO to reduce syscall overhead.
 * 4. Error resilience.
 */
internal object VersesLogger {

    // 1. Config: Max 200 pending logs. If the queue is full, old logs are dropped.
    // This prevents the app from crashing due to OOM if logging goes crazy.
    private const val MAX_QUEUE_SIZE = 200

    private val config get() = Verses.getConfig()

    // 2. Custom Executor with Bounded Queue and Discard Policy
    private val logExecutor = ThreadPoolExecutor(
        1, 1, // Single thread
        0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_QUEUE_SIZE),
        ThreadFactory { r -> Thread(r, "Verses-LogWriter") },
        // Discard the OLDEST log if queue is full (keep the newest info)
        ThreadPoolExecutor.DiscardOldestPolicy()
    )

    // Thread-confined DateFormat (only accessed inside logExecutor)
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    // 3. Keep a buffered writer open to avoid repetitive open/close syscalls
    // Note: In a rigorous implementation, you'd need a flush mechanism or logic to close this
    // when the app goes background, but for a logger, keeping it open is a common trade-off.
    private var logWriter: BufferedWriter? = null
    private var currentLogPath: String? = null

    // --- Public API ---

    fun d(message: String) {
        if (!config.isDebug) return
        Log.d(config.logTag, message)
        enqueueLog("DEBUG", message)
    }

    fun i(message: String) {
        if (!config.isDebug) return
        Log.i(config.logTag, message)
        enqueueLog("INFO ", message)
    }

    fun w(message: String) {
        if (!config.isDebug) return
        Log.w(config.logTag, message)
        enqueueLog("WARN ", message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        // Logcat immediately
        Log.e(config.logTag, message, throwable)

        if (config.isDebug) {
            // Calculate stacktrace only if we are going to write it
            val trace = throwable?.let { "\n${Log.getStackTraceString(it)}" } ?: ""
            enqueueLog("ERROR", message + trace)
        }

        // Telemetry
        throwable?.let { config.errorHandler?.onError(it, message) }
    }

    inline fun lifecycle(event: String, details: () -> String) {
        if (!config.isDebug) return
        val msg = "🔄 [Lifecycle] $event >> ${details()}"
        Log.d(config.logTag, msg)
        enqueueLog("LIFE ", msg)
    }

    /**
     * Specialized logging for diff results.
     */
    fun diff(details: String) {
        if (!config.isDebug) return
        val msg = "⚖️ [Diff] $details"
        Log.d(config.logTag, msg)
        enqueueLog("DIFF ", msg)
    }

    fun perf(action: String, durationMs: Long, details: String) {
        if (!config.isDebug) return

        // String concat here is unavoidable for Logcat, but it's lightweight enough usually.
        val icon = if (durationMs > 10) "⚠️ [PERF_SLOW]" else "⚡ [PERF]"
        val msg = "$icon $action took ${durationMs}ms | $details"

        if (durationMs > 10) Log.w(config.logTag, msg) else Log.d(config.logTag, msg)
        enqueueLog("PERF ", msg)
    }

    // --- Internal Logic ---

    private fun enqueueLog(level: String, message: String) {
        if (!config.isLogToFile) return

        // Capture dependencies (path, time) on the calling thread to ensure accuracy
        val filePath = Verses.logFilePath ?: return
        val timestamp = System.currentTimeMillis()

        // Submit to the background writer
        // Because we use DiscardOldestPolicy, this is safe even in loops.
        try {
            logExecutor.execute {
                writeLog(filePath, timestamp, level, message)
            }
        } catch (e: Exception) {
            // Executor might reject if shutting down, ignore.
        }
    }

    /**
     * Runs strictly on the single background thread.
     */
    private fun writeLog(path: String, timeMillis: Long, level: String, message: String) {
        try {
            val writer = getWriter(path)

            val timeString = fileDateFormat.format(Date(timeMillis))

            // Format: [2023-01-01 12:00:00.000] DEBUG: message
            writer.write("[")
            writer.write(timeString)
            writer.write("] ")
            writer.write(level)
            writer.write(": ")
            writer.write(message)
            writer.newLine()

            // Important: Flush immediately or periodically.
            // For debug logs, flushing immediately is safer to prevent data loss on crash.
            // If performance is absolute key, flush only every N lines.
            writer.flush()

        } catch (e: IOException) {
            // If writing fails, try to close and reset so next attempt might succeed
            closeWriter()
            Log.e("VersesLogger", "File write failed", e)
        }
    }

    /**
     * Manages the BufferedWriter instance.
     * Reuses the existing writer if the path hasn't changed.
     */
    private fun getWriter(path: String): BufferedWriter {
        if (logWriter != null && currentLogPath == path) {
            return logWriter!!
        }

        // Path changed or writer closed, recreate.
        closeWriter()

        val file = File(path)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
        }

        // BufferedWriter reduces actual disk syscalls by buffering locally
        val newWriter = BufferedWriter(FileWriter(file, true), 8192) // 8KB Buffer
        logWriter = newWriter
        currentLogPath = path
        return newWriter
    }

    private fun closeWriter() {
        try {
            logWriter?.close()
        } catch (e: Exception) { /* ignore */
        }
        logWriter = null
        currentLogPath = null
    }
}