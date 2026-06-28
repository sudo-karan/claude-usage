package com.claudeusage.app.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Carries an OAuth redirect (full callback URI) from [OAuthRedirectActivity] to
 * whichever screen is waiting to finish the login. replay = 1 so a redirect that
 * arrives slightly before the collector subscribes is not lost.
 */
object AuthRedirectBus {
    private val _redirects = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val redirects: SharedFlow<String> = _redirects.asSharedFlow()

    fun publish(uri: String) {
        _redirects.tryEmit(uri)
    }

    fun clear() {
        _redirects.resetReplayCache()
    }
}
