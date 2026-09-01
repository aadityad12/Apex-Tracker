package com.example.apextracker.media

import android.graphics.Bitmap

/**
 * What is playing on the device right now, as much of it as the active media session chooses to
 * publish. Every field is best-effort: a player is free to report no artist, no artwork, or no
 * duration, and the panel has to read sensibly when it does.
 */
data class NowPlaying(
    val packageName: String,
    val title: String,
    val artist: String,
    val artwork: Bitmap?,
    val durationMillis: Long,
    val isPlaying: Boolean,
    /** Position as the session last reported it — a snapshot, not a live value. See [positionAt]. */
    val reportedPositionMillis: Long,
    /** `SystemClock.elapsedRealtime()` when that snapshot was taken; 0 if the session gave none. */
    val positionUpdatedAtRealtime: Long,
    val playbackSpeed: Float,
) {
    /** The position to display at [elapsedRealtimeMillis]. See [estimatePositionMillis]. */
    fun positionAt(elapsedRealtimeMillis: Long): Long = estimatePositionMillis(
        reportedPositionMillis = reportedPositionMillis,
        positionUpdatedAtRealtime = positionUpdatedAtRealtime,
        playbackSpeed = playbackSpeed,
        nowRealtimeMillis = elapsedRealtimeMillis,
        durationMillis = durationMillis
    )
}

/** One active session, reduced to what choosing between several needs. */
data class MediaCandidate(val packageName: String, val isPlaying: Boolean)

/**
 * Which of the device's active media sessions the panel should follow.
 *
 * A phone routinely has several: a paused podcast, a browser tab that played a video once, and the
 * music the user actually cares about. Anything currently playing wins; failing that the list is
 * already ordered most-recently-active-first by `MediaSessionManager`, so the head of it is the
 * session the user last touched — which is the right thing to offer a play button for.
 */
fun pickPreferredSession(candidates: List<MediaCandidate>): MediaCandidate? =
    candidates.firstOrNull { it.isPlaying } ?: candidates.firstOrNull()

/**
 * Where playback has reached, extrapolated from the session's last report.
 *
 * `PlaybackState.getPosition()` is a snapshot taken at `getLastPositionUpdateTime()`, not a value
 * that advances — a player publishes it once when playback starts and then stays quiet for the rest
 * of the track. Reading it directly gives a progress bar frozen at wherever the song was when it
 * began, which looks exactly like a bug. The elapsed wall time since that snapshot, scaled by the
 * playback speed, is the missing part.
 *
 * A speed of zero (paused, buffering) extrapolates nothing, and a session that published no update
 * time at all is taken at its word rather than extrapolated from an epoch.
 */
fun estimatePositionMillis(
    reportedPositionMillis: Long,
    positionUpdatedAtRealtime: Long,
    playbackSpeed: Float,
    nowRealtimeMillis: Long,
    durationMillis: Long
): Long {
    val base = reportedPositionMillis.coerceAtLeast(0L)
    val elapsed = if (positionUpdatedAtRealtime <= 0L || playbackSpeed <= 0f) {
        0L
    } else {
        ((nowRealtimeMillis - positionUpdatedAtRealtime).coerceAtLeast(0L) * playbackSpeed).toLong()
    }
    val position = base + elapsed
    return if (durationMillis > 0L) position.coerceAtMost(durationMillis) else position
}
