package com.example.apextracker.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * The widget colour set — a hand-kept mirror of `ApexPalette`'s GRAPHITE dark scheme.
 *
 * Glance runs in the launcher's process with its own composition runtime and cannot read the app's
 * Compose `ColorScheme`, so these values have to be duplicated. They live in one file rather than
 * per-widget so that "if ApexPalette changes, change these too" is a single edit; nothing enforces
 * the correspondence.
 *
 * The scheme is monochrome by design: [ink] is both the app's `primary` and its `onSurface`, and
 * the only hues are [positive] (Sage, the "met/on track" semantic) and [negative] (Crimson).
 */
internal object WidgetPalette {
    /** Frost — headline text and the monochrome "accent". */
    val ink = ColorProvider(Color(0xFFE9EBEE))
    /** Muted graphite — labels and secondary text. */
    val muted = ColorProvider(Color(0xFF9AA1A9))
    /** Graphite base — the widget background. */
    val background = ColorProvider(Color(0xFF0E0F11))
    /** Raised graphite — progress tracks and control chips. */
    val track = ColorProvider(Color(0xFF292D33))
    /** Sage — a met goal, an on-track figure. */
    val positive = ColorProvider(Color(0xFF6FA88C))
    /** Crimson — over a limit, overdue. */
    val negative = ColorProvider(Color(0xFFFFB4AB))
}
