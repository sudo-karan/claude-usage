package com.claudeusage.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

/** Opens a URL in a themed Chrome Custom Tab, falling back to any browser. */
object CustomTab {
    fun open(context: Context, uri: Uri) {
        val colors = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(0xFF1E1A14.toInt())
            .build()
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .setDefaultColorSchemeParams(colors)
            .build()
        try {
            intent.launchUrl(context, uri)
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "No browser available to sign in.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
