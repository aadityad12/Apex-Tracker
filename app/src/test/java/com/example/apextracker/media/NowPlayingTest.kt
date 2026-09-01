package com.example.apextracker.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NowPlayingTest {

    private fun candidate(pkg: String, playing: Boolean) = MediaCandidate(pkg, playing)

    @Test
    fun `nothing active means nothing to follow`() {
        assertNull(pickPreferredSession(emptyList()))
    }

    @Test
    fun `a playing session wins over a more recent paused one`() {
        // getActiveSessions is ordered most-recently-active first, so the paused browser tab the
        // user touched last sits ahead of the music that is actually audible.
        val picked = pickPreferredSession(
            listOf(
                candidate("com.android.chrome", playing = false),
                candidate("com.spotify.music", playing = true)
            )
        )
        assertEquals("com.spotify.music", picked?.packageName)
    }

    @Test
    fun `with nothing playing the most recent session is offered`() {
        val picked = pickPreferredSession(
            listOf(
                candidate("com.spotify.music", playing = false),
                candidate("com.google.android.youtube", playing = false)
            )
        )
        assertEquals("com.spotify.music", picked?.packageName)
    }

    @Test
    fun `position advances with wall time while playing`() {
        val position = estimatePositionMillis(
            reportedPositionMillis = 30_000,
            positionUpdatedAtRealtime = 1_000_000,
            playbackSpeed = 1f,
            nowRealtimeMillis = 1_005_000,
            durationMillis = 300_000
        )
        assertEquals(35_000, position)
    }

    @Test
    fun `a paused session does not advance`() {
        // Speed 0 is how a paused player reports itself; without this the bar would keep crawling.
        val position = estimatePositionMillis(
            reportedPositionMillis = 30_000,
            positionUpdatedAtRealtime = 1_000_000,
            playbackSpeed = 0f,
            nowRealtimeMillis = 1_060_000,
            durationMillis = 300_000
        )
        assertEquals(30_000, position)
    }

    @Test
    fun `playback speed scales the extrapolation`() {
        val position = estimatePositionMillis(
            reportedPositionMillis = 0,
            positionUpdatedAtRealtime = 1_000,
            playbackSpeed = 1.5f,
            nowRealtimeMillis = 11_000,
            durationMillis = 300_000
        )
        assertEquals(15_000, position)
    }

    @Test
    fun `a session that published no update time is taken at its word`() {
        // Extrapolating from 0 would add the whole time since boot.
        val position = estimatePositionMillis(
            reportedPositionMillis = 42_000,
            positionUpdatedAtRealtime = 0,
            playbackSpeed = 1f,
            nowRealtimeMillis = 9_000_000,
            durationMillis = 300_000
        )
        assertEquals(42_000, position)
    }

    @Test
    fun `position never runs past the duration`() {
        val position = estimatePositionMillis(
            reportedPositionMillis = 299_000,
            positionUpdatedAtRealtime = 1_000,
            playbackSpeed = 1f,
            nowRealtimeMillis = 61_000,
            durationMillis = 300_000
        )
        assertEquals(300_000, position)
    }

    @Test
    fun `an unknown duration leaves the position unclamped`() {
        // A live stream publishes no duration; clamping to 0 would freeze the elapsed figure.
        val position = estimatePositionMillis(
            reportedPositionMillis = 10_000,
            positionUpdatedAtRealtime = 1_000,
            playbackSpeed = 1f,
            nowRealtimeMillis = 6_000,
            durationMillis = 0
        )
        assertEquals(15_000, position)
    }
}
