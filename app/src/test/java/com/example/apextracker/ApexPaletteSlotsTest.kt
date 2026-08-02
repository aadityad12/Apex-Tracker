package com.example.apextracker

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.example.apextracker.ui.design.ApexDarkColors
import com.example.apextracker.ui.design.ApexLightColors
import com.example.apextracker.ui.design.Char
import com.example.apextracker.ui.design.CharDim
import com.example.apextracker.ui.design.Frost
import com.example.apextracker.ui.design.FrostDim
import com.example.apextracker.ui.design.GraphiteBase
import com.example.apextracker.ui.design.GraphiteElevated
import com.example.apextracker.ui.design.GraphiteLine
import com.example.apextracker.ui.design.GraphiteLineFaint
import com.example.apextracker.ui.design.GraphiteRaised
import com.example.apextracker.ui.design.GraphiteSurface
import com.example.apextracker.ui.design.PaperBase
import com.example.apextracker.ui.design.PaperElevated
import com.example.apextracker.ui.design.PaperLine
import com.example.apextracker.ui.design.PaperLineFaint
import com.example.apextracker.ui.design.PaperRaised
import com.example.apextracker.ui.design.PaperSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the rule in Design.md §8: **every `ColorScheme` slot the app can reach must be defined in
 * both schemes.** An undefined slot does not fall back to something neutral — `darkColorScheme()`
 * and `lightColorScheme()` fill it from Material's *baseline* palette, which is purple-tinted. That
 * failure is silent: nothing crashes, nothing warns, and it is invisible to a grep for
 * `colorScheme.` because the slot is reached through a component's own defaults.
 *
 * It has now happened twice — `tertiaryContainer` (the original Overview stat-card bug), and the
 * whole container ladder + snackbar trio found by the 2026-07-30 audit, which the graphite rewrite
 * (2026-07-30, same day) shipped *without*, reopening the same class of bug under new names. Hence
 * a test rather than a code comment.
 *
 * The reachable set comes from resolving each M3 component's default `*Tokens.ContainerColor` back
 * to its `ColorSchemeKeyTokens`. If a new component type is adopted, add its slots here.
 */
class ApexPaletteSlotsTest {

    /** Every hex in Design.md §3, per theme. A slot holding anything else is off-palette. */
    private val darkTokens = setOf(
        GraphiteBase, GraphiteSurface, GraphiteRaised, GraphiteElevated, GraphiteLine, GraphiteLineFaint,
        Frost, FrostDim,
        // The dark scheme's snackbar is an inverted (light) surface, so paper tones are legal there.
        PaperRaised, Char
    )

    private val lightTokens = setOf(
        PaperBase, PaperSurface, PaperRaised, PaperElevated, PaperLine, PaperLineFaint,
        Char, CharDim,
        // The light scheme's snackbar is an inverted (dark) surface.
        GraphiteRaised, GraphiteBase, Frost
    )

    /**
     * Slots reached only through a component's *defaults* — no call site mentions them, which is
     * exactly why they went undefined. Each is annotated with what reaches it.
     */
    private fun reachedByComponentDefaults(cs: ColorScheme): Map<String, Color> = mapOf(
        "surfaceContainer" to cs.surfaceContainer,              // DropdownMenu, scrolled TopAppBar, NavigationBar
        "surfaceContainerHigh" to cs.surfaceContainerHigh,      // AlertDialog, DatePickerDialog, TimePicker
        "surfaceContainerLow" to cs.surfaceContainerLow,        // ModalBottomSheet
        "surfaceContainerLowest" to cs.surfaceContainerLowest,
        "surfaceContainerHighest" to cs.surfaceContainerHighest, // Card
        "surfaceDim" to cs.surfaceDim,
        "surfaceBright" to cs.surfaceBright,
        "scrim" to cs.scrim,                                    // ModalBottomSheet scrim
        "inverseSurface" to cs.inverseSurface,                  // Snackbar container
        "inverseOnSurface" to cs.inverseOnSurface,              // Snackbar text
        "inversePrimary" to cs.inversePrimary,                  // Snackbar action label
        "tertiaryContainer" to cs.tertiaryContainer,
        "onTertiaryContainer" to cs.onTertiaryContainer
    )

    @Test
    fun `every component-default slot in the dark scheme is an authored token`() {
        reachedByComponentDefaults(ApexDarkColors).forEach { (name, color) ->
            assertTrue(
                "dark $name = ${color.hex()} is not a Design.md §3 token — it is almost certainly " +
                    "Material's baseline, which is purple-tinted",
                color in darkTokens
            )
        }
    }

    @Test
    fun `every component-default slot in the light scheme is an authored token`() {
        reachedByComponentDefaults(ApexLightColors).forEach { (name, color) ->
            assertTrue(
                "light $name = ${color.hex()} is not a Design.md §3 token — it is almost certainly " +
                    "Material's baseline, which is lavender",
                color in lightTokens
            )
        }
    }

    @Test
    fun `the two schemes define the same set of slots`() {
        // The light scheme was missing tertiaryContainer/onTertiaryContainer for a full release
        // cycle after the dark scheme gained them. Asymmetry is the bug.
        val dark = reachedByComponentDefaults(ApexDarkColors)
        val light = reachedByComponentDefaults(ApexLightColors)
        assertEquals(dark.keys, light.keys)
        // `scrim` is the one slot that is legitimately identical across themes: it is a cool
        // near-black under a sheet, and a sheet needs the same dimming behind it in light mode as
        // in dark.
        dark.filterKeys { it != "scrim" }.forEach { (name, darkColor) ->
            assertNotEquals(
                "$name is identical in both schemes — one of them was probably left at the default",
                darkColor,
                light.getValue(name)
            )
        }
    }

    @Test
    fun `surfaceTint is transparent so tonal elevation cannot drift surfaces toward ink`() {
        // M3 composites surfaceTint over surface at elevation. This design layers with authored
        // tones and hairlines (Design.md §5), so the tint must contribute nothing.
        assertEquals(Color.Transparent, ApexDarkColors.surfaceTint)
        assertEquals(Color.Transparent, ApexLightColors.surfaceTint)
    }

    @Test
    fun `menus and dialogs share a tone, which is what makes the menu hairline load-bearing`() {
        // apexMenuBorder() exists solely because of this. If a future change separates these two
        // tones, the border becomes optional — until then, removing it hides menus inside dialogs.
        assertEquals(ApexDarkColors.surfaceContainer, ApexDarkColors.surfaceContainerHigh)
        assertEquals(ApexLightColors.surfaceContainer, ApexLightColors.surfaceContainerHigh)
    }

    @Test
    fun `snackbar inverts against its own theme`() {
        // A snackbar that is not clearly inverted has fallen back to a normal surface.
        assertTrue(
            "dark theme's snackbar must be a light surface",
            ApexDarkColors.inverseSurface.luminanceApprox() > ApexDarkColors.surface.luminanceApprox()
        )
        assertTrue(
            "light theme's snackbar must be a dark surface",
            ApexLightColors.inverseSurface.luminanceApprox() < ApexLightColors.surface.luminanceApprox()
        )
    }

    private fun Color.luminanceApprox(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

    private fun Color.hex(): String =
        "#%02X%02X%02X".format((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
}
