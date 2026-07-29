package com.example.apextracker.ui.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.apextracker.R

/**
 * Three faces, three jobs (see .claude/skills/android-product-design):
 *  - [InstrumentSerif] display only, >= 20sp. One weight exists — never fake bold.
 *  - [Geist] all UI text.
 *  - [GeistMono] every number the user reads as a quantity. Monospaced, so figures are
 *    inherently tabular and a changing value never reflows its neighbours.
 *
 * Geist is bundled as *static* weight instances rather than its variable file on purpose:
 * setting a `wght` axis in Compose requires `FontVariation.Settings`, which is
 * `@ExperimentalTextApi` in every overload. Statics cost ~340KB more and need no opt-in,
 * which is the trade this project chose when it ruled out experimental UI APIs.
 *
 * Only the weights the type scale actually uses are shipped. Adding a weight here means
 * adding a file; that friction is deliberate.
 */
val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic)
)

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold)
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium)
)

/**
 * Numeric styles. Deliberately NOT part of [ApexTypography] — Material's type scale has no
 * slot that means "this is a quantity", and routing numbers through bodyLarge is how they end
 * up in the wrong face. Reach for these explicitly.
 */
object ApexNumerals {
    /** The stopwatch. One per screen, at most. */
    val hero = TextStyle(
        fontFamily = GeistMono, fontWeight = FontWeight.Medium,
        fontSize = 52.sp, lineHeight = 56.sp, letterSpacing = (-1).sp
    )

    /** A headline statistic — today's total, month spend. */
    val large = TextStyle(
        fontFamily = GeistMono, fontWeight = FontWeight.Medium,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp
    )

    /** Values in list rows: currency amounts, durations. The workhorse. */
    val medium = TextStyle(
        fontFamily = GeistMono, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp
    )

    /** Chart axis labels, dense secondary figures. */
    val small = TextStyle(
        fontFamily = GeistMono, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp
    )
}

/**
 * Display slots are Instrument Serif; everything else is Geist. The uppercase section-header
 * convention the app already uses survives as [Typography.titleSmall], but quieter than the
 * old Black/2sp treatment — the serif now carries the emphasis that weight used to.
 */
val ApexTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
        fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
        fontSize = 27.sp, lineHeight = 34.sp, letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
        fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.sp
    ),

    titleLarge = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Medium,
        fontSize = 19.sp, lineHeight = 25.sp, letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.sp
    ),
    // The ALL-CAPS section eyebrow. Small, tracked, never bold — it labels, it does not shout.
    titleSmall = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.4.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 23.sp, letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.1.sp
    ),

    labelLarge = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.3.sp
    )
)
