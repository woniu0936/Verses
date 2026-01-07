package com.woniu0936.verses.core

import android.util.Log
import io.mockk.*
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
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.getStackTraceString(any<Throwable>()) } answers { firstArg<Throwable>().message ?: "" }

        tempLogDir = Files.createTempDirectory("verses_test").toFile()
        logFile = File(tempLogDir, "verses_diagnostic.log")
        Verses.logFilePath = logFile.absolutePath
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        tempLogDir.deleteRecursively()
    }

    @Test
    fun `test logging to file works when enabled`() {
        val mockContext = mockk<android.content.Context>(relaxed = true) {
            every { getExternalFilesDir(any()) } returns tempLogDir
        }
        
        Verses.initialize(mockContext) {
            debug(true)
            logToFile(true)
        }

        VersesLogger.d("This is a test debug message")
        VersesLogger.e("This is a test error message", Exception("Oops"))

        // Give some time for the async logger to write to file
        Thread.sleep(500)

        assertTrue(logFile.exists())
        val content = logFile.readText()
        assertTrue(content.contains("DEBUG: This is a test debug message"))
        // Based on the updated VersesLogger.e, it appends trace via message + trace
        // where trace = "\n" + stackTrace. 
        // We mocked stackTrace to return the message ("Oops").
        assertTrue(content.contains("ERROR: This is a test error message"))
        assertTrue(content.contains("Oops"))
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
        Thread.sleep(200)

        if (logFile.exists()) {
            val content = logFile.readText()
            assertTrue(!content.contains("Should not be in file"))
        }
    }
}