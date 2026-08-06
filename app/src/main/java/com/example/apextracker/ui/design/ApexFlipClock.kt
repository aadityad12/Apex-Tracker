package com.example.apextracker.ui.design

import com.example.apextracker.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * A split-flap ("flip clock") readout of an elapsed duration.
 *
 * Each digit is a card split at a horizontal seam. When a digit changes, the top half of the old
 * digit hinges down over the seam and the new digit's bottom half falls into place behind it. Only
 * the cards whose digit actually changed move — see [flipClockGroups] for why the slot keys are what
 * make that true.
 *
 * This satisfies the design law's motion rule the same way the determinate goal arc did and the
 * counter-rotating rings did not: the flap is driven by a value changing, never by a clock running.
 * Nothing here animates on its own. In particular **the field gap must never blink** — a blinking
 * separator is the textbook case of the ambient decoration this app deleted.
 *
 * @param seconds a lambda, not a value, on purpose. A running study session emits once a second, and
 *   reading that flow in the caller's scope recomposes the caller's whole tree — chart, history rows
 *   and all — sixty times a minute. Deferring the read confines the per-tick recomposition to here.
 * @param active selects the card tone. Under GRAPHITE `primary` and `onSurface` are the same colour,
 *   so a running/idle distinction cannot be carried by ink; it is carried by lifting the card one
 *   step further from the background instead.
 * @param ambient selects the fixed dark, low-power palette and disables flap animation. It is used
 *   only by Study's in-app ambient display, independent of the app's light/dark theme.
 */
@Composable
fun ApexFlipClock(
    seconds: () -> Long,
    modifier: Modifier = Modifier,
    style: TextStyle = ApexNumerals.hero,
    active: Boolean = false,
    ambient: Boolean = false,
) {
    val value = seconds()
    val metrics = rememberFlipMetrics(style)
    val colors = if (ambient) {
        FlipClockColors(card = GraphiteSurface, edge = GraphiteLineFaint, ink = FrostDim)
    } else {
        FlipClockColors(
            card = if (active) MaterialTheme.colorScheme.surfaceContainerHighest
            else MaterialTheme.colorScheme.surfaceVariant,
            edge = MaterialTheme.colorScheme.outlineVariant,
            ink = MaterialTheme.colorScheme.onSurface,
        )
    }

    // Only a +1s tick is a flap. Everything else is a jump and snaps: the asynchronous restore after
    // process death (the flow starts at 0 and fills in a moment later, which would otherwise flip
    // every card at once on the first frame), a reset, a subject switch, a manual backfill, the
    // midnight rollover, and resuming after the screen was off. One predicate covers all of them,
    // and it makes the very first render instant by construction rather than by a first-frame flag.
    var lastValue by remember { mutableStateOf<Long?>(null) }
    val animate = !ambient && lastValue?.let { value - it == 1L } == true
    SideEffect { lastValue = value }

    val groups = flipClockGroups(value)
    val hasHours = groups.size == 3
    // Held across the exit so AnimatedVisibility still has cards to draw while it shrinks.
    var hourSlots by remember { mutableStateOf(emptyList<FlipSlot>()) }
    if (hasHours) hourSlots = groups.first()

    val spoken = spokenDuration(value)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The hour field is content arriving, so it expands rather than appearing. expandHorizontally
        // animates the measured width and the clock is centred by its parent, so the minute and
        // second cards slide over rather than teleporting. The freshly mounted hour cards reach
        // FlipDigit with digit == shown and therefore arrive without flapping, which is right: they
        // are new content, not a changed value.
        AnimatedVisibility(
            visible = hasHours,
            enter = expandHorizontally(ApexMotion.enter(), expandFrom = Alignment.End) +
                    fadeIn(ApexMotion.enter()),
            exit = shrinkHorizontally(ApexMotion.exit(), shrinkTowards = Alignment.End) +
                    fadeOut(ApexMotion.exit()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DigitGroup(hourSlots, animate, metrics, colors)
                Spacer(Modifier.width(ApexSpacing.m))
            }
        }
        DigitGroup(groups[groups.lastIndex - 1], animate, metrics, colors)
        Spacer(Modifier.width(ApexSpacing.m))
        DigitGroup(groups.last(), animate, metrics, colors)
    }
}

