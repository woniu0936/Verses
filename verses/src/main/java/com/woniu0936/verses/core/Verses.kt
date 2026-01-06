package com.woniu0936.verses.core

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
}
