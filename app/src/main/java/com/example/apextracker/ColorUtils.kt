package com.example.apextracker

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

/**
 * Parses a "#RRGGBB"/"#AARRGGBB" hex string into a Compose [Color], falling back to [fallback]
 * on malformed input. [Category.colorHex] can arrive via cloud sync with no validation, so any
 * render-time parse of it must be guarded.
 */
fun parseColorSafe(colorHex: String?, fallback: Color = Color.Gray): Color {
    if (colorHex == null) return fallback
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: IllegalArgumentException) {
        fallback
    }
}

/**
 * A colour in cylindrical HSV: [hue] in degrees 0–360, [saturation] and [value] in 0–1.
 *
 * Hue is meaningless for an achromatic colour, so [isAchromatic] is the caller's gate rather than a
 * magic saturation comparison repeated at each site. The threshold matches the one [swatchHueOf]
 * has always used.
 */
data class Hsv(val hue: Double, val saturation: Double, val value: Double) {
    val isAchromatic: Boolean get() = saturation < 0.18
}

/**
 * Parses "#RRGGBB"/"#AARRGGBB" into [Hsv], or null if it cannot be read.
 *
 * Deliberately hand-rolled rather than `android.graphics.Color.colorToHSV`: this is the shared
 * basis for both [swatchHueOf] and the category palette mapping, and both need to be unit-testable
 * on the JVM without Robolectric.
 */
fun hsvOf(colorHex: String?): Hsv? {
    val hex = colorHex?.removePrefix("#") ?: return null
    val rgb = when (hex.length) {
        6 -> hex
        8 -> hex.substring(2) // AARRGGBB
        else -> return null
    }
    val r = rgb.substring(0, 2).toIntOrNull(16) ?: return null
    val g = rgb.substring(2, 4).toIntOrNull(16) ?: return null
    val b = rgb.substring(4, 6).toIntOrNull(16) ?: return null

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = (max - min).toDouble()
    if (max == 0) return Hsv(0.0, 0.0, 0.0)

    val hue = if (delta == 0.0) 0.0 else when (max) {
        r -> 60.0 * (((g - b) / delta) % 6)
        g -> 60.0 * (((b - r) / delta) + 2)
        else -> 60.0 * (((r - g) / delta) + 4)
    }.let { if (it < 0) it + 360 else it }

    return Hsv(hue = hue, saturation = delta / max, value = max / 255.0)
}

/** Coarse colour families, enough to say aloud which swatch is which (Issue #107). */
enum class SwatchHue { RED, ORANGE, YELLOW, GREEN, TEAL, BLUE, PURPLE, PINK, GREY, UNKNOWN }

/**
 * Classifies a "#RRGGBB" swatch into a nameable hue family so the colour picker can carry a
 * spoken label instead of nothing. Pure (no android.graphics) so it can be unit-tested; low
 * saturation collapses to [SwatchHue.GREY] and unparseable input to [SwatchHue.UNKNOWN].
 */
fun swatchHueOf(colorHex: String?): SwatchHue {
    val hsv = hsvOf(colorHex) ?: return SwatchHue.UNKNOWN
    if (hsv.isAchromatic) return SwatchHue.GREY
    val hue = hsv.hue

    return when {
        hue < 15 -> SwatchHue.RED
        hue < 40 -> SwatchHue.ORANGE
        hue < 68 -> SwatchHue.YELLOW
        hue < 160 -> SwatchHue.GREEN
        hue < 195 -> SwatchHue.TEAL
        hue < 255 -> SwatchHue.BLUE
        hue < 295 -> SwatchHue.PURPLE
        hue < 350 -> SwatchHue.PINK
        else -> SwatchHue.RED
    }
}

/** Spoken name for a swatch hue, for contentDescription use. */
@StringRes
fun swatchHueLabelRes(hue: SwatchHue): Int = when (hue) {
    SwatchHue.RED -> R.string.color_name_red
    SwatchHue.ORANGE -> R.string.color_name_orange
    SwatchHue.YELLOW -> R.string.color_name_yellow
    SwatchHue.GREEN -> R.string.color_name_green
    SwatchHue.TEAL -> R.string.color_name_teal
    SwatchHue.BLUE -> R.string.color_name_blue
    SwatchHue.PURPLE -> R.string.color_name_purple
    SwatchHue.PINK -> R.string.color_name_pink
    SwatchHue.GREY -> R.string.color_name_grey
    SwatchHue.UNKNOWN -> R.string.color_name_other
}
