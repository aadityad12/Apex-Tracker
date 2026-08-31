package com.example.apextracker.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The GRAPHITE identity (Plan.md Phase 2, superseding issue #139's warm grayscale): a cold
 * monochrome — and as of 2026-08-11, **entirely** monochrome. There is no accent hue and no
 * semantic hue either: emphasis is carried by ink (near-white in dark, near-black in light),
 * size, weight and position, and nothing else.
 *
 * Sage (met/positive) and Crimson (error) used to be the two exceptions. The owner's call was
 * that two hues scattered across checkmarks, delete buttons and overdue rows read as
 * inconsistency rather than as meaning, so both are gone. Nothing is lost by it: the design
 * already required every negative state to carry an icon or a word, never hue alone, so those
 * signals were never doing the work on their own. `error` is now simply full-strength ink, which
 * on a screen whose supporting text is `onSurfaceVariant` still reads as the loud thing.
 *
 * The one deliberate exception is **budget category colour**, which is content rather than
 * chrome — eight hues are the only thing separating eight pie slices, and they are validated for
 * CVD separation in `CategoryPalette`/`Design.md` §6. Third-party app icons keep their brand
 * colours for the same reason.
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

// The semantic hues (Sage / Crimson) were removed on 2026-08-11 — see the note at the top of
// this file. `error` resolves to ink in both schemes; do not reintroduce a hue here.

val ApexDarkColors: ColorScheme = darkColorScheme(
    // Monochrome's emphasis is light itself: primary is ink, and a filled button is an
    // inverse (near-white) block. If a screen doesn't read with an ink primary, its hierarchy
    // is broken and colour was carrying it — fix the hierarchy, don't add a hue.
    primary = Frost,
    onPrimary = Color(0xFF111316),
    primaryContainer = Color(0xFF2A2F35),
    onPrimaryContainer = Frost,
    // Secondary is a dimmer ink, never a second hue — there is no semantic hue left at all as of
    // 2026-08-11 (see the note atop this file). The pre-graphite version of this app had
    // FilterChips default to secondaryContainer and render "goal met" green; that failure mode is
    // exactly why secondaryContainer stays a neutral tone (GraphiteElevated/PaperElevated) rather
    // than resolving to anything meaningful.
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
    // Ink, not a hue. A destructive control is distinguished by its icon and its word; an error
    // message by sitting at full ink strength while the copy around it is onSurfaceVariant.
    error = Frost,
    onError = GraphiteBase,
    errorContainer = GraphiteElevated,
    onErrorContainer = Frost,
    // ── Fixed roles (Issue #245, M3 1.4.0) ─────────────────────────────────────────────
    // M3 defines Fixed colors to stay visually identical whether the active theme is light or
    // dark (e.g. a chip that shouldn't flip tone when the user switches themes) — so unlike
    // every other slot in this file, these values are the same literal colours in both
    // ApexDarkColors and ApexLightColors below, not a per-theme pair. No accent hue exists in
    // this design, so Fixed borrows the light theme's own paper/ink container relationship,
    // collapsed across primary/secondary/tertiary the same way this design already treats them
    // as one ink identity rather than three distinct hues.
    primaryFixed = PaperElevated,
    primaryFixedDim = PaperRaised,
    onPrimaryFixed = Char,
    onPrimaryFixedVariant = CharDim,
    secondaryFixed = PaperElevated,
    secondaryFixedDim = PaperRaised,
    onSecondaryFixed = Char,
    onSecondaryFixedVariant = CharDim,
    tertiaryFixed = PaperElevated,
    tertiaryFixedDim = PaperRaised,
    onTertiaryFixed = Char,
    onTertiaryFixedVariant = CharDim
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
    error = Char,
    onError = PaperSurface,
    errorContainer = PaperElevated,
    onErrorContainer = Char,
    // Fixed roles — identical values to ApexDarkColors' block above; see the comment there.
    primaryFixed = PaperElevated,
    primaryFixedDim = PaperRaised,
    onPrimaryFixed = Char,
    onPrimaryFixedVariant = CharDim,
    secondaryFixed = PaperElevated,
    secondaryFixedDim = PaperRaised,
    onSecondaryFixed = Char,
    onSecondaryFixedVariant = CharDim,
    tertiaryFixed = PaperElevated,
    tertiaryFixedDim = PaperRaised,
    onTertiaryFixed = Char,
    onTertiaryFixedVariant = CharDim
)

/**
 * Fill for chart marks that are *not* the current period. Authored per theme rather than an
 * onSurface alpha — a single alpha that reads on near-black is invisible on paper.
 */
val ChartMutedDark = Color(0xFF33383E)
val ChartMutedLight = Color(0xFFD3D7DC)

/**
 * The heatmap's intensity ramp: **brighter means more of that day's goals met** (2026-08-11).
 *
 * This replaces the fill-height bars the Graphite redesign shipped. The bars encoded intensity as
 * geometry specifically so the grid would not read as GitHub's; the owner's call is that the
 * GitHub reading is the *familiar* one and worth having, and in a now fully-monochrome app a
 * brightness ramp is the natural encoding anyway — there is no hue left for it to compete with.
 *
 * Six steps, indexed by `intensityBucket(fraction) + 1`, with index 0 reserved for a day that had
 * no goals at all:
 *
 * | index | day |
 * |---|---|
 * | 0 | untracked — no goals were active |
 * | 1 | tracked, none met |
 * | 2–4 | partial |
 * | 5 | perfect |
 *
 * Re-authored rather than reused. The previous ramp compressed at the bottom — untracked→none-met
 * was only ΔL* 6.9 in dark and 4.9 in light, so "I had goals and missed them all" looked identical
 * to "I had no goals", which is the one distinction this graph exists to draw. Steps are now
 * evenly spaced in L* (no gap below 8.9) and the cold cast is a constant RGB offset, so the tint
 * stays subtle instead of turning blue at the ends.
 *
 * The bottom two steps sit deliberately below 3:1 against the background: they encode *absence*,
 * and a grid whose empty cells shouted would drown the days that aren't empty. What has to be
 * legible is each step against its neighbours, which is what the even L* spacing buys.
 */
val ApexHeatRampDark = listOf(
    Color(0xFF171C26), // 0 untracked
    Color(0xFF2B303A), // 1 tracked, none met
    Color(0xFF4B505A), // 2
    Color(0xFF6D727C), // 3
    Color(0xFF9499A3), // 4
    Color(0xFFC7CCD6)  // 5 perfect
)

val ApexHeatRampLight = listOf(
    Color(0xFFE3E8F2), // 0 untracked
    Color(0xFFCACFD9), // 1 tracked, none met
    Color(0xFFA6ABB5), // 2
    Color(0xFF818690), // 3
    Color(0xFF595E68), // 4
    Color(0xFF30353F)  // 5 perfect
)
