package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetItemTest {

    private fun item(type: String = TransactionType.EXPENSE) =
        BudgetItem(title = "x", amount = 1.0, type = type)

    @Test
    fun `default type is EXPENSE so pre-Issue 218 constructors are unaffected`() {
        assertEquals(TransactionType.EXPENSE, BudgetItem(title = "x", amount = 1.0).type)
    }

    @Test
    fun `EXPENSE row isExpense`() {
        assertTrue(item(TransactionType.EXPENSE).isExpense)
    }

    @Test
    fun `INCOME row is not isExpense`() {
        assertFalse(item(TransactionType.INCOME).isExpense)
    }

    @Test
    fun `an unrecognized type reads as expense, not silently dropped`() {
        assertTrue(item("garbage").isExpense)
    }

    @Test
    fun `expensesOnly filters out income rows`() {
        val items = listOf(item(TransactionType.EXPENSE), item(TransactionType.INCOME), item(TransactionType.EXPENSE))
        assertEquals(2, items.expensesOnly().size)
        assertTrue(items.expensesOnly().all { it.isExpense })
    }
}
