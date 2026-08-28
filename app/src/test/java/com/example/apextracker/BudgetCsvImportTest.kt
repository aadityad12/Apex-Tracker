package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetCsvImportTest {

    // ── parseCsvRows ─────────────────────────────────────────────────────────

    @Test
    fun `plain rows split on commas and newlines`() {
        val rows = parseCsvRows("a,b,c\nd,e,f")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("d", "e", "f")), rows)
    }

    @Test
    fun `a quoted field can contain a comma`() {
        val rows = parseCsvRows("\"Coffee, tea\",5.0")
        assertEquals(listOf(listOf("Coffee, tea", "5.0")), rows)
    }

    @Test
    fun `a doubled quote inside a quoted field unescapes to one quote`() {
        val rows = parseCsvRows("\"Bob\"\"s coffee\",5.0")
        assertEquals(listOf(listOf("Bob\"s coffee", "5.0")), rows)
    }

    @Test
    fun `a quoted field can contain a literal newline`() {
        val rows = parseCsvRows("\"line1\nline2\",5.0\nnext,6.0")
        assertEquals(listOf(listOf("line1\nline2", "5.0"), listOf("next", "6.0")), rows)
    }

    @Test
    fun `a file with no trailing newline still parses the last row`() {
        val rows = parseCsvRows("a,b,c")
        assertEquals(listOf(listOf("a", "b", "c")), rows)
    }

    @Test
    fun `trailing blank lines are dropped`() {
        val rows = parseCsvRows("a,b,c\n\n")
        assertEquals(listOf(listOf("a", "b", "c")), rows)
    }

    @Test
    fun `carriage returns before a newline are swallowed`() {
        val rows = parseCsvRows("a,b\r\nc,d")
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), rows)
    }

    // ── parseBudgetCsv ───────────────────────────────────────────────────────

    private val header = "date,title,amount,type,category,description"

    @Test
    fun `header row is always skipped`() {
        val result = parseBudgetCsv("$header\n2026-07-13,Lunch,12.5,EXPENSE,Food,with friends")
        assertEquals(1, result.rows.size)
    }

    @Test
    fun `a well-formed row round-trips`() {
        val result = parseBudgetCsv("$header\n2026-07-13,Lunch,12.5,EXPENSE,Food,with friends")
        val row = result.rows.single()
        assertTrue(row.isValid)
        assertEquals(LocalDate.of(2026, 7, 13), row.date)
        assertEquals("Lunch", row.title)
        assertEquals(12.5, row.amount!!, 0.0001)
        assertEquals(TransactionType.EXPENSE, row.type)
        assertEquals("Food", row.categoryName)
        assertEquals("with friends", row.description)
    }

    @Test
    fun `INCOME type round-trips`() {
        val result = parseBudgetCsv("$header\n2026-08-01,Paycheck,2000.0,INCOME,,")
        assertEquals(TransactionType.INCOME, result.rows.single().type)
    }

    @Test
    fun `an unrecognized type falls back to EXPENSE rather than rejecting the row`() {
        val result = parseBudgetCsv("$header\n2026-08-01,Item,5.0,garbage,,")
        val row = result.rows.single()
        assertTrue(row.isValid)
        assertEquals(TransactionType.EXPENSE, row.type)
    }

    @Test
    fun `blank category and description are null-ish, not errors`() {
        val result = parseBudgetCsv("$header\n2026-08-01,Item,5.0,EXPENSE,,")
        val row = result.rows.single()
        assertTrue(row.isValid)
        assertEquals("", row.categoryName)
        assertNull(row.description)
    }

    @Test
    fun `an unreadable date is reported, not silently coerced`() {
        val result = parseBudgetCsv("$header\nnot-a-date,Item,5.0,EXPENSE,,")
        val row = result.rows.single()
        assertTrue(!row.isValid)
        assertTrue(row.error!!.contains("date"))
    }

    @Test
    fun `a missing title is reported`() {
        val result = parseBudgetCsv("$header\n2026-08-01,,5.0,EXPENSE,,")
        val row = result.rows.single()
        assertTrue(!row.isValid)
        assertTrue(row.error!!.contains("title"))
    }

    @Test
    fun `an unreadable amount is reported`() {
        val result = parseBudgetCsv("$header\n2026-08-01,Item,not-a-number,EXPENSE,,")
        val row = result.rows.single()
        assertTrue(!row.isValid)
        assertTrue(row.error!!.contains("amount"))
    }

    @Test
    fun `a negative amount is rejected — the entry dialog can never produce one`() {
        // Regression test for Issue #241: a negative CSV amount used to import cleanly even
        // though BudgetItemDialog's own input regex can't produce one, silently corrupting the
        // pie chart (backward arcs), trend chart (clamped to 0, hiding the month), and limits.
        val result = parseBudgetCsv("$header\n2026-08-01,Refund,-12.5,EXPENSE,,")
        val row = result.rows.single()
        assertTrue(!row.isValid)
        assertTrue(row.error!!.contains("positive"))
    }

    @Test
    fun `a zero amount is rejected`() {
        val result = parseBudgetCsv("$header\n2026-08-01,Item,0,EXPENSE,,")
        assertTrue(!result.rows.single().isValid)
    }

    @Test
    fun `too few columns is reported rather than throwing`() {
        val result = parseBudgetCsv("$header\n2026-08-01,Item")
        assertTrue(!result.rows.single().isValid)
    }

    @Test
    fun `valid and invalid partition correctly across multiple rows`() {
        val result = parseBudgetCsv(
            "$header\n" +
                "2026-08-01,Good,5.0,EXPENSE,,\n" +
                "bad-date,Bad,5.0,EXPENSE,,\n" +
                "2026-08-02,AlsoGood,10.0,INCOME,,"
        )
        assertEquals(2, result.valid.size)
        assertEquals(1, result.invalid.size)
    }

    @Test
    fun `a formula-neutralizer prefix from export is stripped on import`() {
        // csvEscape() prepends ' to a title/description starting with =, +, -, @ so a spreadsheet
        // never evaluates it as a formula; re-importing that same file must not keep the prefix.
        val result = parseBudgetCsv("$header\n2026-08-01,'=SUM(A1),5.0,EXPENSE,,'+note")
        val row = result.rows.single()
        assertEquals("=SUM(A1)", row.title)
        assertEquals("+note", row.description)
    }

    @Test
    fun `an empty file produces no rows`() {
        assertEquals(0, parseBudgetCsv("").rows.size)
    }

    @Test
    fun `a header-only file produces no rows`() {
        assertEquals(0, parseBudgetCsv(header).rows.size)
    }

    // ── resolveImportedCategoryId ────────────────────────────────────────────

    @Test
    fun `resolveImportedCategoryId matches case-insensitively`() {
        val categories = listOf(Category(id = 3, name = "Groceries", colorHex = "#000000"))
        assertEquals(3L, resolveImportedCategoryId("groceries", categories))
    }

    @Test
    fun `resolveImportedCategoryId is null for a blank name`() {
        assertNull(resolveImportedCategoryId("", listOf(Category(id = 1, name = "Food", colorHex = "#000000"))))
    }

    @Test
    fun `resolveImportedCategoryId is null when nothing matches`() {
        assertNull(resolveImportedCategoryId("Nonexistent", listOf(Category(id = 1, name = "Food", colorHex = "#000000"))))
    }

    @Test
    fun `resolveImportedCategoryId does not special-case the Subscriptions label`() {
        // -1L is a synthetic bucket, not a real category id — a literal "Subscriptions" export
        // label only resolves if the user happens to have a real category by that name.
        assertNull(resolveImportedCategoryId("Subscriptions", listOf(Category(id = 1, name = "Food", colorHex = "#000000"))))
    }

    // ── export/import round trip ─────────────────────────────────────────────

    @Test
    fun `buildBudgetCsv output round-trips through parseBudgetCsv`() {
        val categories = listOf(Category(id = 1, name = "Food", colorHex = "#000000"))
        val items = listOf(
            BudgetItem(title = "Lunch", amount = 12.5, description = "with friends", date = LocalDate.of(2026, 7, 13), categoryId = 1L),
            BudgetItem(title = "Paycheck", amount = 2000.0, date = LocalDate.of(2026, 8, 1), type = TransactionType.INCOME)
        )
        val csv = buildBudgetCsv(items, categories)
        val result = parseBudgetCsv(csv)

        assertEquals(2, result.valid.size)
        val (lunch, paycheck) = result.valid
        assertEquals("Lunch", lunch.title)
        assertEquals(12.5, lunch.amount!!, 0.0001)
        assertEquals("Food", lunch.categoryName)
        assertEquals(TransactionType.INCOME, paycheck.type)
        assertEquals(2000.0, paycheck.amount!!, 0.0001)
    }
}
