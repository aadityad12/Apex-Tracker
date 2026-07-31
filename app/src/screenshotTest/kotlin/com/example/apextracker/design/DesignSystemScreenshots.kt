package com.example.apextracker.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import androidx.compose.ui.unit.dp
import com.example.apextracker.ui.design.ApexChartFrame
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexEmptyState
import com.example.apextracker.ui.design.ApexGroup
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.ApexStatRow
import com.example.apextracker.ui.design.ApexTrackerTheme
import com.example.apextracker.ui.design.LocalApexSemantics

/**
 * Reference renders of the design system. These are the redesign's regression net: any change to
 * a token, a face, or a component shows up here as an image diff before it reaches a screen.
 *
 *   record a new baseline:  ./gradlew updateDebugScreenshotTest
 *   check against baseline: ./gradlew validateDebugScreenshotTest
 *
 * Baselines live in app/src/screenshotTestDebug/reference/ and are checked in. A failing run
 * writes an HTML report with reference/actual/diff to app/build/reports/screenshotTest/.
 *
 * Note the filenames embed a hash of the preview's configuration, so changing a @Preview's
 * parameters orphans its baseline rather than updating it — delete the stale file when that
 * happens, or the reference directory accumulates images nothing renders any more.
 *
 * Every composable here is rendered in dark, light, and dark-at-200%-font, because those are the
 * three states the design contract promises and the three that break silently.
 */

@Composable
private fun TypeSpecimen() {
    val t = MaterialTheme.typography
    Column(Modifier.padding(ApexSpacing.l), verticalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
        Text("Instrument Serif", style = t.displayMedium)
        Text("Budget flow", style = t.displaySmall)
        Text("Geist · titleLarge", style = t.titleLarge)
        Text("GEIST · EYEBROW", style = t.titleSmall)
        Text("Geist · bodyLarge, 0123456789", style = t.bodyLarge)
        Text("Geist · bodySmall", style = t.bodySmall)
        Spacer(Modifier.height(ApexSpacing.s))
        Text("02:14:37", style = ApexNumerals.hero)
        Text("$1,284.50", style = ApexNumerals.medium)
        Text("0m", style = ApexNumerals.small)
    }
}

@Composable
private fun PaletteSpecimen() {
    val cs = MaterialTheme.colorScheme
    val positive = LocalApexSemantics.current.positive
    val ramp = LocalApexSemantics.current.heatRamp
    Column(Modifier.padding(ApexSpacing.l), verticalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
        ApexSectionHeader("Surfaces")
        Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
            listOf(cs.background, cs.surface, cs.surfaceVariant, cs.surfaceContainerHighest).forEach {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(ApexShapes.cell)).background(it))
            }
        }
        ApexSectionHeader("Semantics — Ink / Sage / Crimson must be mutually distinct")
        Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
            listOf(cs.primary, positive, cs.error).forEach {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(ApexShapes.cell)).background(it))
            }
        }
        ApexSectionHeader("Heat ramp — index 0 cool, index 1 warm, both visible")
        Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.xs)) {
            ramp.forEach {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(ApexShapes.cell)).background(it))
            }
        }
    }
}

@Composable
private fun ComponentSpecimen() {
    Column(Modifier.padding(ApexSpacing.l), verticalArrangement = Arrangement.spacedBy(ApexSpacing.xl)) {
        ApexChartFrame("This month") {
            Text("$1,284.50", style = ApexNumerals.large)
        }
        Column {
            ApexSectionHeader("Transactions")
            Spacer(Modifier.height(ApexSpacing.s))
            ApexStatRow("Groceries", "$1,284.50", supporting = "Recurring · 12 Jul")
            ApexDivider()
            ApexStatRow("Rent", "$2,100.00")
            ApexDivider()
            ApexStatRow("Coffee", "$8.75", valueColor = LocalApexSemantics.current.positive)
        }
        ApexGroup {
            ApexSectionHeader("Today")
            Spacer(Modifier.height(ApexSpacing.s))
            Text("02:14:37", style = ApexNumerals.hero)
        }
        // Controls whose M3 defaults this design overrides — a pill-shaped Button or a
        // Sage-green selected chip regressing would show up as a diff here.
        Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
            Button(onClick = {}, shape = RoundedCornerShape(ApexShapes.control)) { Text("Save") }
            OutlinedButton(onClick = {}, shape = RoundedCornerShape(ApexShapes.control)) { Text("Cancel") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
            FilterChip(selected = true, onClick = {}, label = { Text("12 months") },
                shape = RoundedCornerShape(ApexShapes.control))
            FilterChip(selected = false, onClick = {}, label = { Text("2026") },
                shape = RoundedCornerShape(ApexShapes.control))
            Checkbox(checked = true, onCheckedChange = {})
        }
        ApexEmptyState(
            message = "No goals yet — add one to start tracking",
            actionLabel = "Add goal",
            onAction = {}
        )
    }
}

@Composable
private fun Framed(dark: Boolean, content: @Composable () -> Unit) {
    ApexTrackerTheme(darkTheme = dark) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) { content() }
    }
}

@PreviewTest
@Preview(name = "type-dark", widthDp = 380)
@Composable fun TypeDark() = Framed(dark = true) { TypeSpecimen() }

@PreviewTest
@Preview(name = "type-light", widthDp = 380)
@Composable fun TypeLight() = Framed(dark = false) { TypeSpecimen() }

@PreviewTest
@Preview(name = "type-dark-font200", widthDp = 380, fontScale = 2.0f)
@Composable fun TypeDarkLargeFont() = Framed(dark = true) { TypeSpecimen() }

@PreviewTest
@Preview(name = "palette-dark", widthDp = 380)
@Composable fun PaletteDark() = Framed(dark = true) { PaletteSpecimen() }

@PreviewTest
@Preview(name = "palette-light", widthDp = 380)
@Composable fun PaletteLight() = Framed(dark = false) { PaletteSpecimen() }

@PreviewTest
@Preview(name = "components-dark", widthDp = 380, heightDp = 900)
@Composable fun ComponentsDark() = Framed(dark = true) { ComponentSpecimen() }

@PreviewTest
@Preview(name = "components-light", widthDp = 380, heightDp = 900)
@Composable fun ComponentsLight() = Framed(dark = false) { ComponentSpecimen() }

@PreviewTest
@Preview(name = "components-dark-font200", widthDp = 380, heightDp = 1600, fontScale = 2.0f)
@Composable fun ComponentsDarkLargeFont() = Framed(dark = true) { ComponentSpecimen() }
