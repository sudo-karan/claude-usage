package com.claudeusage.app.web

import android.content.Context
import android.webkit.JavascriptInterface
import com.claudeusage.app.Graph
import com.claudeusage.app.data.UsageResponseParser
import com.claudeusage.app.data.model.UsageSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Bridge exposed to the claude.ai page as `UsageBridge`. The injected hook
 * (see WebLoginActivity) forwards every fetch/XHR response here; when a payload
 * parses into recognisable usage meters we persist it and remember the URL so
 * the headless refresher can reuse it later.
 */
class UsageCaptureBridge(
    context: Context,
    private val onCaptured: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    @JavascriptInterface
    fun capture(url: String?, body: String?, headers: String?) {
        if (body.isNullOrBlank()) return
        val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return
        val meters = runCatching { UsageResponseParser.parse(root, System.currentTimeMillis()) }
            .getOrNull().orEmpty()
        if (meters.isEmpty()) return

        val snapshot = UsageSnapshot(
            meters = meters,
            capturedAtEpochMs = System.currentTimeMillis(),
            isLive = true,
        )
        Graph.cache(appContext).save(snapshot)
        if (!url.isNullOrBlank()) {
            WebSessionStore(appContext).learnedUsageUrl = url
        }
        onCaptured()
    }
}
