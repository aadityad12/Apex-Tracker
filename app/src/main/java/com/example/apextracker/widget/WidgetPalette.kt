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
 * The scheme is **fully** monochrome as of 2026-08-11: [ink] is both the app's `primary` and its
 * `onSurface`, and there are no hues at all. The "met/on track" green and the over-limit red were
 * removed from the app; a widget that reintroduced either would be the one surface still showing
 * them. State is carried by the word next to the figure, not by colour.
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
    /** A met goal, a running timer — ink, same as any other emphasised figure. */
    val positive = ink
    /** Over a limit, overdue — also ink; the accompanying label is what says which. */
    val negative = ink
}
