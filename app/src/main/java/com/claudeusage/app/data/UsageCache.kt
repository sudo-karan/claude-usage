package com.claudeusage.app.data

import android.content.Context
import com.claudeusage.app.data.model.UsageSnapshot
import kotlinx.serialization.json.Json

/**
 * Last-known snapshot, persisted as JSON in plain prefs so the widget and the
 * background workers can render instantly without a network round-trip. Also
 * remembers which meters we've already notified about, to avoid duplicate alerts.
 */
class UsageCache(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(snapshot: UsageSnapshot) {
        prefs.edit().putString(KEY_SNAPSHOT, json.encodeToString(UsageSnapshot.serializer(), snapshot)).apply()
    }

    fun load(): UsageSnapshot? {
        val raw = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        return runCatching { json.decodeFromString(UsageSnapshot.serializer(), raw) }.getOrNull()
    }

    /**
     * Last usage fraction we observed for a meter, used to detect a real reset
     * (usage dropping back toward zero). Returns -1f when never seen.
     */
    fun lastSeenFraction(meterId: String): Float = prefs.getFloat(KEY_FRACTION + meterId, -1f)

    fun setLastSeenFraction(meterId: String, fraction: Float) {
        prefs.edit().putFloat(KEY_FRACTION + meterId, fraction).apply()
    }

    private companion object {
        const val FILE = "claude_usage_cache"
        const val KEY_SNAPSHOT = "snapshot_json"
        const val KEY_FRACTION = "last_fraction_"
    }
}
