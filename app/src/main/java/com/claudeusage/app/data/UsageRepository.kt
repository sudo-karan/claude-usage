package com.claudeusage.app.data

import android.content.Context
import com.claudeusage.app.auth.OAuthManager
import com.claudeusage.app.data.model.UsageSnapshot

/** Result of a refresh: always a snapshot to render, plus an optional note. */
data class RefreshOutcome(val snapshot: UsageSnapshot, val errorMessage: String? = null)

/**
 * Single source of truth for usage. Prefers a live fetch when signed in, then
 * the cached snapshot, then the bundled sample — so callers always get something
 * to render. Persists every live result for the widget and workers.
 */
class UsageRepository(
    context: Context,
    val auth: OAuthManager = OAuthManager(context),
    private val api: ClaudeUsageApi = ClaudeUsageApi(),
    private val cache: UsageCache = UsageCache(context),
) {
    val isLoggedIn: Boolean get() = auth.isLoggedIn
    val accountLabel: String? get() = auth.accountLabel

    /** Instant, no-network reading for first paint. */
    fun cachedOrSample(): UsageSnapshot = cache.load() ?: SampleUsage.snapshot()

    fun cached(): UsageSnapshot? = cache.load()

    suspend fun refresh(now: Long = System.currentTimeMillis()): RefreshOutcome {
        if (!auth.isLoggedIn) {
            return RefreshOutcome(cache.load() ?: SampleUsage.snapshot(now))
        }
        val token = auth.validAccessToken()
            ?: return RefreshOutcome(
                cache.load() ?: SampleUsage.snapshot(now),
                "Your session expired. Please sign in again.",
            )
        return api.fetchUsage(token, now).fold(
            onSuccess = { snapshot ->
                cache.save(snapshot)
                RefreshOutcome(snapshot)
            },
            onFailure = { err ->
                RefreshOutcome(
                    cache.load() ?: SampleUsage.snapshot(now),
                    "Showing last known usage — couldn't reach Claude (${err.message ?: "network error"}).",
                )
            },
        )
    }

    fun logout() {
        auth.logout()
    }
}
