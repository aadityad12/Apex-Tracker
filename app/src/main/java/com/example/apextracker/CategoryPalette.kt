package com.example.apextracker

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.min

/**
 * The budget-category colour palette, and the mapping that lets it apply to data written before it
 * existed.
 *
 * ## Why a mapping is needed at all
 *
 * The picker used to offer 24 swatches copied verbatim from Google Calendar's event palette
 * (`#ac725e`, `#d06b64`, `#f83a22`, `#9fc6e7`, …) — borrowed rather than designed, pastel, and most
 * of them below 3:1 against this app's near-black background. [PALETTE] replaces them with eight
 * hues validated for lightness band, chroma floor, CVD separation and contrast in both themes
 * (`Design.md` §6).
 *
 * Swapping the picker alone would not have been enough. Every `Category.colorHex` is persisted in
 * Room **and** mirrored to Firestore, so a new picker only changes what *new* categories get:
 * existing categories would stay pastel forever and the app would show two palettes at once,
 * permanently. Of the three ways out — map on read, migrate the data once, or accept the split —
 * this is the first. Stored values are never rewritten; [resolveCategoryHex] maps whatever is on
 * disk onto a palette slot at render time.
 *
 * The consequences, all deliberate:
 * - **Nothing is destroyed.** The original hex stays in Room and Firestore, so this is reversible by
 *   deleting one function, and an older build of the app installed on another device still renders
 *   its own colours correctly from the same rows.
 * - **The mapping is many-to-one.** 24 legacy swatches (in fact any hex, since cloud data is
 *   unvalidated) collapse onto 8 slots, so two categories that used to be distinguishable can end up
 *   sharing a colour. That is why every surface that uses category colour also carries the category's
 *   name as text — colour is never the only channel.
 * - **It must be deterministic.** The same stored hex always resolves to the same slot, so a category
 *   does not change colour between sessions or between devices.
 *
 * ## The metric
 *
 * Nearest slot by cylindrical HSV distance, hue-dominant. Hue is weighted far above saturation and
 * value because hue is what a user means by "my red category" — a plain RGB distance would drag the
 * whole pastel legacy set toward whichever palette entry happens to be lightest, which is how a
 * "nearest colour" mapping ends up making everything brown.
 *
 * Saturation and value still carry a little weight, and they earn it on one real case: `#ff7537`
 * (bright orange) and `#ac725e` (muted clay) sit ~7° apart in hue, but belong on
 * different slots — gold and brown respectively. Hue alone cannot separate them.
 */

/**
 * The palette, in the order `Design.md` §6 fixes. **Do not reorder.** The order is computed, not
 * aesthetic: it was chosen so adjacent pairs stay separable under deuteranopia (worst adjacent
 * ΔE 12.9; the naive ordering of the same eight hues scored 3.3). Re-run
 * `scripts/validate_palette.js` if you change it.
 */
val PALETTE: List<String> = listOf(
    "#C75C8A", // 0 pink
    "#C1832B", // 1 gold
    "#3E90C4", // 2 blue
    "#9A5F2A", // 3 brown
    "#A0509F", // 4 purple
    "#3FA47C", // 5 green
    "#5A62CC", // 6 indigo
    "#8E9432"  // 7 olive
)

/**
 * Spoken names for the eight slots.
 *
 * These are explicit rather than derived from [swatchHueOf], which classifies by hue band and would
 * call both slot 1 (`#C1832B`) and slot 3 (`#9A5F2A`) "Orange" — two swatches in one picker
 * announcing the same name is exactly the ambiguity Issue #107 set out to remove.
 */
@StringRes
val PALETTE_NAME_RES: List<Int> = listOf(
    R.string.color_name_pink,
    R.string.color_name_gold,
    R.string.color_name_blue,
    R.string.color_name_brown,
    R.string.color_name_purple,
    R.string.color_name_green,
    R.string.color_name_indigo,
    R.string.color_name_olive
)

/** Weights for the distance metric. Hue dominates; see the file header. */
private const val W_HUE = 1.0
private const val W_SAT = 0.18
private const val W_VAL = 0.18

/**
 * The slot an achromatic colour resolves to.
 *
 * The palette has no neutral — `Design.md` §6 requires eight chromatic hues with a chroma floor, so
 * a grey category cannot stay grey. Brown is the least saturated-reading of the eight and so the
 * least jarring home for the legacy `#c2c2c2` / `#cabdbf` swatches. Fixed rather than computed
 * because hue is undefined for these inputs and any "nearest hue" answer would be arbitrary.
 */
private const val ACHROMATIC_SLOT = 3

private val paletteHsv: List<Hsv> by lazy {
    // Every entry is a literal in this file, so a null here is a typo, not a runtime condition.
    PALETTE.map { requireNotNull(hsvOf(it)) { "Malformed palette hex: $it" } }
}

/** Circular hue distance in degrees, 0–180. */
private fun hueDistance(a: Double, b: Double): Double {
    val d = abs(a - b) % 360.0
    return min(d, 360.0 - d)
}

/**
 * The [PALETTE] index that [colorHex] resolves to, or null if the hex cannot be parsed at all.
 *
 * Pure and total for any parseable input, including hexes that were never in either palette —
 * `Category.colorHex` arrives from Firestore without validation, so this has to cope with anything.
 */
fun categorySlotOf(colorHex: String?): Int? {
    val hsv = hsvOf(colorHex) ?: return null
    if (hsv.isAchromatic) return ACHROMATIC_SLOT
    return paletteHsv.indices.minBy { i ->
        val p = paletteHsv[i]
        val h = hueDistance(hsv.hue, p.hue) / 180.0
        val s = hsv.saturation - p.saturation
        val v = hsv.value - p.value
        W_HUE * h * h + W_SAT * s * s + W_VAL * v * v
    }
}

/**
 * The palette hex that [colorHex] renders as. **The single read-side entry point** — every surface
 * that paints a category colour goes through this or [categoryColorOf], so the app can never show a
 * legacy pastel next to a palette hue.
 *
 * Unparseable input falls to [ACHROMATIC_SLOT] rather than propagating a null: a category with a
 * corrupt colour still has to render as something, and a fixed slot is more useful than grey.
 */
fun resolveCategoryHex(colorHex: String?): String = PALETTE[categorySlotOf(colorHex) ?: ACHROMATIC_SLOT]

/** [resolveCategoryHex] as a Compose [Color]. */
fun categoryColorOf(colorHex: String?): Color = parseColorSafe(resolveCategoryHex(colorHex))

/**
 * The colour to pre-select for a **new** category: the first palette slot no existing category
 * already resolves to, so two categories created in a row do not come out the same colour.
 *
 * [existingHexes] are raw stored values, resolved here — a legacy pastel already occupies the slot it
 * renders as, so it has to count against that slot.
 *
 * Falls back to slot 0 once all eight are taken. Collisions are fine past that point (there are only
 * eight colours and no limit on categories); what matters is not defaulting the first eight to one
 * colour, which is what the old picker did with its fixed `colors[15]`.
 */
fun nextCategoryHex(existingHexes: List<String?>): String {
    val taken = existingHexes.mapTo(mutableSetOf()) { resolveCategoryHex(it) }
    return PALETTE.firstOrNull { it !in taken } ?: PALETTE[0]
}
