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
 * A media-player clock: "3:29", or "1:04:07" once a track runs past an hour.
 *
 * Separate from [formatDurationCompact] rather than a mode of it, because they answer different
 * questions. "How long did I study" wants a rounded, readable "1h 30m"; "where am I in this track"
 * wants seconds, zero-padded, and the same width from one second to the next so the figure does not
 * jitter under a progress line. Negative input clamps to zero for the same reason its neighbour
 * does (Issue #248) — a media session is free to report nonsense, and this is where it surfaces.
 */
fun formatClockTime(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
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
