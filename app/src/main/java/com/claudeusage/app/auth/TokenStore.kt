package com.claudeusage.app.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted-at-rest storage for OAuth tokens and the short-lived PKCE state of
 * an in-flight login. Backed by Jetpack Security's [EncryptedSharedPreferences];
 * the underlying file is excluded from cloud backup (see res/xml/backup_rules).
 */
class TokenStore(context: Context) {

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

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_ACCESS, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_REFRESH, value).apply()

    var expiresAtEpochMs: Long
        get() = prefs.getLong(KEY_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_EXPIRES, value).apply()

    var accountLabel: String?
        get() = prefs.getString(KEY_ACCOUNT, null)
        set(value) = prefs.edit().putString(KEY_ACCOUNT, value).apply()

    // --- transient PKCE state for the active authorization request ---
    var pendingVerifier: String?
        get() = prefs.getString(KEY_VERIFIER, null)
        set(value) = prefs.edit().putString(KEY_VERIFIER, value).apply()

    var pendingState: String?
        get() = prefs.getString(KEY_STATE, null)
        set(value) = prefs.edit().putString(KEY_STATE, value).apply()

    val isLoggedIn: Boolean get() = !accessToken.isNullOrEmpty()

    fun saveTokens(access: String, refresh: String?, expiresInSeconds: Long, account: String?) {
        prefs.edit().apply {
            putString(KEY_ACCESS, access)
            if (refresh != null) putString(KEY_REFRESH, refresh)
            putLong(KEY_EXPIRES, System.currentTimeMillis() + expiresInSeconds * 1000L)
            if (account != null) putString(KEY_ACCOUNT, account)
            remove(KEY_VERIFIER)
            remove(KEY_STATE)
        }.apply()
    }

    fun beginLogin(pkce: Pkce) {
        prefs.edit()
            .putString(KEY_VERIFIER, pkce.verifier)
            .putString(KEY_STATE, pkce.state)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val FILE_NAME = "claude_secure_tokens"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES = "expires_at"
        const val KEY_ACCOUNT = "account_label"
        const val KEY_VERIFIER = "pending_verifier"
        const val KEY_STATE = "pending_state"
    }
}