/**
 * One field — hours, minutes or seconds. There is deliberately **no colon** between fields: a
 * split-flap board separates fields by gap, and in a monospace face a colon carries a full digit
 * advance, so a colon would be as wide as a card.
 */
@Composable
private fun DigitGroup(
    slots: List<FlipSlot>,
    animate: Boolean,
    metrics: FlipMetrics,
    colors: FlipClockColors,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.hairline)) {
        slots.forEach { slot ->
            key(slot.key) { FlipDigit(slot.char, animate, metrics, colors) }
        }
    }
}

/**
 * One card. Four layers over a two-phase progress:
 *
 * | layer        | shows                    | hinge            | rotationX                 |
 * |--------------|--------------------------|------------------|---------------------------|
 * | static upper | new digit, top half      | —                | —                         |
 * | static lower | old digit, bottom half   | —                | —                         |
 * | flap ①       | old digit, top half      | its bottom edge  | 0° → −90° over p ∈ [0,.5] |
 * | flap ②       | new digit, bottom half   | its top edge     | +90° → 0° over p ∈ [.5,1] |
 *
 * At rest (progress 1f) the composite is already correct — flap ① is hidden and flap ② sits at
 * rotationX 0 covering the static lower — so there is no reconciliation step after the animation and
 * `previous` never needs clearing.
 *
 * The rotation is read inside the `graphicsLayer { }` *lambda* overload, which runs in the layer
 * phase. Reading the Animatable there does not invalidate composition, so a running flap costs zero
 * recompositions per frame; the only recomposition is the single one that swaps the two digits.
 *
 * There is no flap shading. A real flap darkens as it turns, but graphite has no tone that means
 * "darker" in both themes (surfaceContainerLowest is darker in dark mode and lighter in light), and
 * a black overlay is off-palette — it reads as grime on the light theme's paper. The foreshortening
 * and the seam carry the flip on their own.
 */
