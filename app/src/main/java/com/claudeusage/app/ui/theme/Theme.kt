package com.claudeusage.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.claudeusage.app.data.model.UsageSnapshot

private val ClaudeDarkColors = darkColorScheme(
    primary = Coral,
    onPrimary = Color(0xFF2A130A),
    secondary = AccentWeekly,
    tertiary = AccentSonnet,
    background = InkBackground,
    onBackground = OnInk,
    surface = InkSurface,
    onSurface = OnInk,
    surfaceVariant = InkSurfaceVariant,
    onSurfaceVariant = OnInkMuted,
    outline = OutlineSubtle,
    error = DangerRed,
)

@Composable
fun ClaudeUsageTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClaudeDarkColors,
        typography = Typography,
        content = content,
    )
}

/** Stable accent colour per meter so the dashboard and widget always agree. */
fun accentFor(meterId: String): Color = when (meterId) {
    UsageSnapshot.SESSION -> AccentSession
    UsageSnapshot.WEEKLY -> AccentWeekly
    UsageSnapshot.SONNET_WEEKLY -> AccentSonnet
    else -> AccentExtra
}
