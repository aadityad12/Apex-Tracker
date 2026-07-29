package com.example.apextracker.ui.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The spacing scale. Every layout modifier reads from here; a raw `.dp` literal in a padding
 * or spacedBy is a bug. Six steps is enough — the previous ad-hoc set (1, 2, 4, 8, 12, 16,
 * 20, 24, 32 dp, chosen per call site) is what "no rhythm" looks like in practice.
 */
object ApexSpacing {
    val hairline = 2.dp   // optical nudges only
    val xs = 4.dp         // inside a control
    val s = 8.dp          // between tightly-related elements
    val m = 12.dp         // row internal padding
    val l = 16.dp         // screen gutter, between rows
    val xl = 24.dp        // between sections
    val xxl = 40.dp       // above a screen's first section, below its last
}

/**
 * Three radii and a sheet. A fourth needs a written reason.
 * Note how small these are relative to the old 16/20/24/28dp set — heavy rounding on every
 * container is a large part of the generated-dashboard signature.
 */
object ApexShapes {
    val cell = 3.dp        // heatmap cells, swatches, chart bars
    val control = 9.dp     // buttons, chips, inputs, small tappables
    val container = 14.dp  // grouped content that genuinely lifts
    val sheet = 26.dp      // bottom sheets and dialogs only

    val material = Shapes(
        extraSmall = RoundedCornerShape(cell),
        small = RoundedCornerShape(control),
        medium = RoundedCornerShape(container),
        large = RoundedCornerShape(container),
        extraLarge = RoundedCornerShape(sheet)
    )
}

/**
 * Named motion. Every animation in the app references one of these; a raw `spring()` or
 * `tween()` at a call site is a bug, for the same reason a raw hex is.
 *
 * The distinction that matters: [snap] and [settle] are physical (they respond to a gesture
 * and can be interrupted), [enter] and [exit] are choreographic (they play once, on arrival
 * or dismissal). [emphasis] is the only loose one, reserved for a moment worth noticing —
 * a goal completing, a streak advancing. It is not a default.
 */
object ApexMotion {
    /** Press/release feedback. Fast, barely overshoots. */
    fun <T> snap(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessHigh)

    /** A value settling into place: a bar growing, a row reordering. */
    fun <T> settle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)

    /** Content arriving. Decelerates — it comes from somewhere and stops. */
    fun <T> enter(): FiniteAnimationSpec<T> = tween(280, easing = EmphasizedDecelerate)

    /** Content leaving. Shorter than enter — departures should not be dwelt on. */
    fun <T> exit(): FiniteAnimationSpec<T> = tween(180, easing = EmphasizedAccelerate)

    /** A moment worth noticing. Visible overshoot. Use sparingly and never ambiently. */
    fun <T> emphasis(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)

    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

/** Semantic colours Material's ColorScheme has no slot for. */
data class ApexSemantics(
    val positive: Color,
    val heatRamp: List<Color>
)

val LocalApexSemantics = staticCompositionLocalOf {
    ApexSemantics(positive = SageDark, heatRamp = ApexHeatRampDark)
}

/**
 * The app's accent identity.
 *
 * One entry, deliberately. The app used to ship four interchangeable accents
 * (EMERALD/OCEAN/MAGMA/ROYAL), which is customisation theatre: four accents means no identity.
 * The enum survives rather than being inlined so a *second, fully-authored* identity is a
 * drop-in — a monochrome + semantic-only theme is the planned next one. A single entry also
 * means the settings picker disappears on its own, with no dead code left behind.
 *
 * `FirebaseManager` still round-trips this by name. Legacy values ("EMERALD" etc.) written by
 * older builds fail `valueOf` and are swallowed at the call site in `MainActivity`, which is
 * the correct behaviour — an unknown identity falls back to the default.
 */
enum class ApexTheme { EMBER }

@Composable
fun ApexTrackerTheme(
    theme: ApexTheme = ApexTheme.EMBER,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = when (theme) {
        ApexTheme.EMBER -> if (darkTheme) ApexDarkColors else ApexLightColors
    }
    val semantics = when (theme) {
        ApexTheme.EMBER -> ApexSemantics(
            positive = if (darkTheme) SageDark else SageLight,
            heatRamp = if (darkTheme) ApexHeatRampDark else ApexHeatRampLight
        )
    }
    CompositionLocalProvider(LocalApexSemantics provides semantics) {
        MaterialTheme(
            colorScheme = colors,
            typography = ApexTypography,
            shapes = ApexShapes.material,
            content = content
        )
    }
}
