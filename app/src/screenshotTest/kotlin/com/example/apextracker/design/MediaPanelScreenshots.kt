package com.example.apextracker.design

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.apextracker.media.NowPlaying
import com.example.apextracker.media.StudyMediaPanel
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.ApexTrackerTheme

/**
 * Reference renders of the focus surface's now-playing panel, in all three of the states it can be
 * in plus the ambient variant.
 *
 * These exist because the panel's real inputs come from another app's media session, which no test
 * on this side can conjure — a live session needs a real device with a real player. What a static
 * frame *can* prove is everything that has ever actually gone wrong with a surface in this repo:
 * that both themes are correct, that cover art is dropped in ambient mode rather than glowing, that
 * a long title ellipsizes instead of pushing the transport row off the edge, and that the elapsed
 * figures sit in the tabular mono.
 *
 * `positionUpdatedAtRealtime = 0` in the fixtures on purpose: it stops [NowPlaying.positionAt]
 * extrapolating from the render clock, which would make the baseline change on every run.
 */
private fun fakeArtwork(): Bitmap {
    val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).drawColor(AndroidColor.rgb(96, 112, 128))
    return bitmap
}

private fun playingSession(
    title: String = "Nights",
    artist: String = "Frank Ocean",
    isPlaying: Boolean = true,
) = NowPlaying(
    packageName = "com.spotify.music",
    title = title,
    artist = artist,
    artwork = fakeArtwork(),
    durationMillis = 307_000,
    isPlaying = isPlaying,
    reportedPositionMillis = 209_000,
    positionUpdatedAtRealtime = 0,
    playbackSpeed = if (isPlaying) 1f else 0f
)

@Composable
private fun MediaPanelGallery(ambient: Boolean = false) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.padding(vertical = ApexSpacing.l),
            verticalArrangement = Arrangement.spacedBy(ApexSpacing.xl),
        ) {
            ApexSectionHeader("Playing")
            StudyMediaPanel(
                nowPlaying = playingSession(),
                hasAccess = true,
                ambient = ambient,
                onPlayPause = {}, onNext = {}, onPrevious = {}, onConnect = {}
            )
            ApexSectionHeader("Paused, long title")
            StudyMediaPanel(
                nowPlaying = playingSession(
                    title = "A Title Long Enough That It Has To Be Cut Off Somewhere",
                    artist = "An Artist With A Similarly Unreasonable Name",
                    isPlaying = false
                ),
                hasAccess = true,
                ambient = ambient,
                onPlayPause = {}, onNext = {}, onPrevious = {}, onConnect = {}
            )
            ApexSectionHeader("Connected, nothing playing")
            StudyMediaPanel(
                nowPlaying = null,
                hasAccess = true,
                ambient = ambient,
                onPlayPause = {}, onNext = {}, onPrevious = {}, onConnect = {}
            )
            ApexSectionHeader("Not connected")
            StudyMediaPanel(
                nowPlaying = null,
                hasAccess = false,
                ambient = ambient,
                onPlayPause = {}, onNext = {}, onPrevious = {}, onConnect = {}
            )
        }
    }
}

@PreviewTest
@Preview(name = "Media panel · dark", showBackground = true)
@Composable
private fun MediaPanelDark() {
    ApexTrackerTheme(darkTheme = true) { MediaPanelGallery() }
}

@PreviewTest
@Preview(name = "Media panel · light", showBackground = true)
@Composable
private fun MediaPanelLight() {
    ApexTrackerTheme(darkTheme = false) { MediaPanelGallery() }
}

@PreviewTest
@Preview(name = "Media panel · ambient", showBackground = true)
@Composable
private fun MediaPanelAmbient() {
    ApexTrackerTheme(darkTheme = true) { MediaPanelGallery(ambient = true) }
}

@PreviewTest
@Preview(name = "Media panel · dark 200%", showBackground = true, fontScale = 2.0f)
@Composable
private fun MediaPanelLargeText() {
    ApexTrackerTheme(darkTheme = true) { MediaPanelGallery() }
}
