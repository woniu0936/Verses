package com.woniu0936.verses.sample

import android.app.Application
import android.util.Log
import com.woniu0936.verses.core.Verses
import com.woniu0936.verses.core.initialize

class VersesSampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Verses with DSL (Kotlin idiomatic)
        Verses.initialize(this) {
            debug(true) // Enable logs in console
            logTag("VersesSample")
            logToFile(true) // Enable file logging for diagnostic sharing
            
            // Production Error Tracking (Example)
            onError { throwable, message ->
                Log.e("VersesSample", "Telemetry Received: $message", throwable)
                // Here you would typically send to Firebase Crashlytics or Sentry
            }
        }
    }
}
