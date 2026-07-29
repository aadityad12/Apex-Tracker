package com.example.apextracker.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Two hand-authored palettes. Neither is derived from the other — the old
 * `shiftColorForLightMode()` HSV multiply is exactly the shortcut this replaces.
 *
 * The darks are deliberately *warm* near-blacks rather than the conventional cool #121212.
 * A cool grey under a warm terracotta accent reads as two unrelated decisions; warming the
 * substrate is most of what makes the accent look chosen rather than dropped on.
 */

// ── Dark ───────────────────────────────────────────────────────────────────────────────
val InkBase      = Color(0xFF131210) // app background
val InkSurface   = Color(0xFF1A1816) // resting surface
val InkRaised    = Color(0xFF221F1C) // grouped content, sheet body
val InkElevated  = Color(0xFF2B2724) // menus, dialogs, pressed states
// Measured on device: the first pass at #3D3835 came out at 1.6:1 on InkBase — invisible.
// Hairlines are load-bearing here (they are what replaces cards), so they are pushed up to
// where they actually read rather than left at a decorative weight.
val InkLine      = Color(0xFF5A544F) // hairline dividers, borders
val InkLineFaint = Color(0xFF332E2B) // barely-there separation inside a group
val Bone         = Color(0xFFEDE8E2) // primary text — warm off-white, never pure white
val BoneDim      = Color(0xFFA09890) // secondary text, axis labels

// ── Light ──────────────────────────────────────────────────────────────────────────────
val PaperBase      = Color(0xFFF5F2ED)
val PaperSurface   = Color(0xFFFFFDFA)
val PaperRaised    = Color(0xFFEBE6DE)
val PaperElevated  = Color(0xFFE2DCD2)
val PaperLine      = Color(0xFFB3AA9A) // was #D8D1C6 — measured 1.4:1 on PaperBase, invisible
val PaperLineFaint = Color(0xFFDCD5C9)
val Sable          = Color(0xFF1B1815)
val SableDim       = Color(0xFF5F574E)

// ── Accent ─────────────────────────────────────────────────────────────────────────────
/** The one brand colour. Carries emphasis and attention — never "good" and never "bad". */
val EmberDark  = Color(0xFFD9613C)
val EmberLight = Color(0xFFB84A28)

/**
 * Semantic pair. These exist because Ember is an orange-red, so a conventional error red sits
 * ~15° away from it and the two are not reliably distinguishable — least of all for a
 * red-weak viewer. [Sage] is far enough around the wheel to survive that, and negative states
 * are additionally always carried by an icon or a word, never by hue alone.
 *
 * Judge these three side by side on the style plate before trusting them.
 */
val SageDark      = Color(0xFF6FA88C)
val SageLight     = Color(0xFF3F7A62)
/**
 * Crimson, not the conventional red. The first pass used #E0574E, ~15° from Ember: on device
 * "Missed" and "12 days" were marginal in dark and literally the same colour in light. Crimson
 * separates on hue *and* value, which is what makes it survive both themes.
 *
 * Do not warm this toward Ember. If a future change makes Alarm more orange, the two stop
 * being distinguishable and every over-budget and missed-goal state in the app degrades at
 * once — silently, because nothing crashes.
 */
val AlarmDark  = Color(0xFFDC3D57)
val AlarmLight = Color(0xFFAE1F38)

val ApexDarkColors: ColorScheme = darkColorScheme(
    primary = EmberDark,
    onPrimary = Color(0xFF14100E),
    primaryContainer = Color(0xFF3A211A),
    onPrimaryContainer = Color(0xFFF0A98F),
    // Secondary is a *desaturated Ember*, not Sage. Sage lives only in ApexSemantics.positive.
    // Putting it in the ColorScheme meant every M3 component that defaults to secondary —
    // FilterChip's selected state most visibly — rendered "goal met" green for things that had
    // nothing to do with goals. One accent means secondary is a variant of it, not a new hue.
    secondary = Color(0xFFB5745C),
    onSecondary = Color(0xFF14100E),
    secondaryContainer = Color(0xFF3A211A),
    onSecondaryContainer = Color(0xFFF0A98F),
    tertiary = BoneDim,
    onTertiary = InkBase,
    background = InkBase,
    onBackground = Bone,
    surface = InkSurface,
    onSurface = Bone,
    surfaceVariant = InkRaised,
    onSurfaceVariant = BoneDim,
    surfaceContainerHighest = InkElevated,
    outline = InkLine,
    outlineVariant = InkLineFaint,
    error = AlarmDark,
    onError = Color(0xFF1A0E0D),
    errorContainer = Color(0xFF3D1F1C),
    onErrorContainer = Color(0xFFF0A49E)
)

val ApexLightColors: ColorScheme = lightColorScheme(
    primary = EmberLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF7DED3),
    onPrimaryContainer = Color(0xFF5C220F),
    // See the dark scheme's note: secondary is a desaturated Ember, never Sage.
    secondary = Color(0xFF94553B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7DED3),
    onSecondaryContainer = Color(0xFF5C220F),
    tertiary = SableDim,
    onTertiary = PaperSurface,
    background = PaperBase,
    onBackground = Sable,
    surface = PaperSurface,
    onSurface = Sable,
    surfaceVariant = PaperRaised,
    onSurfaceVariant = SableDim,
    surfaceContainerHighest = PaperElevated,
    outline = PaperLine,
    outlineVariant = PaperLineFaint,
    error = AlarmLight,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DAD7),
    onErrorContainer = Color(0xFF5C1913)
)

/**
 * The heatmap ramp — the app's signature surface, so it gets an explicit six-step scale
 * rather than alpha-modulating the accent at the call site (which is what produces a ramp
 * whose middle steps are indistinguishable).
 *
 * Index 0 is "no goals active that day". It separates from index 1 ("tracked, none met") by
 * *hue*, not by visibility: untracked is a cool neutral, tracked-but-empty is warm. An earlier
 * pass made untracked near-background so it couldn't be mistaken for a failed day, which
 * overcorrected — it erased the grid's structure, so a user with little history saw a blank
 * panel instead of the shape of their year. Both must be visible; only their temperature differs.
 *
 * Steps 1..5 walk from a dim ember toward full intensity, gaining chroma and lightness together
 * so they separate on an AMOLED panel at 20dp as well as they do in a swatch strip.
 */
val ApexHeatRampDark = listOf(
    Color(0xFF211F1E), // -1  untracked — visible, cool neutral: the cell exists, nothing was asked of it
    Color(0xFF3B2C24), // 0   tracked, none met — visible and warm: on the ramp, at zero
    Color(0xFF6B3524), // 1
    Color(0xFF9A462A), // 2
    Color(0xFFC05733), // 3
    Color(0xFFEE7A4E)  // 4   perfect day — brighter than the accent itself, so it reads as a peak
)

val ApexHeatRampLight = listOf(
    Color(0xFFE7E4DF), // -1  untracked — visible, cool neutral
    Color(0xFFDCCCBD), // 0   tracked, none met — visible and warm
    Color(0xFFEFC0A6), // 1
    Color(0xFFE29370), // 2
    Color(0xFFC96844), // 3
    Color(0xFFA33C1C)  // 4   perfect day
)
