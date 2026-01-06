package com.woniu0936.verses.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Global configuration entry point for the Verses library.
 */
object Verses {
    /**
     * Set to true to enable detailed internal logging for debugging purposes.
     * Default is false.
     */
    @JvmStatic
    var isDebug: Boolean = false

    /**
     * Custom tag used for all Verses log output.
     */
    @JvmStatic
    var logTag: String = "Verses"

    /**
     * Whether to write logs to a local file. 
     * Requires [init] to be called first to establish the file path.
     * Only recommended for [isDebug] mode.
     */
    @JvmStatic
    var isLogToFile: Boolean = false

    /**
     * Global error callback for production monitoring.
     * 
     * Use this to bridge Verses internal errors to your crash reporting tool 
     * like Sentry, Bugly, or Firebase Crashlytics.
     * 
     * Signature: (Throwable, String) -> Unit where String is a descriptive message.
     */
    @JvmStatic
    var onError: ((Throwable, String) -> Unit)? = null

    @PublishedApi
    internal var logFilePath: String? = null

    /**
     * Initializes the Verses environment. 
     * Mandatory if [isLogToFile] is enabled or if using [getLogFile].
     */
    @JvmStatic
    fun init(context: Context) {
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
     * 
     * Note: You must define a FileProvider in your Manifest with authority:
     * "${context.packageName}.verses.fileprovider"
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