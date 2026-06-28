package com.claudeusage.app.data

import com.claudeusage.app.data.model.UsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Headless usage fetch that reuses the cookies captured during the WebView
 * login. Used for the in-app refresh button and the background worker/widget,
 * so we don't have to spin up a WebView every time.
 *
 * Strategy: hit the endpoint we "learned" from the live page first; otherwise
 * resolve the org id from claude.ai's own /api/organizations and probe the
 * most likely usage paths. Everything is parsed leniently.
 */
class ClaudeWebUsageDataSource(
    private val http: OkHttpClient = defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun fetchUsage(
        cookieHeader: String,
        learnedUrl: String?,
        now: Long = System.currentTimeMillis(),
    ): Result<UsageSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val urls = buildList {
                if (!learnedUrl.isNullOrBlank()) add(learnedUrl)
                resolveOrgId(cookieHeader)?.let { org ->
                    add("https://claude.ai/api/organizations/$org/usage")
                    add("https://claude.ai/api/organizations/$org/rate_limit")
                }
            }.distinct()

            for (url in urls) {
                val body = get(url, cookieHeader) ?: continue
                val root = json.parseToJsonElement(body) as? JsonObject ?: continue
                val meters = UsageResponseParser.parse(root, now)
                if (meters.isNotEmpty()) {
                    return@runCatching UsageSnapshot(meters, now, isLive = true)
                }
            }
            error("No usable usage data from claude.ai session (it may have changed or expired).")
        }
    }

    private fun resolveOrgId(cookieHeader: String): String? {
        val body = get("https://claude.ai/api/organizations", cookieHeader) ?: return null
        val arr = runCatching { json.parseToJsonElement(body) as? JsonArray }.getOrNull() ?: return null
        // Prefer an org that has a chat/claude_pro-like capability; fall back to first.
        return arr.mapNotNull { (it as? JsonObject)?.get("uuid")?.jsonPrimitive?.contentOrNull }
            .firstOrNull()
    }

    private fun get(url: String, cookieHeader: String): String? {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Cookie", cookieHeader)
            .header("Accept", "application/json")
            .header("User-Agent", BROWSER_UA)
            .header("Referer", "https://claude.ai/")
            .build()
        return http.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    }

    private companion object {
        const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
