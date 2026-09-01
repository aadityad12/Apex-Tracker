package com.example.apextracker.media

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.apextracker.R
import com.example.apextracker.formatClockTime
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.FrostDim
import kotlinx.coroutines.delay

/**
 * Cover art size. A named constant rather than an `ApexSpacing` value because this is a component
 * dimension, not a gap — the same reasoning `MainActivity`'s raised nav button records.
 */
private val ArtworkSize = 44.dp
private val TransportIconSize = 26.dp
private val ConnectIconSize = 18.dp

/**
 * Transport controls for whatever the device is playing, anchored at the foot of the focus surface.
 *
 * The one thing a studying user reliably leaves this screen for is the music, so it is the one
 * thing the screen earns back by carrying. It stays subordinate to the clock: a single row of
 * metadata, a hairline of progress, three buttons, and no surface of its own — a card here would
 * make it a second centre of gravity on a screen whose whole argument is that it has one.
 *
 * Cover art is the only colour on this screen, and it is allowed for the same reason a category
 * colour is: it is *content*, published by another app, not this app's chrome. It is dropped
 * entirely in ambient mode, where a bright square would defeat the point of dimming the display.
 */
@Composable
fun StudyMediaPanel(
    nowPlaying: NowPlaying?,
    hasAccess: Boolean,
    ambient: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subdued = if (ambient) FrostDim else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ApexDivider()
        Spacer(Modifier.height(ApexSpacing.m))
        when {
            !hasAccess -> ConnectPrompt(subdued = subdued, onConnect = onConnect)
            nowPlaying == null -> Text(
                text = stringResource(R.string.study_media_nothing_playing),
                style = MaterialTheme.typography.bodySmall,
                color = subdued,
                modifier = Modifier.padding(vertical = ApexSpacing.m)
            )
            else -> PlayingPanel(
                session = nowPlaying,
                ambient = ambient,
                subdued = subdued,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious
            )
        }
    }
}

/**
 * The un-granted state. Deliberately one quiet line rather than a card with an explanation: the
 * user did not come to this screen to configure anything, and the full "why does a study tracker
 * want notification access" answer belongs in the system dialog they are about to see and in the
 * privacy policy, not shouted at somebody trying to start a session.
 */
@Composable
private fun ConnectPrompt(subdued: Color, onConnect: () -> Unit) {
    TextButton(onClick = onConnect) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = subdued,
            modifier = Modifier.size(ConnectIconSize)
        )
        Spacer(Modifier.size(ApexSpacing.s))
        Text(
            text = stringResource(R.string.study_media_connect),
            style = MaterialTheme.typography.labelLarge,
            color = subdued
        )
    }
}

@Composable
private fun PlayingPanel(
    session: NowPlaying,
    ambient: Boolean,
    subdued: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val ink = if (ambient) FrostDim else MaterialTheme.colorScheme.onSurface
    // The session reports its position once and then goes quiet, so it is advanced here — and only
    // while something is actually playing, so a paused panel is completely still.
    val positionMillis by produceState(session.positionAt(SystemClock.elapsedRealtime()), session) {
        while (true) {
            value = session.positionAt(SystemClock.elapsedRealtime())
            if (!session.isPlaying) break
            delay(1_000L)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ApexSpacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ApexSpacing.m)
    ) {
        val artwork = session.artwork
        if (artwork != null && !ambient) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = null,   // the title and artist below say the same thing
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(ArtworkSize).clip(RoundedCornerShape(ApexShapes.cell))
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = session.title.ifBlank { stringResource(R.string.study_media_unknown_track) },
                style = MaterialTheme.typography.titleSmall,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (session.artist.isNotBlank()) {
                Text(
                    text = session.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = subdued,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // Null when the player publishes no duration — a live stream, or a session that simply does
    // not say. There is no honest progress bar to draw then, so none is drawn.
    val progress = if (session.durationMillis > 0L) {
        (positionMillis.toFloat() / session.durationMillis).coerceIn(0f, 1f)
    } else null
    if (progress != null) {
        Spacer(Modifier.height(ApexSpacing.m))
        // Bare Canvas, matching StudyGoalMeter and the charts — there is no progress component in
        // this app, and a Material indicator would be the only one of its kind.
        val track = MaterialTheme.colorScheme.outlineVariant
        Canvas(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ApexSpacing.l)
                .height(ApexSpacing.hairline)
        ) {
            drawRect(track)
            drawRect(color = ink, size = Size(size.width * progress, size.height))
        }
        Spacer(Modifier.height(ApexSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ApexSpacing.l),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatClockTime(positionMillis), style = ApexNumerals.small, color = subdued)
            Text(formatClockTime(session.durationMillis), style = ApexNumerals.small, color = subdued)
        }
    }

    Spacer(Modifier.height(ApexSpacing.s))
    Row(
        horizontalArrangement = Arrangement.spacedBy(ApexSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton(
            icon = Icons.Default.SkipPrevious,
            description = stringResource(R.string.cd_media_previous),
            tint = ink,
            onClick = onPrevious
        )
        TransportButton(
            icon = if (session.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            // The label, not just the glyph, carries the state — a play triangle and a pause bar
            // are not distinguishable to a screen reader.
            description = stringResource(
                if (session.isPlaying) R.string.cd_media_pause else R.string.cd_media_play
            ),
            tint = ink,
            onClick = onPlayPause
        )
        TransportButton(
            icon = Icons.Default.SkipNext,
            description = stringResource(R.string.cd_media_next),
            tint = ink,
            onClick = onNext
        )
    }
}

@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
) {
    // IconButton is 48dp regardless of the glyph inside it, which is the touch-target floor.
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(TransportIconSize))
    }
}
