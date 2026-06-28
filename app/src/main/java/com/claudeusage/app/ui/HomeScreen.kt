package com.claudeusage.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudeusage.app.R
import com.claudeusage.app.ping.PingClaude
import com.claudeusage.app.ui.components.MeterRow
import com.claudeusage.app.ui.components.SourceBadge
import com.claudeusage.app.ui.theme.Coral
import com.claudeusage.app.ui.theme.InkSurface
import com.claudeusage.app.ui.theme.InkSurfaceVariant
import com.claudeusage.app.ui.theme.OnInk
import com.claudeusage.app.ui.theme.OnInkMuted
import com.claudeusage.app.ui.theme.OutlineSubtle
import com.claudeusage.app.util.TimeFormat
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: UsageViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Tick once a minute so the reset countdowns stay current.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            HeaderBar(
                accountLabel = state.accountLabel,
                isLoggedIn = state.isLoggedIn,
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                onLogout = viewModel::logout,
            )

            state.message?.let { msg ->
                MessageBar(message = msg, onDismiss = viewModel::dismissMessage)
            }

            if (!state.isLoggedIn) {
                ConnectBanner(onSignIn = viewModel::openAuthSheet)
            }

            UsagePanel(state = state, now = now)

            PingCard(onPing = { PingClaude.ping(context) })

            FooterRow(capturedAt = state.snapshot.capturedAtEpochMs, now = now)
        }
    }

    if (state.authInProgress) {
        AuthSheet(
            onDismiss = viewModel::cancelLogin,
            onOpenAuthorize = { CustomTab.open(context, viewModel.beginLogin()) },
            onSubmitCode = viewModel::submitAuthCode,
        )
    }
}

@Composable
private fun HeaderBar(
    accountLabel: String?,
    isLoggedIn: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_widget_logo),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Claude Usage",
                style = MaterialTheme.typography.headlineMedium,
                color = OnInk,
            )
            Text(
                accountLabel ?: if (isLoggedIn) "Signed in" else "Not signed in",
                style = MaterialTheme.typography.bodyMedium,
                color = OnInkMuted,
            )
        }
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Coral,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
        } else {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = OnInk)
            }
        }
        if (isLoggedIn) {
            IconButton(onClick = onLogout) {
                Icon(Icons.Rounded.Logout, contentDescription = "Sign out", tint = OnInkMuted)
            }
        }
    }
}

@Composable
private fun PremiumCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = InkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
    ) {
        Box(Modifier.padding(22.dp)) { content() }
    }
}

@Composable
private fun UsagePanel(state: UsageUiState, now: Long) {
    PremiumCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "USAGE",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnInkMuted,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f),
                )
                SourceBadge(isLive = state.snapshot.isLive)
            }
            Spacer(Modifier.height(20.dp))
            val meters = state.snapshot.meters
            meters.forEachIndexed { index, meter ->
                MeterRow(meter = meter, now = now)
                if (index != meters.lastIndex) Spacer(Modifier.height(22.dp))
            }
        }
    }
}

@Composable
private fun ConnectBanner(onSignIn: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = InkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, Coral.copy(alpha = 0.4f)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Connect to see live usage",
                style = MaterialTheme.typography.titleLarge,
                color = OnInk,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Sign in with your Claude account to replace the sample numbers below with your real limits.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnInkMuted,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color(0xFF2A130A)),
            ) {
                Text("Sign in", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PingCard(onPing: () -> Unit) {
    PremiumCard {
        Column {
            Text(
                "Ping Claude",
                style = MaterialTheme.typography.titleLarge,
                color = OnInk,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Opens a new chat and says “Hi” to kick off a fresh 5-hour session.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnInkMuted,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onPing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color(0xFF2A130A)),
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ping Claude", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MessageBar(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = InkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = OnInk,
                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = OnInkMuted)
            }
        }
    }
}

@Composable
private fun FooterRow(capturedAt: Long, now: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(OnInkMuted),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            TimeFormat.updatedLabel(capturedAt, now),
            style = MaterialTheme.typography.bodyMedium,
            color = OnInkMuted,
        )
    }
}
