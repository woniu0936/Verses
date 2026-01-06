package com.woniu0936.verses.core

import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VersesLoggerTest {

    private lateinit var tempLogDir: File
    private lateinit var logFile: File

    @Before
    fun setup() {
        // Mock android.util.Log to prevent RuntimeException during unit tests
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        tempLogDir = Files.createTempDirectory("verses_test").toFile()
        logFile = File(tempLogDir, "verses_diagnostic.log")
        
        // Inject temp path for testing
        Verses.logFilePath = logFile.absolutePath
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        tempLogDir.deleteRecursively()
    }

    @Test
    fun `test logging to file works when enabled`() {
        // Setup config
        val config = VersesConfig.Builder()
            .debug(true)
            .logToFile(true)
            .build()
        
        // Manual injection for test since Verses.initialize requires a real Context for path
        // We already set Verses.logFilePath in @Before
        
        // We need to use reflection or make Verses.config accessible for testing
        // But since Verses is an object, we can just use its initialize DSL
        
        // Mock context for initialize
        val mockContext = mockk<android.content.Context>(relaxed = true) {
            every { getExternalFilesDir(any()) } returns tempLogDir
        }
        
        Verses.initialize(mockContext) {
            debug(true)
            logToFile(true)
        }

        VersesLogger.d("This is a test debug message")
        VersesLogger.e("This is a test error message", Exception("Oops"))

        assertTrue(logFile.exists())
        val content = logFile.readText()
        assertTrue(content.contains("DEBUG: This is a test debug message"))
        assertTrue(content.contains("ERROR: This is a test error message | Oops"))
    }

    @Test
    fun `test logging to file is disabled when isLogToFile is false`() {
        val mockContext = mockk<android.content.Context>(relaxed = true) {
            every { getExternalFilesDir(any()) } returns tempLogDir
        }
        
        Verses.initialize(mockContext) {
            debug(true)
            logToFile(false)
        }

        VersesLogger.i("Should not be in file")

        // File might exist from previous tests or initialization, but content shouldn't have the message
        if (logFile.exists()) {
            val content = logFile.readText()
            assertTrue(!content.contains("Should not be in file"))
        }
    }
}
