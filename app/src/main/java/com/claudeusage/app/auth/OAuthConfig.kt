package com.claudeusage.app.auth

/**
 * OAuth + API endpoints for signing in with a Claude subscription.
 *
 * These mirror the public "Claude Code" OAuth client (PKCE, no client secret).
 * They are NOT a formally documented public API, so treat them as best-effort:
 * if Anthropic changes the flow, update the constants here in one place. The app
 * degrades gracefully to the bundled sample data when live calls fail.
 *
 * The default redirect uses Anthropic's hosted callback, which returns an
 * authorization code on-screen for the user to paste back ("manual code" flow).
 * This avoids relying on a custom-scheme redirect being whitelisted for the
 * client. A [REDIRECT_URI_APP] custom scheme is also wired up in the manifest
 * for clients/instances that do allow it.
 */
object OAuthConfig {
    const val CLIENT_ID = "9d1c250a-e61b-44d9-88ed-5944d1962f5e"

    const val AUTHORIZE_URL = "https://claude.ai/oauth/authorize"
    const val TOKEN_URL = "https://console.anthropic.com/v1/oauth/token"

    /** Hosted callback that shows the code for manual paste. */
    const val REDIRECT_URI_MANUAL = "https://console.anthropic.com/oauth/code/callback"

    /** Custom-scheme redirect handled by OAuthRedirectActivity. */
    const val REDIRECT_URI_APP = "claudeusage://oauth/callback"

    // Subscription (claude.ai) login must NOT request `org:create_api_key` —
    // that scope only applies to the Console / API-key flow and causes claude.ai
    // to reject the authorize request ("Invalid request format" / "Unknown scope").
    // These two are also all we need to read profile + usage.
    val SCOPES = listOf("user:inference", "user:profile")

    /** Header values Claude Code sends alongside the bearer token. */
    const val ANTHROPIC_VERSION = "2023-06-01"
    const val ANTHROPIC_BETA = "oauth-2025-04-20"

    /**
     * Endpoint queried for live usage. The response is parsed leniently
     * (see UsageResponseParser) so minor shape changes won't crash the app.
     * If/when the official path differs, change it here.
     */
    const val USAGE_URL = "https://api.anthropic.com/api/oauth/usage"

    /** Profile endpoint, used to show the signed-in account. */
    const val PROFILE_URL = "https://api.anthropic.com/api/oauth/profile"

    fun scopeParam(): String = SCOPES.joinToString(" ")
}
