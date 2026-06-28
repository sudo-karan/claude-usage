package com.claudeusage.app.web

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted store for the claude.ai web session: the cookie header captured
 * after the user logs in via WebView, the usage endpoint we "learned" by
 * watching the page's own network calls, and a display label for the account.
 *
 * No password or OAuth client is involved — this is purely the user's own
 * authenticated browser session, reused to read their own usage.
 */
class WebSessionStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var cookieHeader: String?
        get() = prefs.getString(KEY_COOKIE, null)
        set(value) = prefs.edit().putString(KEY_COOKIE, value).apply()

    /** Absolute usage URL discovered from the page's own fetch/XHR traffic. */
    var learnedUsageUrl: String?
        get() = prefs.getString(KEY_USAGE_URL, null)
        set(value) = prefs.edit().putString(KEY_USAGE_URL, value).apply()

    var accountLabel: String?
        get() = prefs.getString(KEY_ACCOUNT, null)
        set(value) = prefs.edit().putString(KEY_ACCOUNT, value).apply()

    val isLoggedIn: Boolean get() = !cookieHeader.isNullOrBlank()

    fun saveSession(cookieHeader: String, account: String?) {
        prefs.edit().apply {
            putString(KEY_COOKIE, cookieHeader)
            if (account != null) putString(KEY_ACCOUNT, account)
        }.apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val FILE_NAME = "claude_web_session"
        const val KEY_COOKIE = "cookie_header"
        const val KEY_USAGE_URL = "usage_url"
        const val KEY_ACCOUNT = "account_label"
    }
}
