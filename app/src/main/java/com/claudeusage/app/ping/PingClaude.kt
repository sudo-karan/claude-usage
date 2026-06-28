package com.claudeusage.app.ping

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Opens a brand-new Claude conversation with "Hi" pre-filled, which starts a
 * fresh usage window. Routes to the installed Claude app via Android App Links
 * when present, otherwise falls back to claude.ai in the browser.
 */
object PingClaude {
    private const val NEW_CHAT_URL = "https://claude.ai/new?q=Hi"
    private const val CLAUDE_PACKAGE = "com.anthropic.claude"

    fun ping(context: Context) {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(NEW_CHAT_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Prefer the official app if it's installed and handles the link.
            if (isClaudeAppInstalled(context)) setPackage(CLAUDE_PACKAGE)
        }
        try {
            context.startActivity(view)
        } catch (_: ActivityNotFoundException) {
            // App couldn't take the link — retry without the package constraint.
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(NEW_CHAT_URL))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "No browser or Claude app available.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isClaudeAppInstalled(context: Context): Boolean =
        runCatching {
            context.packageManager.getLaunchIntentForPackage(CLAUDE_PACKAGE) != null
        }.getOrDefault(false)
}