@Composable
private fun FlipDigit(
    digit: Char,
    animate: Boolean,
    metrics: FlipMetrics,
    colors: FlipClockColors,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(digit) }
    var previous by remember { mutableStateOf(digit) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(digit) {
        if (digit == shown) return@LaunchedEffect
        if (!animate) {
            previous = digit
            shown = digit
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        previous = shown
        progress.snapTo(0f)          // reset while both halves still read as the old digit
        shown = digit
        // exit() then enter(), not a spring. settle() overshoots past 1f, which drives flap ② to a
        // negative rotationX — the landed flap lifts its edge back toward the viewer and bounces,
        // and a flap lands rather than bouncing. snap() resolves in ~100ms, too fast to read as a
        // flip at all. These two tokens are also the literal semantics: the old digit is content
        // leaving (accelerating, as gravity does) and the new digit is content arriving (decelerating
        // into place). 460ms total, comfortably inside the one-second tick.
        // Measured on device: ~190ms fall + ~300ms land. Reduced motion ("Remove animations", which
        // zeroes ANIMATOR_DURATION_SCALE) is honoured for free through Compose's MotionDurationScale
        // — the same flap resolves in ~20ms, an instant swap, with no code here. Verified, not assumed.
        progress.animateTo(0.5f, ApexMotion.exit())
        progress.animateTo(1f, ApexMotion.enter())
    }

    Column(
        modifier = modifier.clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(metrics.seam),
    ) {
        Box {
            DigitHalf(shown, top = true, metrics, colors)
            DigitHalf(
                previous, top = true, metrics, colors,
                Modifier.graphicsLayer {
                    val p = (progress.value * 2f).coerceIn(0f, 1f)
                    rotationX = -90f * p
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    alpha = if (progress.value < 0.5f) 1f else 0f
                }
            )
        }
        Box {
            DigitHalf(previous, top = false, metrics, colors)
            DigitHalf(
                shown, top = false, metrics, colors,
                Modifier.graphicsLayer {
                    val p = ((progress.value - 0.5f) * 2f).coerceIn(0f, 1f)
                    rotationX = 90f * (1f - p)
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    alpha = if (progress.value >= 0.5f) 1f else 0f
                }
            )
        }
    }
}

/**
 * Half a card: a box of half the card height that draws the glyph as if the whole card were there,
 * then lets [clip] remove the half that does not belong to it.
 *
 * The glyph is drawn rather than composed. Handing an over-tall `Text` to an aligned `Box` and
 * relying on the overflow being clipped renders the *complete* digit in both halves — the child gets
 * sized to the half it sits in, so each half centres a whole digit and the card reads as the same
 * number stamped twice. Drawing it puts the vertical offset under direct control: the full card is
 * `size.height * 2` (taken from the drawn size, so the top and bottom halves are exactly
 * complementary and no Dp rounding can open a seam), the glyph is centred in that, and the bottom
 * half shifts up by its own height. This also matches how every other bespoke mark in the app is
 * made — ApexLogo, the heatmap cells and the trend charts are all hand-drawn.
 */
@Composable
private fun DigitHalf(
    digit: Char,
    top: Boolean,
    metrics: FlipMetrics,
    colors: FlipClockColors,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val layout = remember(measurer, digit, metrics.glyphStyle) {
        measurer.measure(
            AnnotatedString(digit.toString()),
            style = metrics.glyphStyle,
            maxLines = 1,
            softWrap = false,
        )
    }
    val shape = if (top) {
        RoundedCornerShape(topStart = ApexShapes.cell, topEnd = ApexShapes.cell)
    } else {
        RoundedCornerShape(bottomStart = ApexShapes.cell, bottomEnd = ApexShapes.cell)
    }
    Canvas(
        modifier
            .size(metrics.cardWidth, metrics.halfHeight)
            .clip(shape)
            .background(colors.card)
            // A stroke weight, not a layout dimension — the same category as apexMenuBorder()'s 1dp.
            // Load-bearing in the light theme, where PaperElevated on PaperBase is about 1.1:1 and
            // the card would otherwise have no edge at all.
            .border(HAIRLINE, colors.edge, shape)
    ) {
        val cardHeight = size.height * 2f
        val x = (size.width - layout.size.width) / 2f
        val yInCard = (cardHeight - layout.size.height) / 2f
        drawText(
            textLayoutResult = layout,
            color = colors.ink,
            topLeft = Offset(x, if (top) yInCard else yInCard - size.height),
        )
    }
}

/**
 * Scales the clock down to fit its parent. At 200% font scale [ApexNumerals.hero] is 84sp and a
 * six-card row runs past 500dp — wider than any phone. Scaling the drawn row keeps one type token
 * (rather than forking a second, smaller numeral style with two things to maintain) and keeps the
 * flap geometry in the coordinate space it was measured in. It is scale-down only, so rasterisation
 * stays crisp, and because the factor is recomputed each measure the hour field's arrival shrinks
 * the row continuously instead of in a step.
 */
@Composable
fun FlipClockFitToWidth(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val placeable = measurables.first().measure(Constraints())
        val scale = if (placeable.width > 0 && constraints.maxWidth < placeable.width) {
            constraints.maxWidth / placeable.width.toFloat()
        } else 1f
        layout((placeable.width * scale).roundToInt(), (placeable.height * scale).roundToInt()) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

// ── Internals ──────────────────────────────────────────────────────────────────────────────

private val HAIRLINE = 1.dp

@Immutable
private data class FlipClockColors(val card: Color, val edge: Color, val ink: Color)

@Immutable
private data class FlipMetrics(
    val cardWidth: Dp,
    val cardHeight: Dp,
    val halfHeight: Dp,
    val seam: Dp,
    val glyphStyle: TextStyle,
)

/**
 * Card geometry derived from the glyph itself rather than from a dp constant, so the clock survives
 * a 200% font scale. rememberTextMeasurer is already keyed on density and the font-family resolver,
 * so the remember re-measures when the font scale changes; Martian Mono is a bundled R.font, whose
 * default loading strategy is blocking, so the first measurement is the real one and not a
 * fallback-metric guess; and it is a monospace, so measuring "0" sizes every digit.
 *
 * SubcomposeLayout would also work but yields the size only inside the measure pass — too late for
 * four sibling layers that each need the half height as a layout constraint. Intrinsics cannot
 * express "split this in half and hand that half to three other composables".
 */
@Composable
private fun rememberFlipMetrics(style: TextStyle): FlipMetrics {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(measurer, style, density) {
        // ApexNumerals.hero tracks at -1.5sp. Tracking exists to tighten a *string*; a single glyph
        // alone in a card has no neighbour to tighten against, and the trailing advance would push
        // the digit off-centre by half the tracking (3dp at 200% scale, visibly asymmetric). Zeroed
        // here and only here — the rhythm between cards is supplied by the layout instead.
        val glyph = style.copy(letterSpacing = 0.sp)
        val measured = measurer.measure(
            AnnotatedString("0"),
            style = glyph,
            maxLines = 1,
            softWrap = false,
        )
        with(density) {
            val height = measured.size.height.toDp() + ApexSpacing.s * 2
            FlipMetrics(
                cardWidth = measured.size.width.toDp() + ApexSpacing.m * 2,
                cardHeight = height,
                halfHeight = height / 2,
                seam = ApexSpacing.hairline,
                glyphStyle = glyph,
            )
        }
    }
}

/**
 * What TalkBack reads. Rounded to minutes on purpose: a seconds-precise description would change
 * sixty times a minute and is unusable to listen to. Assembled here rather than taken as a parameter
 * so the clock can never be announced digit by digit. Deliberately **not** a liveRegion — this is
 * read when the element takes focus, never announced on every tick.
 */
@Composable
private fun spokenDuration(seconds: Long): String {
    val totalMinutes = (seconds.coerceAtLeast(0L) / 60).toInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    val minutes = pluralStringResource(R.plurals.duration_minutes, m, m)
    val elapsed = if (h > 0) {
        stringResource(R.string.duration_joined, pluralStringResource(R.plurals.duration_hours, h, h), minutes)
    } else minutes
    return stringResource(R.string.cd_study_elapsed, elapsed)
}

// ── Previews ───────────────────────────────────────────────────────────────────────────────

@Composable
private fun FlipClockGallery() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.padding(ApexSpacing.l),
            verticalArrangement = Arrangement.spacedBy(ApexSpacing.xl),
        ) {
            // A mid-flip state is deliberately not previewed: a preview renders one frame, so the
            // captured rotation would be non-deterministic and the screenshot baseline flaky.
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

@Preview(name = "Flip clock · dark", showBackground = true)
@Composable
private fun FlipClockDarkPreview() {
    ApexTrackerTheme(darkTheme = true) { FlipClockGallery() }
}

@Preview(name = "Flip clock · light", showBackground = true)
@Composable
private fun FlipClockLightPreview() {
    ApexTrackerTheme(darkTheme = false) { FlipClockGallery() }
}

@Preview(name = "Flip clock · dark 200% font", showBackground = true, fontScale = 2.0f)
@Composable
private fun FlipClockLargeFontPreview() {
    ApexTrackerTheme(darkTheme = true) { FlipClockGallery() }
}
