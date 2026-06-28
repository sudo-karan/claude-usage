package com.claudeusage.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.claudeusage.app.Graph
import com.claudeusage.app.MainActivity
import com.claudeusage.app.R
import com.claudeusage.app.data.SampleUsage
import com.claudeusage.app.data.model.UsageMeter
import com.claudeusage.app.data.model.UsageSnapshot
import com.claudeusage.app.ping.PingClaude
import com.claudeusage.app.ui.theme.AccentExtra
import com.claudeusage.app.ui.theme.AccentSession
import com.claudeusage.app.ui.theme.AccentSonnet
import com.claudeusage.app.ui.theme.AccentWeekly
import com.claudeusage.app.ui.theme.Coral
import com.claudeusage.app.ui.theme.OnInk
import com.claudeusage.app.ui.theme.OnInkMuted
import com.claudeusage.app.ui.theme.SuccessGreen
import com.claudeusage.app.ui.theme.TrackNeutral
import com.claudeusage.app.util.TimeFormat

class UsageWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = Graph.cache(context).load() ?: SampleUsage.snapshot()
        provideContent { WidgetBody(snapshot) }
    }
}

private fun accent(meterId: String) = when (meterId) {
    UsageSnapshot.SESSION -> AccentSession
    UsageSnapshot.WEEKLY -> AccentWeekly
    UsageSnapshot.SONNET_WEEKLY -> AccentSonnet
    else -> AccentExtra
}

@androidx.compose.runtime.Composable
private fun WidgetBody(snapshot: UsageSnapshot) {
    val now = System.currentTimeMillis()
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .cornerRadius(28.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_logo),
                contentDescription = null,
                modifier = GlanceModifier.size(18.dp),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                "Claude Usage",
                style = TextStyle(color = ColorProvider(OnInk), fontSize = 14.sp, fontWeight = FontWeight.Medium),
            )
            Spacer(GlanceModifier.width(8.dp))
            SourcePill(snapshot.isLive)
            Spacer(GlanceModifier.defaultWeight())
            RefreshButton()
            Spacer(GlanceModifier.width(6.dp))
            PingPill()
        }

        Spacer(GlanceModifier.height(12.dp))

        snapshot.meters.take(3).forEachIndexed { index, meter ->
            MeterBlock(meter, now)
            if (index != snapshot.meters.take(3).lastIndex) Spacer(GlanceModifier.height(10.dp))
        }
    }
}

@androidx.compose.runtime.Composable
private fun MeterBlock(meter: UsageMeter, now: Long) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                meter.label,
                style = TextStyle(color = ColorProvider(OnInk), fontSize = 13.sp),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                "${meter.usedPercent}%",
                style = TextStyle(color = ColorProvider(accent(meter.id)), fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                TimeFormat.remaining(meter.resetAtEpochMs, now),
                style = TextStyle(color = ColorProvider(OnInkMuted), fontSize = 12.sp),
            )
        }
        Spacer(GlanceModifier.height(5.dp))
        LinearProgressIndicator(
            progress = meter.usedFraction.coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp),
            color = ColorProvider(accent(meter.id)),
            backgroundColor = ColorProvider(TrackNeutral),
        )
    }
}

@androidx.compose.runtime.Composable
private fun PingPill() {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(Coral))
            .cornerRadius(50.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable(actionRunCallback<PingActionCallback>()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Ping",
            style = TextStyle(color = ColorProvider(OnInk), fontSize = 12.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@androidx.compose.runtime.Composable
private fun SourcePill(isLive: Boolean) {
    val accent = if (isLive) SuccessGreen else OnInkMuted
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(accent.copy(alpha = 0.18f)))
            .cornerRadius(50.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (isLive) "LIVE" else "SAMPLE",
            style = TextStyle(color = ColorProvider(accent), fontSize = 10.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@androidx.compose.runtime.Composable
private fun RefreshButton() {
    Box(
        modifier = GlanceModifier
            .cornerRadius(50.dp)
            .padding(6.dp)
            .clickable(actionRunCallback<RefreshActionCallback>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "Refresh",
            modifier = GlanceModifier.size(16.dp),
        )
    }
}

/** Background action for the widget's Ping pill. */
class PingActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        PingClaude.ping(context)
    }
}

/** Refreshes usage from the saved session, then repaints all widgets. */
class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runCatching { Graph.repository(context).refresh() }
        runCatching { UsageWidget().updateAll(context) }
    }
}
