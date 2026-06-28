package com.claudeusage.app.data

import com.claudeusage.app.data.model.UsageMeter
import com.claudeusage.app.data.model.UsageSnapshot
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant

/**
 * Lenient parser for the live usage payload. The exact response schema is not a
 * documented public contract, so rather than bind to fixed field names we walk
 * the JSON, find the nested objects that look like rate-limit windows, and map
 * them to our three meters by their key names.
 *
 * Recognised per-window fields (any one of each group):
 *   fraction : "utilization" | "used_fraction" | "percentage" | "used"
 *   reset    : "resets_at" | "reset_at" | "resets_at_epoch_ms" | "reset"
 *
 * Returns an empty list if nothing usable is found, which the repository treats
 * as "no live data" and falls back to the cache / sample.
 */
object UsageResponseParser {

    fun parse(root: JsonObject, now: Long): List<UsageMeter> {
        val out = mutableMapOf<String, UsageMeter>()
        collect(root, now, out)
        // Preserve a stable display order.
        return listOfNotNull(
            out[UsageSnapshot.SESSION],
            out[UsageSnapshot.WEEKLY],
            out[UsageSnapshot.SONNET_WEEKLY],
        ) + out.values.filter {
            it.id != UsageSnapshot.SESSION &&
                it.id != UsageSnapshot.WEEKLY &&
                it.id != UsageSnapshot.SONNET_WEEKLY
        }
    }

    private fun collect(obj: JsonObject, now: Long, out: MutableMap<String, UsageMeter>) {
        for ((key, value) in obj) {
            if (value is JsonObject) {
                val fraction = readFraction(value)
                val reset = readReset(value, now)
                if (fraction != null) {
                    classify(key)?.let { (id, label) ->
                        out.putIfAbsent(id, UsageMeter(id, label, fraction, reset))
                    }
                }
                // Recurse for wrapper objects like {"usage": {...}}.
                collect(value, now, out)
            }
        }
    }

    private fun classify(key: String): Pair<String, String>? {
        val k = key.lowercase()
        return when {
            "sonnet" in k -> UsageSnapshot.SONNET_WEEKLY to "Sonnet Weekly"
            "opus" in k -> "opus_weekly" to "Opus Weekly"
            "5" in k || "five" in k || "hour" in k || "session" in k -> UsageSnapshot.SESSION to "Session (5h)"
            "week" in k || "7" in k || "seven" in k || "day" in k -> UsageSnapshot.WEEKLY to "Weekly"
            else -> null
        }
    }

    private fun readFraction(obj: JsonObject): Float? {
        for (name in listOf("utilization", "used_fraction", "percentage", "percent", "used")) {
            val prim = obj[name] as? JsonPrimitive ?: continue
            val d = prim.doubleOrNull ?: continue
            val f = if (d > 1.0) (d / 100.0) else d
            return f.toFloat().coerceIn(0f, 1f)
        }
        return null
    }

    private fun readReset(obj: JsonObject, now: Long): Long {
        for (name in listOf("resets_at", "reset_at", "resets_at_epoch_ms", "reset", "resets_at_epoch")) {
            val el: JsonElement = obj[name] ?: continue
            val prim = el as? JsonPrimitive ?: continue
            prim.longOrNull?.let { raw ->
                // Heuristic: < 10^12 is seconds, otherwise milliseconds.
                return if (raw < 1_000_000_000_000L) raw * 1000L else raw
            }
            prim.contentOrNull?.let { str ->
                runCatching { return Instant.parse(str).toEpochMilli() }
            }
        }
        return 0L
    }
}
