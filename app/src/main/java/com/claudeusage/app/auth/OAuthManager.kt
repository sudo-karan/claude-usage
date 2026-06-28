package com.claudeusage.app.auth

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Drives the PKCE authorization-code flow against [OAuthConfig] and keeps a
 * valid access token available for the data layer.
 */
class OAuthManager(
    context: Context,
    private val tokens: TokenStore = TokenStore(context),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val isLoggedIn: Boolean get() = tokens.isLoggedIn
    val accountLabel: String? get() = tokens.accountLabel

    /** Builds the URL the user opens in a browser / custom tab to authorize. */
    fun startAuthorization(useAppRedirect: Boolean): Uri {
        val pkce = Pkce.generate()
        tokens.beginLogin(pkce)
        val redirect = if (useAppRedirect) OAuthConfig.REDIRECT_URI_APP else OAuthConfig.REDIRECT_URI_MANUAL
        return Uri.parse(OAuthConfig.AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("code", "true")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", OAuthConfig.CLIENT_ID)
            .appendQueryParameter("redirect_uri", redirect)
            .appendQueryParameter("scope", OAuthConfig.scopeParam())
            .appendQueryParameter("code_challenge", pkce.challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", pkce.state)
            .build()
    }

    /**
     * Completes login from a raw authorization "code" (optionally suffixed with
     * "#state" as the hosted callback returns it) or a full redirect URI.
     */
    suspend fun completeAuthorization(rawInput: String, usedAppRedirect: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (code, returnedState) = parseCodeAndState(rawInput)
                val expectedState = tokens.pendingState
                if (!returnedState.isNullOrEmpty() && expectedState != null && returnedState != expectedState) {
                    error("State mismatch — please try signing in again.")
                }
                val verifier = tokens.pendingVerifier
                    ?: error("No login in progress. Start sign-in again.")
                val redirect = if (usedAppRedirect) OAuthConfig.REDIRECT_URI_APP else OAuthConfig.REDIRECT_URI_MANUAL

                val body = buildJsonBody(
                    "grant_type" to "authorization_code",
                    "code" to code,
                    "redirect_uri" to redirect,
                    "client_id" to OAuthConfig.CLIENT_ID,
                    "code_verifier" to verifier,
                    "state" to (returnedState ?: expectedState ?: ""),
                )
                val obj = postJson(OAuthConfig.TOKEN_URL, body)
                persist(obj)
            }
        }

    /** Returns a non-expired access token, refreshing transparently if needed. */
    suspend fun validAccessToken(): String? = withContext(Dispatchers.IO) {
        val current = tokens.accessToken ?: return@withContext null
        val skewMs = 60_000L
        if (System.currentTimeMillis() < tokens.expiresAtEpochMs - skewMs) return@withContext current
        refresh().getOrNull() ?: current
    }

    suspend fun refresh(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val refresh = tokens.refreshToken ?: error("Not signed in.")
            val body = buildJsonBody(
                "grant_type" to "refresh_token",
                "refresh_token" to refresh,
                "client_id" to OAuthConfig.CLIENT_ID,
            )
            val obj = postJson(OAuthConfig.TOKEN_URL, body)
            persist(obj)
            tokens.accessToken ?: error("Refresh returned no token.")
        }
    }

    fun logout() = tokens.clear()

    // --- internals -------------------------------------------------------

    private fun persist(obj: JsonObject) {
        val access = obj["access_token"]?.jsonPrimitive?.contentOrNull
            ?: error("Token response missing access_token.")
        val refresh = obj["refresh_token"]?.jsonPrimitive?.contentOrNull
        val expiresIn = obj["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L
        val account = obj["account"]?.let { (it as? JsonObject) }
            ?.get("email_address")?.jsonPrimitive?.contentOrNull
        tokens.saveTokens(access, refresh, expiresIn, account)
    }

    private fun parseCodeAndState(raw: String): Pair<String, String?> {
        val trimmed = raw.trim()
        // Full redirect URI? pull code/state from the query.
        if (trimmed.startsWith("http") || trimmed.startsWith("claudeusage://")) {
            val uri = Uri.parse(trimmed)
            val code = uri.getQueryParameter("code") ?: ""
            val state = uri.getQueryParameter("state")
            return code to state
        }
        // Hosted callback hands back "code#state".
        val hashIdx = trimmed.indexOf('#')
        return if (hashIdx >= 0) {
            trimmed.substring(0, hashIdx) to trimmed.substring(hashIdx + 1)
        } else {
            trimmed to null
        }
    }

    private fun buildJsonBody(vararg pairs: Pair<String, String>): String {
        val encoded = pairs.joinToString(",") { (k, v) ->
            "\"$k\":\"${v.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }
        return "{$encoded}"
    }

    private fun postJson(url: String, jsonBody: String): JsonObject {
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(JSON_MEDIA))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()
        http.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                error("Sign-in failed (HTTP ${resp.code}). ${text.take(180)}")
            }
            return json.parseToJsonElement(text) as? JsonObject
                ?: error("Unexpected token response.")
        }
    }

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
