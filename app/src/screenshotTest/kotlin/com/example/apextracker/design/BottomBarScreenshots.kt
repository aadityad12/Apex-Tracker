package com.example.apextracker.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.apextracker.AppBottomBar
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.ApexTrackerTheme

/**
 * Reference renders of the bottom navigation bar.
 *
 * These exist for one reason: the bar used to break its own labels mid-word at large font scales
 * ("Stud y", "Budg et"). That is a layout failure no unit test can see and no single-scale baseline
 * would have caught, because at 1.0f the bar is perfectly fine. The interesting renders are
 * therefore the pair straddling the label-drop threshold — just under it, labels must still fit
 * without wrapping; at and above it, the bar must be cleanly icon-only rather than truncated.
 *
 * The raised centre Dashboard button overhangs the bar's top edge, so the frame carries vertical
 * padding; without it the button is clipped by the image bounds and every diff looks like a change.
 */

@Composable
private fun Bar(currentRoute: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = ApexSpacing.l)) {
        AppBottomBar(currentRoute = currentRoute, onSelectPrimary = {}, onMore = {})
    }
}

@Composable
private fun Framed(dark: Boolean, content: @Composable () -> Unit) {
    ApexTrackerTheme(darkTheme = dark) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) { content() }
    }
}

@PreviewTest
@Preview(name = "bottom-bar-dark", widthDp = 400)
@Composable fun BottomBarDark() = Framed(dark = true) { Bar("dashboard") }

@PreviewTest
@Preview(name = "bottom-bar-light", widthDp = 400)
@Composable fun BottomBarLight() = Framed(dark = false) { Bar("dashboard") }

/** A flat tab selected rather than the centre button, so the selected-item indicator is covered. */
@PreviewTest
@Preview(name = "bottom-bar-dark-tab-selected", widthDp = 400)
@Composable fun BottomBarDarkTabSelected() = Framed(dark = true) { Bar("budget_tracker") }

/** Just below the threshold: labels are still shown and must not wrap. */
@PreviewTest
@Preview(name = "bottom-bar-dark-font140", widthDp = 400, fontScale = 1.4f)
@Composable fun BottomBarDarkFont140() = Framed(dark = true) { Bar("dashboard") }

/** At the threshold: the first scale that drops labels. */
@PreviewTest
@Preview(name = "bottom-bar-dark-font150", widthDp = 400, fontScale = 1.5f)
@Composable fun BottomBarDarkFont150() = Framed(dark = true) { Bar("dashboard") }

/** The scale that produced the original bug report. */
@PreviewTest
@Preview(name = "bottom-bar-dark-font200", widthDp = 400, fontScale = 2.0f)
@Composable fun BottomBarDarkFont200() = Framed(dark = true) { Bar("dashboard") }

@PreviewTest
@Preview(name = "bottom-bar-light-font200", widthDp = 400, fontScale = 2.0f)
@Composable fun BottomBarLightFont200() = Framed(dark = false) { Bar("dashboard") }
