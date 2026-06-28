package com.claudeusage.app.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.claudeusage.app.MainActivity

/**
 * Transparent activity that catches the `claudeusage://oauth/callback` redirect,
 * forwards the full URI onto [AuthRedirectBus], then bounces back to the app.
 */
class OAuthRedirectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.data?.toString()?.let(AuthRedirectBus::publish)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        finish()
    }
}
