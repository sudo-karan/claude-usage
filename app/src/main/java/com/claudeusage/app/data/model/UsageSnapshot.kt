package com.claudeusage.app.data.model

import kotlinx.serialization.Serializable

/**
 * A single rate-limit window (e.g. the rolling 5-hour session, the weekly cap,
 * or the weekly Sonnet cap) as shown on the dashboard and the widget.
 *
 * [usedFraction] is clamped to 0f..1f. [resetAtEpochMs] is the wall-clock instant
 * the window rolls over; a value <= 0 means "unknown / not provided".
 */
@Serializable
data class UsageMeter(
    val id: String,
    val label: String,
    val usedFraction: Float,
    val resetAtEpochMs: Long,
) {
    val usedPercent: Int get() = (usedFraction.coerceIn(0f, 1f) * 100f).toInt()
}

/** A full reading of every meter at a point in time. */
@Serializable
data class UsageSnapshot(
    val meters: List<UsageMeter>,
    val capturedAtEpochMs: Long,
    /** true when sourced from the live API, false for the bundled sample. */
    val isLive: Boolean,
    val planName: String? = null,
) {
    fun meter(id: String): UsageMeter? = meters.firstOrNull { it.id == id }

    companion object {
        const val SESSION = "session"
        const val WEEKLY = "weekly"
        const val SONNET_WEEKLY = "sonnet_weekly"
    }
}
