package com.claudeusage.app.data

import com.claudeusage.app.data.model.UsageMeter
import com.claudeusage.app.data.model.UsageSnapshot

/**
 * Bundled placeholder reading. Shown before the first live fetch, in Compose
 * previews, and as a graceful fallback when a live call fails. Numbers mirror
 * the design reference so the UI always looks complete.
 */
object SampleUsage {
    fun snapshot(now: Long = System.currentTimeMillis()): UsageSnapshot {
        val hour = 60 * 60 * 1000L
        return UsageSnapshot(
            meters = listOf(
                UsageMeter(UsageSnapshot.SESSION, "Session (5h)", 0.16f, now + (4 * hour + 27 * 60 * 1000L)),
                UsageMeter(UsageSnapshot.WEEKLY, "Weekly", 0.11f, now + (16 * hour + 57 * 60 * 1000L)),
                UsageMeter(UsageSnapshot.SONNET_WEEKLY, "Sonnet Weekly", 0.01f, now + (16 * hour + 57 * 60 * 1000L)),
            ),
            capturedAtEpochMs = now,
            isLive = false,
            planName = "Sample data",
        )
    }
}
