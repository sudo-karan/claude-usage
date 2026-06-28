package com.claudeusage.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import com.claudeusage.app.Graph
import com.claudeusage.app.data.model.UsageSnapshot
import com.claudeusage.app.notify.UsageWorkScheduler
import com.claudeusage.app.widget.UsageWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsageUiState(
    val snapshot: UsageSnapshot,
    val isRefreshing: Boolean = false,
    val isLoggedIn: Boolean = false,
    val accountLabel: String? = null,
    val message: String? = null,
    val authInProgress: Boolean = false,
)

class UsageViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Graph.repository(app)

    private val _state = MutableStateFlow(
        UsageUiState(
            snapshot = repo.cachedOrSample(),
            isLoggedIn = repo.isLoggedIn,
            accountLabel = repo.accountLabel,
        )
    )
    val state: StateFlow<UsageUiState> = _state.asStateFlow()

    /** true when we kicked off auth with the custom-scheme redirect. */
    private var usedAppRedirect = false

    init {
        refresh()
        observeRedirects()
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val outcome = repo.refresh()
            _state.update {
                it.copy(
                    snapshot = outcome.snapshot,
                    isRefreshing = false,
                    isLoggedIn = repo.isLoggedIn,
                    accountLabel = repo.accountLabel,
                    message = outcome.errorMessage,
                )
            }
            UsageWorkScheduler.scheduleNextReset(getApplication(), outcome.snapshot)
            runCatching { UsageWidget().updateAll(getApplication()) }
        }
    }

    /** Shows the sign-in sheet without launching the browser yet. */
    fun openAuthSheet() {
        _state.update { it.copy(authInProgress = true, message = null) }
    }

    /** Builds the authorize URL and flips into "waiting for code" mode. */
    fun beginLogin(useAppRedirect: Boolean = false): Uri {
        usedAppRedirect = useAppRedirect
        _state.update { it.copy(authInProgress = true, message = null) }
        return repo.auth.startAuthorization(useAppRedirect)
    }

    fun cancelLogin() {
        _state.update { it.copy(authInProgress = false) }
    }

    /** Completes login from a pasted code or a full redirect URI. */
    fun submitAuthCode(raw: String) {
        if (raw.isBlank()) return
        viewModelScope.launch {
            val result = repo.auth.completeAuthorization(raw, usedAppRedirect)
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            authInProgress = false,
                            isLoggedIn = true,
                            accountLabel = repo.accountLabel,
                            message = "Signed in. Fetching your usage…",
                        )
                    }
                    UsageWorkScheduler.ensurePeriodic(getApplication())
                    refresh()
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(message = err.message ?: "Sign-in failed. Please try again.")
                    }
                },
            )
        }
    }

    fun logout() {
        repo.logout()
        _state.update {
            it.copy(
                isLoggedIn = false,
                accountLabel = null,
                snapshot = repo.cachedOrSample(),
                message = "Signed out.",
            )
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun observeRedirects() {
        viewModelScope.launch {
            com.claudeusage.app.auth.AuthRedirectBus.redirects.collect { uri ->
                if (_state.value.authInProgress) {
                    com.claudeusage.app.auth.AuthRedirectBus.clear()
                    submitAuthCode(uri)
                }
            }
        }
    }
}
