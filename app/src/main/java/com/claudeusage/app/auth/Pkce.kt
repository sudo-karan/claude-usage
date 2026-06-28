package com.claudeusage.app.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/** PKCE (RFC 7636) verifier/challenge pair for the authorization-code flow. */
data class Pkce(val verifier: String, val challenge: String, val state: String) {
    companion object {
        private const val FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

        fun generate(): Pkce {
            val random = SecureRandom()
            val verifierBytes = ByteArray(32).also { random.nextBytes(it) }
            val verifier = Base64.encodeToString(verifierBytes, FLAGS)

            val digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.toByteArray(Charsets.US_ASCII))
            val challenge = Base64.encodeToString(digest, FLAGS)

            val stateBytes = ByteArray(16).also { random.nextBytes(it) }
            val state = Base64.encodeToString(stateBytes, FLAGS)

            return Pkce(verifier = verifier, challenge = challenge, state = state)
        }
    }
}
