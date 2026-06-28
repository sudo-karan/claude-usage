package com.claudeusage.app.data

import com.claudeusage.app.auth.OAuthConfig
import com.claudeusage.app.data.model.UsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Fetches the live usage snapshot using a valid OAuth bearer token. */
class ClaudeUsageApi(
    private val http: OkHttpClient = defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun fetchUsage(accessToken: String, now: Long = System.currentTimeMillis()): Result<UsageSnapshot> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(OAuthConfig.USAGE_URL)
                    .get()
                    .header("Authorization", "Bearer $accessToken")
                    .header("anthropic-version", OAuthConfig.ANTHROPIC_VERSION)
                    .header("anthropic-beta", OAuthConfig.ANTHROPIC_BETA)
                    .header("Accept", "application/json")
                    .build()

                http.newCall(request).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        error("Usage request failed (HTTP ${resp.code}).")
                    }
                    val root = json.parseToJsonElement(text) as? JsonObject
                        ?: error("Unexpected usage response shape.")
                    val meters = UsageResponseParser.parse(root, now)
                    if (meters.isEmpty()) error("No recognisable usage windows in response.")
                    UsageSnapshot(
                        meters = meters,
                        capturedAtEpochMs = now,
                        isLive = true,
                    )
                }
            }
        }

    private companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
