package com.woniu0936.verses.sample.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.woniu0936.verses.core.Verses

/**
 * Utility class to handle the "Last Mile" of user experience for sharing.
 */
object ShareUtils {

    /**
     * Helper to share the Verses diagnostic log file.
     *
     * @param context The context to start the activity.
     * @param title The title for the chooser dialog.
     */
    @JvmStatic
    fun shareLogFile(context: Context, title: String = "Share Diagnostic Log") {
        val shareIntent = Verses.getShareLogIntent(context)
        if (shareIntent == null) {
            Toast.makeText(context, "Log file not found or Verses not initialized.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val chooser = Intent.createChooser(shareIntent, title)
            // Ensure we can handle the intent
            if (shareIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooser)
            } else {
                Toast.makeText(context, "No app available to handle file sharing.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing log: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Helper to share the Verses diagnostic log file via Email with a professional template.
     *
     * @param context The context to start the activity.
     * @param supportEmail The destination support email address.
     */
    @JvmStatic
    fun shareLogViaEmail(context: Context, supportEmail: String = "support@verses.io") {
        val shareIntent = Verses.getShareLogIntent(context) ?: run {
            Toast.makeText(context, "Log file not found or Verses not initialized.", Toast.LENGTH_SHORT).show()
            return
        }

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (e: Exception) { "Unknown" }

        // Build a professional email template inspired by top commercial apps
        val emailBody = StringBuilder().apply {
            append("Dear Support Team,\n\n")
            append("I am reporting an issue with the Verses library. Please find the attached diagnostic log for analysis.\n\n")
            append("--- Environment Details ---\n")
            append("• App Name: Verses Sample\n")
            append("• App Version: $appVersion\n")
            append("• Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
            append("• Android OS: Version ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
            append("---------------------------\n\n")
            append("Issue Description:\n")
            append("(Please describe the steps to reproduce the issue or the expected vs. actual behavior below)\n")
            append("\n\n\n")
            append("Best regards,\n")
            append("Sent from Verses Diagnostic Tool")
        }.toString()

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            // Using "message/rfc822" to specifically target email clients
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
            putExtra(Intent.EXTRA_SUBJECT, "[Bug Report] Verses Diagnostic - v$appVersion")
            putExtra(Intent.EXTRA_TEXT, emailBody)
            
            // Extract URI from the base share intent
            val streamUri = shareIntent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
            putExtra(Intent.EXTRA_STREAM, streamUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(Intent.createChooser(emailIntent, "Send Bug Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "No email client installed.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Helper to share simple text (e.g., sharing a URL or a snippet).
     */
    @JvmStatic
    fun shareText(context: Context, text: String, title: String = "Share via") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
