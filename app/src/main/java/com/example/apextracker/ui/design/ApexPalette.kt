package com.example.apextracker.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The GRAPHITE identity (Plan.md Phase 2, superseding issue #139's warm grayscale): a cold
 * monochrome. Colour appears only where it carries meaning — Sage for met/positive, Crimson for
 * over/error — everything else is a neutral ramp. There is deliberately no accent hue: emphasis
 * is carried by ink (near-white in dark, near-black in light), size, and weight.
 *
 * The temperature is the point. The previous Ember identity's warm near-blacks + terracotta +
 * serif sat squarely in Claude's visual territory; cool graphite is the single biggest move away
 * from it. Values were judged on a real AMOLED panel via the debug style plate (2026-07-30) —
 * both hairlines were bumped after measuring 2.8:1 (dark) and 2.2:1 (light) against the 3:1
 * floor; the plate now reads 3.2/3.1.
 */

// ── Dark ───────────────────────────────────────────────────────────────────────────────
val GraphiteBase     = Color(0xFF0E0F11) // app background
val GraphiteSurface  = Color(0xFF16181B) // resting surface
val GraphiteRaised   = Color(0xFF1D2024) // grouped content, sheet body, heatmap slots
val GraphiteElevated = Color(0xFF262A2F) // menus, dialogs, pressed states
val GraphiteLine     = Color(0xFF5E656E) // hairline dividers, borders (≥3:1 on base — load-bearing)
val GraphiteLineFaint = Color(0xFF2C3036) // barely-there separation inside a group
val Frost            = Color(0xFFE9EBEE) // primary text AND the dark theme's ink/primary
val FrostDim         = Color(0xFF9AA1A9) // secondary text, axis labels

// ── Light ──────────────────────────────────────────────────────────────────────────────
val PaperBase      = Color(0xFFF3F4F6) // cold paper — not cream; warmth is the old identity's
val PaperSurface   = Color(0xFFFFFFFF)
val PaperRaised    = Color(0xFFE9EBEE)
val PaperElevated  = Color(0xFFDFE2E6)
val PaperLine      = Color(0xFF848B95)
val PaperLineFaint = Color(0xFFD9DDE2)
val Char           = Color(0xFF191C20) // primary text AND the light theme's ink/primary
val CharDim        = Color(0xFF575E66)

/**
 * Semantic pair — the only hues in the app, which is exactly why they survive: on a neutral
 * field they read louder than they did next to Ember (measured on the plate: Sage 7.0:1 dark /
 * 4.6:1 light, Crimson 4.4:1 / 6.3:1). Negative states are additionally always carried by an
 * icon or a word, never by hue alone.
 */
val SageDark  = Color(0xFF6FA88C)
val SageLight = Color(0xFF3F7A62)
val AlarmDark  = Color(0xFFDC3D57)
val AlarmLight = Color(0xFFAE1F38)

val ApexDarkColors: ColorScheme = darkColorScheme(
    // Monochrome's emphasis is light itself: primary is ink, and a filled button is an
    // inverse (near-white) block. If a screen doesn't read with an ink primary, its hierarchy
    // is broken and colour was carrying it — fix the hierarchy, don't add a hue.
    primary = Frost,
    onPrimary = Color(0xFF111316),
    primaryContainer = Color(0xFF2A2F35),
    onPrimaryContainer = Frost,
    // Secondary is a dimmer ink, never a second hue. (Sage lives only in ApexSemantics.positive
    // — see the pre-graphite note about FilterChips rendering "goal met" green.)
    secondary = Color(0xFFA6ADB6),
    onSecondary = Color(0xFF111316),
    secondaryContainer = GraphiteElevated,
    onSecondaryContainer = Frost,
    tertiary = FrostDim,
    onTertiary = GraphiteBase,
    // Every slot the app touches must be defined in BOTH schemes — an undefined slot falls back
    // to Material's default scheme (purple), not to something neutral.
    tertiaryContainer = GraphiteElevated,
    onTertiaryContainer = Frost,
    background = GraphiteBase,
    onBackground = Frost,
    surface = GraphiteSurface,
    onSurface = Frost,
    surfaceVariant = GraphiteRaised,
    onSurfaceVariant = FrostDim,
    surfaceContainerHighest = GraphiteElevated,
    outline = GraphiteLine,
    outlineVariant = GraphiteLineFaint,
    error = AlarmDark,
    onError = Color(0xFF1A0E0D),
    errorContainer = Color(0xFF3D1F1C),
    onErrorContainer = Color(0xFFF0A49E)
)

val ApexLightColors: ColorScheme = lightColorScheme(
    primary = Char,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = PaperElevated,
    onPrimaryContainer = Char,
    secondary = Color(0xFF454C55),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4E7EB),
    onSecondaryContainer = Char,
    tertiary = CharDim,
    onTertiary = PaperSurface,
    tertiaryContainer = PaperElevated,
    onTertiaryContainer = Char,
    background = PaperBase,
    onBackground = Char,
    surface = PaperSurface,
    onSurface = Char,
    surfaceVariant = PaperRaised,
    onSurfaceVariant = CharDim,
    surfaceContainerHighest = PaperElevated,
    outline = PaperLine,
    outlineVariant = PaperLineFaint,
    error = AlarmLight,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DAD7),
    onErrorContainer = Color(0xFF5C1913)
)

/**
 * Fill for chart marks that are *not* the current period. Authored per theme rather than an
 * onSurface alpha — a single alpha that reads on near-black is invisible on paper.
 */
val ChartMutedDark = Color(0xFF33383E)
val ChartMutedLight = Color(0xFFD3D7DC)

/**
 * The gray intensity ramp, kept for surfaces that still need a fill-per-bucket scale (the Glance
 * widgets, the day legend fallback). The heatmap itself no longer uses it: with hue gone, a gray
 * ramp has less resolution than ember had, so the in-app heatmap switched to *fill-height bars*
 * (see ApexSemantics.heatInk/heatSlot and DashboardView.HeatCell) — intensity is carried by
 * geometry, which is also what stops the grid pattern-matching GitHub.
 */
val ApexHeatRampDark = listOf(
    Color(0xFF17191C), // -1 untracked
    Color(0xFF24272C), // 0 tracked, none met
    Color(0xFF3A4046), // 1
    Color(0xFF565E67), // 2
    Color(0xFF7C8791), // 3
    Color(0xFFC2CAD3)  // 4 perfect
)

val ApexHeatRampLight = listOf(
    Color(0xFFECEEF1), // -1 untracked
    Color(0xFFDDE0E5), // 0
    Color(0xFFBEC4CC), // 1
    Color(0xFF99A1AB), // 2
    Color(0xFF6E7883), // 3
    Color(0xFF30353C)  // 4 perfect
)

/** Bar ink for the heatmap's fill-height cells; partial days modulate its alpha, perfect days
 *  snap to full onSurface. Judged against slots ([GraphiteRaised]/[PaperRaised]) on the plate. */
val HeatInkDark = Color(0xFFC2CAD3)
val HeatInkLight = Color(0xFF30353C)
