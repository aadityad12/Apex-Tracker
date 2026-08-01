package com.example.apextracker.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.apextracker.ui.design.ApexFlipClock
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.ApexTrackerTheme
import com.example.apextracker.ui.design.FlipClockFitToWidth

/**
 * Reference renders of the study timer's split-flap clock.
 *
 * Only *resting* states are captured. A preview renders a single frame, so a mid-flip rotation would
 * be non-deterministic and the baseline flaky — the flap itself is verified on device via the style
 * plate, not here. What these guard is the geometry that a static frame can prove: the card tone in
 * each theme, the seam, the field gaps, the two-vs-three field layouts, and that six cards still fit
 * once FlipClockFitToWidth has scaled them at a 200% font scale.
 *
 * If a @Preview parameter changes, delete the orphaned reference file — baseline filenames embed a
 * hash of the preview config.
 */
@Composable
private fun FlipClockGallery() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.padding(ApexSpacing.l),
            verticalArrangement = Arrangement.spacedBy(ApexSpacing.xl),
        ) {
            ApexSectionHeader("Paused")
            FlipClockFitToWidth { ApexFlipClock(seconds = { 1477L }, active = false) }
            ApexSectionHeader("Running")
            FlipClockFitToWidth { ApexFlipClock(seconds = { 1477L }, active = true) }
            ApexSectionHeader("Past an hour")
            FlipClockFitToWidth { ApexFlipClock(seconds = { 8096L }, active = true) }
            ApexSectionHeader("Medium")
            FlipClockFitToWidth {
                ApexFlipClock(seconds = { 8096L }, style = ApexNumerals.large, active = false)
            }
        }
    }
}

@PreviewTest
@Preview(name = "Flip clock · dark", showBackground = true)
@Composable
private fun FlipClockDark() {
    ApexTrackerTheme(darkTheme = true) { FlipClockGallery() }
}

@PreviewTest
@Preview(name = "Flip clock · light", showBackground = true)
@Composable
private fun FlipClockLight() {
    ApexTrackerTheme(darkTheme = false) { FlipClockGallery() }
}

@PreviewTest
@Preview(name = "Flip clock · dark 200% font", showBackground = true, fontScale = 2.0f)
@Composable
private fun FlipClockLargeFont() {
    ApexTrackerTheme(darkTheme = true) { FlipClockGallery() }
}
