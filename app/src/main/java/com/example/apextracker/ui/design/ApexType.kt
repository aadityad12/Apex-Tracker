package com.example.apextracker.ui.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.apextracker.R

/**
 * Two faces, two jobs — the mono-led identity (Plan.md Phase 2, decision 9):
 *  - [MartianMono] the voice. Display, headlines, and every number a user reads as a quantity.
 *    One mono speaking in headlines and figures is what makes the app read as an instrument.
 *  - [Geist] everything conversational: body, labels, controls. Paragraphs must never read as
 *    a terminal, so the mono stays out of running text.
 *
 * Instrument Serif is retired — the owner's verdict on the previous identity was that the serif
 * (with the warm palette) read as Claude's. Geist Mono is retired with it: promoting Vercel's
 * mono to display duty would trade one association for another. Martian Mono was chosen against
 * IBM Plex Mono and Space Mono on the style plate — it is the only one of the three drawn as a
 * display face, and no major consumer app owns it.
 *
 * Statics, not the variable file, for the same `@ExperimentalTextApi` reason Geist ships static.
 * Martian is a *wide* mono, so the display sizes run smaller and tighter-tracked than the serif
 * scale they replace — a mono at 34sp occupies the visual space a serif needed 44sp for.
 */
val MartianMono = FontFamily(
    Font(R.font.martian_mono_regular, FontWeight.Normal),
    Font(R.font.martian_mono_medium, FontWeight.Medium)
)

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold)
)

/**
 * Numeric styles. Deliberately NOT part of [ApexTypography] — Material's type scale has no
 * slot that means "this is a quantity", and routing numbers through bodyLarge is how they end
 * up in the wrong face. Reach for these explicitly.
 */
object ApexNumerals {
    /** The stopwatch. One per screen, at most. */
    val hero = TextStyle(
        fontFamily = MartianMono, fontWeight = FontWeight.Medium,
        fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-1.5).sp
    )

    /** A headline statistic — today's total, month spend. */
    val large = TextStyle(
        fontFamily = MartianMono, fontWeight = FontWeight.Medium,
        fontSize = 23.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp
    )

    /** Values in list rows: currency amounts, durations. The workhorse. */
    val medium = TextStyle(
        fontFamily = MartianMono, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp
    )

    /** Chart axis labels, dense secondary figures. */
    val small = TextStyle(
        fontFamily = MartianMono, fontWeight = FontWeight.Normal,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.sp
    )
}

/**
 * Display/headline slots are Martian Mono; title/body/label are Geist. Sizes are NOT the serif
 * scale's — a wide mono needs fewer points and negative tracking to sit in the same layout
 * (the serif's 44/34/27 display run becomes 33/27/22).
 */
val ApexTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MartianMono, fontWeight = FontWeight.Normal,
        fontSize = 33.sp, lineHeight = 40.sp, letterSpacing = (-2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = MartianMono, fontWeight = FontWeight.Normal,
        fontSize = 27.sp, lineHeight = 34.sp, letterSpacing = (-1.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = MartianMono, fontWeight = FontWeight.Normal,
        fontSize = 22.sp, lineHeight = 29.sp, letterSpacing = (-1).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = MartianMono, fontWeight = FontWeight.Normal,
        fontSize = 20.sp, lineHeight = 27.sp, letterSpacing = (-0.75).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = MartianMono, fontWeight = FontWeight.Normal,
        fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = MartianMono, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.5).sp
    ),

    titleLarge = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Medium,
        fontSize = 19.sp, lineHeight = 25.sp, letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.sp
    ),
    // The ALL-CAPS section eyebrow. Stays Geist and quiet — restraint wins on chrome; the mono
    // is spent on headlines and figures, not on labels.
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
