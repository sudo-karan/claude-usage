package com.claudeusage.app.util

/** Human-friendly time helpers for reset countdowns and "last updated" labels. */
object TimeFormat {

    /** e.g. "4h 27m", "16h 57m", "2d 3h", "12m". Empty windows render as "—". */
    fun remaining(resetAtEpochMs: Long, now: Long = System.currentTimeMillis()): String {
        if (resetAtEpochMs <= 0L) return "—"
        var secs = (resetAtEpochMs - now) / 1000L
        if (secs <= 0L) return "now"
        val days = secs / 86_400; secs %= 86_400
        val hours = secs / 3_600; secs %= 3_600
        val mins = secs / 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }

    fun updatedLabel(capturedAtEpochMs: Long, now: Long = System.currentTimeMillis()): String {
        val mins = (now - capturedAtEpochMs) / 60_000L
        return when {
            mins <= 0L -> "Updated just now"
            mins == 1L -> "Updated 1 min ago"
            mins < 60L -> "Updated $mins min ago"
            else -> {
                val hours = mins / 60L
                if (hours == 1L) "Updated 1 hr ago" else "Updated $hours hrs ago"
            }
        }
    }
}
