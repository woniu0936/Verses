package com.woniu0936.verses.core

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VersesConfigTest {

    private val context = mockk<Context>(relaxed = true) {
        every { getExternalFilesDir(any()) } returns File("/tmp/verses_logs")
        every { filesDir } returns File("/tmp/verses_files")
    }

    @Test
    fun `test Builder pattern works correctly`() {
        val config = VersesConfig.Builder()
            .debug(true)
            .logTag("TestTag")
            .logToFile(true)
            .build()

        assertTrue(config.isDebug)
        assertEquals("TestTag", config.logTag)
        assertTrue(config.isLogToFile)
    }

    @Test
    fun `test DSL initialization works correctly`() {
        Verses.initialize(context) {
            debug(true)
            logTag("DSLTag")
            logToFile(false)
        }

        val config = Verses.getConfig()
        assertTrue(config.isDebug)
        assertEquals("DSLTag", config.logTag)
        assertFalse(config.isLogToFile)
    }

    @Test
    fun `test error handler is triggered`() {
        var errorCalled = false
        var capturedMessage = ""

        val handler = VersesErrorHandler { _, message ->
            errorCalled = true
            capturedMessage = message
        }

        Verses.initialize(context) {
            onError(handler)
        }

        Verses.getConfig().errorHandler?.onError(Exception("Test"), "Error Msg")
        
        assertTrue(errorCalled)
        assertEquals("Error Msg", capturedMessage)
    }

    @Test
    fun `test log file path generation`() {
        Verses.initialize(context) {
            logToFile(true)
        }
        
        val file = Verses.getLogFile()
        assertNotNull(file)
        assertTrue(file!!.absolutePath.contains("verses_diagnostic.log"))
    }
}
