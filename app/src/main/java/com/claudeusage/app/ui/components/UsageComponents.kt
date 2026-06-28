package com.claudeusage.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claudeusage.app.data.model.UsageMeter
import com.claudeusage.app.ui.theme.DangerRed
import com.claudeusage.app.ui.theme.OnInkMuted
import com.claudeusage.app.ui.theme.SuccessGreen
import com.claudeusage.app.ui.theme.TrackNeutral
import com.claudeusage.app.ui.theme.WarnAmber
import com.claudeusage.app.ui.theme.accentFor
import com.claudeusage.app.util.TimeFormat

/** One labelled meter: title, percent, reset countdown and a rounded bar. */
@Composable
fun MeterRow(meter: UsageMeter, now: Long, modifier: Modifier = Modifier) {
    val accent = accentFor(meter.id)
    val fraction = meter.usedFraction.coerceIn(0f, 1f)
    val percentColor = when {
        fraction >= 0.9f -> DangerRed
        fraction >= 0.75f -> WarnAmber
        else -> accent
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = meter.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${meter.usedPercent}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = percentColor,
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = OnInkMuted,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = TimeFormat.remaining(meter.resetAtEpochMs, now),
                style = MaterialTheme.typography.bodyMedium,
                color = OnInkMuted,
            )
        }
        Spacer(Modifier.height(10.dp))
        ProgressTrack(fraction = fraction, accent = accent)
    }
}

/** Rounded progress bar: matte neutral track + solid accent fill, animated. */
@Composable
fun ProgressTrack(fraction: Float, accent: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(TrackNeutral),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceAtLeast(if (fraction > 0f) 0.04f else 0f))
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accent),
        )
    }
}

/** Subtle "LIVE" / "SAMPLE" pill. */
@Composable
fun SourceBadge(isLive: Boolean, modifier: Modifier = Modifier) {
    val color = if (isLive) SuccessGreen else OnInkMuted
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
        Text(
            text = if (isLive) "LIVE" else "SAMPLE",
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
