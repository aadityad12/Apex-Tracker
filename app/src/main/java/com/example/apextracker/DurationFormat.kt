package com.example.apextracker

import java.util.Locale

/** Formats a duration as "Xh Ym" (or just "Ym" under an hour). Shared by Study, Screen Time, and Overview. */
fun formatDurationCompact(millis: Long): String {
    // Kotlin's % on a negative dividend yields a negative remainder, so an unclamped negative
    // input renders garbage like "-2h -5m" instead of failing loudly or reading as zero (Issue
    // #248) — this is the widest-used duration formatter in the app, so any future bug upstream
    // that produces a negative duration (a clock-backward jump, a bad subtraction) surfaces here
    // as a coherent "0m" rather than a confusing negative string. Mirrors durationAxisLabels'
    // existing safeMax guard.
    val totalMinutes = millis.coerceAtLeast(0L) / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
    } else {
        String.format(Locale.getDefault(), "%dm", minutes)
    }
}

/**
 * Labels for a duration chart's y-axis gridlines (max, half, zero — top to bottom). Picks one unit
 * for all three from the magnitude of [maxMillis], so a chart whose busiest day is under a minute
 * doesn't render three identical "0m" labels (Issue #97). Values round rather than truncate, so
 * the mid gridline reads as the real halfway point.
 */
fun durationAxisLabels(maxMillis: Long): List<String> {
    val safeMax = maxMillis.coerceAtLeast(0L)
    val values = listOf(safeMax, safeMax / 2, 0L)
    return when {
        safeMax >= 3_600_000L -> values.map { formatDurationCompact(it) }
        safeMax >= 60_000L -> values.map { String.format(Locale.getDefault(), "%dm", Math.round(it / 60_000.0)) }
        else -> values.map { String.format(Locale.getDefault(), "%ds", Math.round(it / 1_000.0)) }
    }
}

/**
 * The smallest "round" minute value strictly greater than [peakMinutes] — the top of a duration
 * chart's y-axis.
 *
 * Strictly greater, so there is always headroom: an axis whose maximum equals the tallest bar puts
 * that bar (and any target line at the same value) flush against the top edge, where it reads as a
 * border rather than data. And *round*, because scaling the peak by a factor produces maxima like
 * "1h 6m", which tells the reader nothing — the step widens with magnitude so the label stays
 * readable at any range.
 */
fun niceAxisMaxMinutes(peakMinutes: Int): Int {
    val peak = peakMinutes.coerceAtLeast(0)
    val step = when {
        peak < 60 -> 10
        peak < 240 -> 30
        else -> 60
    }
    return (peak / step + 1) * step
}
