package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetWidgetSnapshotTest {
    private val august = YearMonth.of(2026, 8)

    private fun item(amount: Double, date: LocalDate = LocalDate.of(2026, 8, 5)) =
        BudgetItem(title = "Expense", amount = amount, date = date)

    @Test
    fun `snapshot without limit still reports this month's spend`() {
        val snapshot = budgetWidgetSnapshot(
            items = listOf(
                item(12.50),
                item(99.0, LocalDate.of(2026, 7, 31)),
                item(7.25)
            ),
            month = august,
            limit = null,
            currencyCode = "USD"
        )

        assertEquals(19.75, snapshot.spent, 0.0001)
        assertNull(snapshot.limitStatus)
    }

    @Test
    fun `snapshot delegates capped state to overall limit logic`() {
        val snapshot = budgetWidgetSnapshot(
            items = listOf(item(80.0), item(30.0)),
            month = august,
            limit = 100.0,
            currencyCode = "EUR"
        )

        val status = snapshot.limitStatus!!
        assertEquals(110.0, snapshot.spent, 0.0001)
        assertEquals(1f, status.fraction, 0.0001f)
        assertEquals(-10.0, status.remaining, 0.0001)
        assertTrue(status.isOver)
        assertEquals("EUR", snapshot.currencyCode)
    }

    @Test
    fun `exactly at limit is not over`() {
        val snapshot = budgetWidgetSnapshot(
            items = listOf(item(100.0)),
            month = august,
            limit = 100.0,
            currencyCode = "USD"
        )

        val status = snapshot.limitStatus!!
        assertFalse(status.isOver)
        assertEquals(0.0, status.remaining, 0.0001)
    }

    @Test
    fun `invalid currency safely falls back to USD`() {
        val snapshot = budgetWidgetSnapshot(emptyList(), august, null, "not-a-code")

        assertEquals(DEFAULT_CURRENCY_CODE, snapshot.currencyCode)
    }

    @Test
    fun `locked snapshot carries no figures at all`() {
        // Issue #187: the widget renders on the launcher, outside anything the module lock can
        // gate. Withholding the amounts must happen here, not in the layout — the snapshot is
        // handed to the widget host's process, so "present but not drawn" would still put the
        // numbers exactly where the lock is meant to keep them out of.
        val snapshot = budgetWidgetSnapshot(
            items = listOf(item(110.0)),
            month = august,
            limit = 100.0,
            currencyCode = "EUR",
            locked = true
        )

        assertTrue(snapshot.locked)
        assertEquals(0.0, snapshot.spent, 0.0001)
        assertNull(snapshot.limitStatus)
        // The currency code is not sensitive and the widget still needs it if the lock is lifted.
        assertEquals("EUR", snapshot.currencyCode)
    }

    @Test
    fun `unlocked is the default so existing callers are unchanged`() {
        assertFalse(budgetWidgetSnapshot(listOf(item(10.0)), august, null, "USD").locked)
    }
}
