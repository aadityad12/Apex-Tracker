package com.example.apextracker.design

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.apextracker.BudgetItem
import com.example.apextracker.BudgetOverview
import com.example.apextracker.Category
import com.example.apextracker.LocalCurrencyCode
import com.example.apextracker.PALETTE
import com.example.apextracker.ui.design.ApexTrackerTheme
import java.time.LocalDate
import java.time.YearMonth

/**
 * Reference renders of the Budget screen — the densest surface in the app, and the one whose
 * previous shape (totals card → pie card → limits card → trend card → tinted row cards) the redesign
 * exists to remove.
 *
 * Rendering the whole [BudgetOverview] rather than its parts is deliberate: the failure modes worth
 * catching here are compositional. Whether the hero total, the donut, the limit bars and the
 * transaction rows read as one column with a rhythm — or as four unrelated blocks — is not visible in
 * any single component's baseline.
 *
 * A fixed month and fixed dates keep these from rotting overnight. The categories deliberately mix
 * palette hexes with **legacy** Google Calendar pastels, so the read-side mapping in
 * `CategoryPalette.kt` is covered: if it ever stops resolving, these baselines go pastel.
 */

private val MONTH: YearMonth = YearMonth.of(2026, 7)

private val categories = listOf(
    // Palette hexes, straight through.
    Category(id = 1, name = "Groceries", colorHex = PALETTE[5], monthlyLimit = 400.0),
    Category(id = 2, name = "Rent", colorHex = PALETTE[2]),
    // Legacy pastels, which must render as their mapped slots and never as themselves.
    Category(id = 3, name = "Coffee", colorHex = "#ff7537", monthlyLimit = 60.0),
    Category(id = 4, name = "Transport", colorHex = "#9fc6e7"),
    Category(id = 5, name = "Books", colorHex = "#a47ae2"),
    // The grey that the palette has no neutral for.
    Category(id = 6, name = "Misc", colorHex = "#c2c2c2")
)

private fun item(id: Long, title: String, amount: Double, day: Int, cat: Long?, desc: String? = null) =
    BudgetItem(
        id = id,
        title = title,
        amount = amount,
        description = desc,
        date = LocalDate.of(2026, 7, day),
        categoryId = cat
    )

private val items = listOf(
    item(1, "Rent", 1450.00, 1, 2),
    item(2, "Weekly shop", 128.40, 3, 1, "Includes the party stuff"),
    item(3, "Flat white", 5.25, 4, 3),
    item(4, "Bus pass", 62.00, 5, 4),
    item(5, "Gödel, Escher, Bach", 24.99, 8, 5),
    item(6, "Weekly shop", 96.10, 10, 1),
    item(7, "Batteries", 8.75, 12, 6),
    item(8, "Uncategorised thing", 31.00, 14, null)
)

@Composable
private fun Budget(searchQuery: String = "", overallLimit: Double? = 2200.0) {
    BudgetOverview(
        items = items,
        categories = categories,
        subscriptions = emptyList(),
        selectedMonth = MONTH,
        onMonthChange = {},
        onEdit = {},
        searchQuery = searchQuery,
        overallLimit = overallLimit
    )
}

@Composable
private fun Framed(dark: Boolean, content: @Composable () -> Unit) {
    ApexTrackerTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalCurrencyCode provides "USD") {
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) { content() }
        }
    }
}

@PreviewTest
@Preview(name = "budget-dark", widthDp = 400, heightDp = 1400)
@Composable fun BudgetDark() = Framed(dark = true) { Budget() }

@PreviewTest
@Preview(name = "budget-light", widthDp = 400, heightDp = 1400)
@Composable fun BudgetLight() = Framed(dark = false) { Budget() }

/**
 * 200% font scale. The layout must not clip: the hero total, the legend rows and the limit rows all
 * carry currency, which is the widest thing on the screen.
 */
@PreviewTest
@Preview(name = "budget-dark-font200", widthDp = 400, heightDp = 2400, fontScale = 2.0f)
@Composable fun BudgetDarkLargeFont() = Framed(dark = true) { Budget() }

/** The empty state — an invitation to act, not a blank plot. */
@PreviewTest
@Preview(name = "budget-empty-dark", widthDp = 400, heightDp = 500)
@Composable fun BudgetEmptyDark() = Framed(dark = true) {
    BudgetOverview(
        items = emptyList(),
        categories = categories,
        subscriptions = emptyList(),
        selectedMonth = MONTH,
        onMonthChange = {},
        onEdit = {},
        overallLimit = null
    )
}

/** The eight palette slots as the picker shows them, both themes, for contrast review. */
@Composable
private fun Swatches() {
    com.example.apextracker.ColorGrid(
        colors = PALETTE,
        selectedColor = PALETTE[2],
        onColorSelected = {}
    )
}

@PreviewTest
@Preview(name = "category-palette-dark", widthDp = 360)
@Composable fun CategoryPaletteDark() = Framed(dark = true) { Swatches() }

@PreviewTest
@Preview(name = "category-palette-light", widthDp = 360)
@Composable fun CategoryPaletteLight() = Framed(dark = false) { Swatches() }
