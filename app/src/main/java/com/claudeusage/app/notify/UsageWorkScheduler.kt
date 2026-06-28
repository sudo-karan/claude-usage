package com.claudeusage.app.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.claudeusage.app.data.model.UsageSnapshot
import java.util.concurrent.TimeUnit

/** Schedules the recurring refresh and the just-in-time reset check. */
object UsageWorkScheduler {
    private const val PERIODIC_NAME = "usage-refresh-periodic"
    private const val RESET_NAME = "usage-reset-check"

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Keeps usage + widget fresh roughly every 30 minutes. Idempotent. */
    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Wakes near the soonest upcoming reset so the alert lands on time. */
    fun scheduleNextReset(context: Context, snapshot: UsageSnapshot, now: Long = System.currentTimeMillis()) {
        val soonest = snapshot.meters
            .map { it.resetAtEpochMs }
            .filter { it > now }
            .minOrNull() ?: return

        // Fire a minute past the reset so the new window is reflected server-side.
        val delay = (soonest - now + 60_000L).coerceAtLeast(60_000L)
        val request = OneTimeWorkRequestBuilder<RefreshWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            RESET_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
