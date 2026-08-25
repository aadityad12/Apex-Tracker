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
    // ── The container ladder ───────────────────────────────────────────────────────────
    // M3 has five container steps; this design has four tones. They are mapped by *role*,
    // not by walking M3's ordering, so several steps deliberately share a tone. See the
    // audit note above ApexLightColors for what each one is reached by and why it matters.
    surfaceContainerLowest = GraphiteBase,
    surfaceContainerLow = GraphiteSurface,   // sheet bodies
    surfaceContainer = GraphiteElevated,     // menus
    surfaceContainerHigh = GraphiteElevated, // dialogs
    surfaceContainerHighest = GraphiteElevated,
    surfaceDim = GraphiteBase,
    surfaceBright = GraphiteElevated,
    // Transparent kills M3's tonal-elevation tint outright: surfaceColorAtElevation composites
    // surfaceTint over surface, so a non-transparent value would drift raised surfaces toward
    // ink. This design layers with authored tones and hairlines instead (Design.md §5).
    surfaceTint = Color.Transparent,
    scrim = GraphiteBase,
    // Snackbars invert. The dark scheme's snackbar is a *light* surface, so it takes the paper
    // tones — and, since graphite has no accent, its "primary" is just the light theme's ink
    // (Char), same as inverseOnSurface. That's not a shortcut: under Ember this slot resolved to
    // the light accent; under graphite "primary" and "onSurface" are the same thing in every
    // theme, so both inverse slots land on Char here.
    inverseSurface = PaperRaised,
    inverseOnSurface = Char,
    inversePrimary = Char,
    outline = GraphiteLine,
    outlineVariant = GraphiteLineFaint,
    error = AlarmDark,
    onError = Color(0xFF1A0E0D),
    errorContainer = Color(0xFF3D1F1C),
    onErrorContainer = Color(0xFFF0A49E)
)

/**
 * Every `ColorScheme` slot the app can reach — audited 2026-07-30 by resolving each M3 component's
 * default `*Tokens.ContainerColor` back to its `ColorSchemeKeyTokens`, rather than by reading call
 * sites (a call site that sets nothing is exactly the one that reaches a slot silently).
 *
 * What that turned up, beyond the already-known `tertiaryContainer` hole:
 *
 * | Slot | Reached by | Was rendering |
 * |---|---|---|
 * | `surfaceContainer` | `ExposedDropdownMenu`, `DropdownMenuItem`, scrolled `TopAppBar` | baseline Neutral12 / Neutral94 |
 * | `surfaceContainerHigh` | `AlertDialog` ×19, `DatePickerDialog` ×4 — none overrode it | baseline Neutral17 / Neutral92 |
 * | `surfaceContainerLow` | the one `ModalBottomSheet` that set no `containerColor` | baseline Neutral10 / Neutral96 |
 * | `inverseSurface`/`inverseOnSurface`/`inversePrimary` | `Snackbar` ×3, all reachable | baseline — the action label was Material **Primary40/80 purple** |
 * | `scrim` | `ModalBottomSheet` ×3 | baseline #000 (benign, but undeclared) |
 *
 * `surfaceContainer` and `surfaceContainerHigh` share `GraphiteElevated`/`PaperElevated` because
 * Design.md §3 gives that tone to "menus, dialogs" as one role. A menu opening *inside* a dialog
 * (`RecurrencePickerDialog`, the budget category dropdown) is therefore tone-identical to its
 * host, and separates on a hairline instead — which is this design's own device. That border is
 * passed at the `ExposedDropdownMenu` call sites via `apexMenuBorder()`; **do not drop it while
 * these two slots share a tone.**
 */
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
    // Same role mapping as the dark scheme — see the audit note above.
    surfaceContainerLowest = PaperBase,
    surfaceContainerLow = PaperSurface,   // sheet bodies
    surfaceContainer = PaperElevated,     // menus
    surfaceContainerHigh = PaperElevated, // dialogs
    surfaceContainerHighest = PaperElevated,
    surfaceDim = PaperRaised,
    surfaceBright = PaperSurface,
    surfaceTint = Color.Transparent,
    // scrim is the one slot that is legitimately identical across themes: a sheet needs the same
    // dimming behind it in light mode as in dark, so both use the cool near-black.
    scrim = GraphiteBase,
    // The light scheme's snackbar is a *dark* surface: graphite tones, and — no accent to borrow
    // — the dark theme's ink (Frost) for both the text and the action label.
    inverseSurface = GraphiteRaised,
    inverseOnSurface = Frost,
    inversePrimary = Frost,
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
 * The gray intensity ramp behind the Dashboard heatmap's filled-square cells, the legend, and the
 * Glance widgets — index 0 is the untracked shade, 1..5 map to `intensityBucket()` 0..4. This is
 * the GitHub-contribution-graph read: a full square whose *colour* carries the day's fraction of
 * goals met. (An earlier Graphite-era pass tried encoding the fraction as bar height instead —
 * see git history on DashboardView.HeatCell — but that read as a bespoke chart rather than the
 * familiar contribution grid, so it was reverted.)
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
