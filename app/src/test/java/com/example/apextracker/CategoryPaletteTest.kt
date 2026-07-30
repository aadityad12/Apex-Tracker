package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The legacy → palette mapping (see [CategoryPalette]'s header for why it exists).
 *
 * The properties worth pinning are stability and hue preservation, not the exact slot for every one
 * of the 24 legacy swatches — over-specifying those would make the tests fail on any future tuning
 * of the weights without indicating a real regression. So the per-swatch assertions below check
 * *hue family*, which is the thing a user would notice changing.
 */
class CategoryPaletteTest {

    /** The 24 Google Calendar swatches the picker used to offer. */
    private val legacy = listOf(
        "#ac725e", "#d06b64", "#f83a22", "#fa573c", "#ff7537", "#ffad46",
        "#42d692", "#16a765", "#7bd148", "#b3dc6c", "#fbe983", "#fad165",
        "#92e1c0", "#9fe1e7", "#9fc6e7", "#4986e7", "#9a9cff", "#b99aff",
        "#c2c2c2", "#cabdbf", "#cca6ac", "#f691b2", "#cd74e6", "#a47ae2"
    )

    @Test
    fun `palette is eight entries with a name for each`() {
        assertEquals(8, PALETTE.size)
        assertEquals(PALETTE.size, PALETTE_NAME_RES.size)
        assertEquals("all palette entries distinct", PALETTE.size, PALETTE.toSet().size)
    }

    @Test
    fun `every palette entry resolves to itself`() {
        // Idempotence is the property that keeps colours stable: a category saved from the new
        // picker must not drift to a neighbouring slot when it is read back.
        PALETTE.forEachIndexed { i, hex ->
            assertEquals("slot $i", i, categorySlotOf(hex))
            assertEquals("slot $i", hex, resolveCategoryHex(hex))
        }
    }

    @Test
    fun `resolving is idempotent for arbitrary input`() {
        legacy.forEach { hex ->
            val once = resolveCategoryHex(hex)
            assertEquals("$hex must settle after one hop", once, resolveCategoryHex(once))
        }
    }

    @Test
    fun `every legacy swatch maps into the palette`() {
        legacy.forEach { hex ->
            val slot = categorySlotOf(hex)
            assertNotNull("$hex should map to a slot", slot)
            assertTrue("$hex slot in range", slot!! in PALETTE.indices)
        }
    }

    @Test
    fun `legacy swatches keep their hue family`() {
        // The user-visible promise: a reddish category stays reddish, a blue one stays blue. Asserted
        // as a family rather than an exact slot so tuning the weights does not fail this spuriously.
        fun familyOf(hex: String) = swatchHueOf(resolveCategoryHex(hex))

        // Reds and oranges land on warm slots (pink, gold or brown).
        listOf("#f83a22", "#fa573c", "#ff7537", "#ffad46", "#ac725e", "#d06b64").forEach {
            assertTrue(
                "$it -> ${resolveCategoryHex(it)} should stay warm",
                familyOf(it) in setOf(SwatchHue.RED, SwatchHue.ORANGE, SwatchHue.PINK)
            )
        }
        // Greens stay green.
        listOf("#42d692", "#16a765", "#7bd148", "#b3dc6c", "#92e1c0").forEach {
            assertTrue(
                "$it -> ${resolveCategoryHex(it)} should stay green",
                familyOf(it) in setOf(SwatchHue.GREEN, SwatchHue.TEAL, SwatchHue.YELLOW)
            )
        }
        // Blues stay blue.
        listOf("#9fc6e7", "#4986e7", "#9fe1e7").forEach {
            assertTrue(
                "$it -> ${resolveCategoryHex(it)} should stay blue",
                familyOf(it) in setOf(SwatchHue.BLUE, SwatchHue.TEAL)
            )
        }
        // Purples and pinks stay on the purple/pink end.
        listOf("#cd74e6", "#a47ae2", "#b99aff", "#9a9cff", "#f691b2").forEach {
            assertTrue(
                "$it -> ${resolveCategoryHex(it)} should stay purple or pink",
                familyOf(it) in setOf(SwatchHue.PURPLE, SwatchHue.PINK, SwatchHue.BLUE)
            )
        }
    }

    @Test
    fun `bright orange and muted clay separate`() {
        // The case that earns saturation and value a place in the metric: these two sit only a few
        // degrees apart in hue but read as completely different colours, so hue alone would collapse
        // them onto one slot.
        assertTrue(
            "#ff7537 and #ac725e should not share a slot",
            categorySlotOf("#ff7537") != categorySlotOf("#ac725e")
        )
    }

    @Test
    fun `greys resolve to the documented achromatic slot`() {
        // The palette has no neutral, so this is a deliberate choice rather than a nearest-hue
        // answer — pinned so it cannot change silently.
        listOf("#c2c2c2", "#000000", "#ffffff").forEach {
            assertEquals("$it", "#9A5F2A", resolveCategoryHex(it))
        }
    }

    @Test
    fun `unparseable input still renders as a colour`() {
        // colorHex arrives from Firestore unvalidated, so every read has to produce something.
        listOf(null, "", "not-a-colour", "#12345", "#GGGGGG").forEach {
            assertNull("$it should have no slot", categorySlotOf(it))
            assertTrue("$it should still resolve", resolveCategoryHex(it) in PALETTE)
        }
    }

    @Test
    fun `alpha-prefixed hexes are handled`() {
        assertEquals(categorySlotOf("#3E90C4"), categorySlotOf("#FF3E90C4"))
    }
}
