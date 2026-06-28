package com.claudeusage.app.data

import android.content.Context
import com.claudeusage.app.data.model.UsageSnapshot
import com.claudeusage.app.web.WebSessionStore

/** Result of a refresh: always a snapshot to render, plus an optional note. */
data class RefreshOutcome(val snapshot: UsageSnapshot, val errorMessage: String? = null)

/**
 * Single source of truth for usage. When a claude.ai web session exists it
 * fetches live usage with the captured cookies; otherwise it falls back to the
 * cached snapshot (which the WebView login also populates) or the bundled
 * sample — so callers always have something to render.
 */
class UsageRepository(
    context: Context,
    private val session: WebSessionStore = WebSessionStore(context),
    private val web: ClaudeWebUsageDataSource = ClaudeWebUsageDataSource(),
    private val cache: UsageCache = UsageCache(context),
) {
    val isLoggedIn: Boolean get() = session.isLoggedIn
    val accountLabel: String? get() = session.accountLabel

    /** Instant, no-network reading for first paint. */
    fun cachedOrSample(): UsageSnapshot = cache.load() ?: SampleUsage.snapshot()

    fun cached(): UsageSnapshot? = cache.load()

    suspend fun refresh(now: Long = System.currentTimeMillis()): RefreshOutcome {
        val cookie = session.cookieHeader
        if (cookie.isNullOrBlank()) {
            return RefreshOutcome(cache.load() ?: SampleUsage.snapshot(now))
        }
        return web.fetchUsage(cookie, session.learnedUsageUrl, now).fold(
            onSuccess = { snapshot ->
                cache.save(snapshot)
                RefreshOutcome(snapshot)
            },
            onFailure = { err ->
                RefreshOutcome(
                    cache.load() ?: SampleUsage.snapshot(now),
                    "Showing last known usage — ${err.message ?: "couldn't reach claude.ai"}.",
                )
            },
        )
    }

    fun logout() {
        session.clear()
    }
}
