package com.claudeusage.app.ui

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
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

    init {
        refresh()
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

    /** Called when the WebView login screen returns. */
    fun onReturnedFromLogin() {
        val loggedIn = repo.isLoggedIn
        _state.update {
            it.copy(
                isLoggedIn = loggedIn,
                accountLabel = repo.accountLabel,
                message = if (loggedIn) "Signed in. Loading your usage…" else null,
            )
        }
        if (loggedIn) UsageWorkScheduler.ensurePeriodic(getApplication())
        refresh()
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
}
