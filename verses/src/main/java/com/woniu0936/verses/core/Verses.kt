package com.woniu0936.verses.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Functional interface for global error handling in Verses.
 */
fun interface VersesErrorHandler {
    /**
     * Triggered when an internal error occurs.
     * @param throwable The actual exception.
     * @param message A descriptive diagnostic message.
     */
    fun onError(throwable: Throwable, message: String)
}

/**
 * Immutable configuration for the Verses library.
 * Use [VersesConfig.Builder] to create an instance.
 */
class VersesConfig private constructor(
    val isDebug: Boolean,
    val logTag: String,
    val isLogToFile: Boolean,
    val errorHandler: VersesErrorHandler?
) {
    class Builder {
        private var isDebug: Boolean = false
        private var logTag: String = "Verses"
        private var isLogToFile: Boolean = false
        private var errorHandler: VersesErrorHandler? = null

        fun debug(enabled: Boolean) = apply { this.isDebug = enabled }
        fun logTag(tag: String) = apply { this.logTag = tag }
        fun logToFile(enabled: Boolean) = apply { this.isLogToFile = enabled }
        fun onError(handler: VersesErrorHandler) = apply { this.errorHandler = handler }

        fun build() = VersesConfig(isDebug, logTag, isLogToFile, errorHandler)
    }
}

/**
 * Global entry point for Verses library management and diagnostic tools.
 */
object Verses {
    private var config: VersesConfig = VersesConfig.Builder().build()

    @PublishedApi
    internal var logFilePath: String? = null

    /**
     * Initializes Verses with the provided configuration.
     * Primarily for Java users or explicit configuration.
     */
    @JvmStatic
    fun initialize(context: Context, config: VersesConfig) {
        this.config = config
        setupFileLogging(context)
    }

    /**
     * Internal access to the current configuration.
     */
    internal fun getConfig(): VersesConfig = config

    private fun setupFileLogging(context: Context) {
        val dir = context.getExternalFilesDir("verses_logs") ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        logFilePath = File(dir, "verses_diagnostic.log").absolutePath
    }

    /**
     * Returns the log file if it has been initialized.
     */
    @JvmStatic
    fun getLogFile(): File? = logFilePath?.let { File(it) }

    /**
     * Utility to create a share intent for the log file.
     */
    @JvmStatic
    fun getShareLogIntent(context: Context): Intent? {
        val file = getLogFile() ?: return null
        if (!file.exists()) return null
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.verses.fileprovider",
            file
        )
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

/**
 * Kotlin idiomatic DSL for initializing Verses.
 * 
 * Usage:
 * ```kotlin
 * Verses.initialize(context) {
 *     debug(true)
 *     logToFile(true)
 *     onError { throwable, message -> ... }
 * }
 * ```
 */
inline fun Verses.initialize(context: Context, crossinline block: VersesConfig.Builder.() -> Unit) {
    val builder = VersesConfig.Builder()
    builder.block()
    initialize(context, builder.build())
}
