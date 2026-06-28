package com.claudeusage.app.notify

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.claudeusage.app.Graph
import com.claudeusage.app.data.model.UsageSnapshot
import com.claudeusage.app.widget.UsageWidget

/**
 * Refreshes usage in the background, repaints the widget, fires reset alerts for
 * any window that rolled over since we last looked, and chains the next
 * just-in-time wake-up.
 */
class RefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = Graph.repository(applicationContext)
        val cache = Graph.cache(applicationContext)

        val outcome = runCatching { repo.refresh() }.getOrNull() ?: return Result.retry()
        val snapshot = outcome.snapshot

        notifyResets(snapshot, cache)
        runCatching { UsageWidget().updateAll(applicationContext) }
        UsageWorkScheduler.scheduleNextReset(applicationContext, snapshot)
        return Result.success()
    }

    private fun notifyResets(snapshot: UsageSnapshot, cache: com.claudeusage.app.data.UsageCache) {
        if (!snapshot.isLive) return // never alert on sample data
        for (meter in snapshot.meters) {
            val now = meter.usedFraction.coerceIn(0f, 1f)
            val prev = cache.lastSeenFraction(meter.id)
            cache.setLastSeenFraction(meter.id, now)

            // Detect a *real* reset: usage was meaningfully consumed and has now
            // dropped back toward zero. We deliberately key off the usage fraction
            // rather than the reset timestamp — the captured reset time can drift
            // forward while usage is still high (which previously caused false
            // "limit reset" alerts at 100%).
            if (prev < 0f) continue // first observation for this meter
            val didReset = prev >= 0.20f && now <= 0.10f
            if (didReset) {
                Notifier.notifyReset(applicationContext, meter)
            }
        }
    }
}
