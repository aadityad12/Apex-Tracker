package com.example.apextracker

import com.example.apextracker.ui.design.flipClockGroups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the digit model behind the study timer's split-flap clock. A keying mistake here is
 * invisible in a static preview — it only shows up on device as the whole clock flipping at once —
 * so the key-stability cases below are the load-bearing ones.
 */
class FlipClockDigitsTest {

    private fun chars(seconds: Long) =
        flipClockGroups(seconds).flatten().joinToString("") { it.char.toString() }

    private fun keys(seconds: Long) = flipClockGroups(seconds).flatten().map { it.key }

    // ── the rendered figure ───────────────────────────────────────────────────

    @Test
    fun `renders zero-padded fields, dropping hours below an hour`() {
        // This is now the only definition of how the stopwatch reads — the old formatTime() string
        // it was cross-checked against is gone, since the clock draws digits rather than text.
        val expected = mapOf(
            0L to "0000",
            1L to "0001",
            59L to "0059",
            60L to "0100",
            61L to "0101",
            599L to "0959",
            3599L to "5959",
            3600L to "010000",
            3661L to "010101",
            35999L to "095959",
            359999L to "995959",
            360000L to "1000000",
        )
        expected.forEach { (seconds, digits) ->
            assertEquals("at $seconds seconds", digits, chars(seconds))
        }
    }

    // ── field structure ───────────────────────────────────────────────────────

    @Test
    fun `hour group is absent below an hour and present at an hour`() {
        assertEquals(2, flipClockGroups(3599L).size)
        assertEquals("5959", chars(3599L))
        assertEquals(3, flipClockGroups(3600L).size)
        assertEquals("010000", chars(3600L))
    }

    @Test
    fun `zero renders four slots and negatives coerce to zero`() {
        assertEquals("0000", chars(0L))
        assertEquals(listOf("m1", "m0", "s1", "s0"), keys(0L))
        assertEquals("0000", chars(-1L))
        assertEquals("0000", chars(Long.MIN_VALUE))
    }

    @Test
    fun `each group is two digits wide until a field overflows`() {
        assertEquals(listOf("h1", "h0", "m1", "m0", "s1", "s0"), keys(3600L))
        assertEquals(listOf("h2", "h1", "h0", "m1", "m0", "s1", "s0"), keys(360000L))
    }

    // ── key stability: what decides which cards flip ──────────────────────────

    @Test
    fun `minute and second slots keep their keys when the hour group appears`() {
        // Index-based keys would fail here: slot 0 means "minutes tens" at 3599 and "hours tens" at
        // 3600, so every card would flip on the hour instead of just the ones whose digit changed.
        val before = keys(3599L)
        val after = keys(3600L)
        listOf("m1", "m0", "s1", "s0").forEach {
            assertTrue("$it missing before the hour", before.contains(it))
            assertTrue("$it missing after the hour", after.contains(it))
        }
    }

    @Test
    fun `crossing one hundred hours adds a card without re-keying the existing ones`() {
        val before = keys(359999L)   // 99:59:59
        val after = keys(360000L)    // 100:00:00
        assertTrue(before.containsAll(listOf("h1", "h0")))
        assertTrue(after.containsAll(listOf("h2", "h1", "h0")))
        assertEquals(before.size + 1, after.size)
    }

    @Test
    fun `a one-second tick changes only the slots whose digit changed`() {
        fun changed(from: Long, to: Long): List<String> {
            val a = flipClockGroups(from).associate { it.first().key.first() to it }
            return flipClockGroups(to).flatten()
                .filter { slot ->
                    a[slot.key.first()]?.firstOrNull { it.key == slot.key }?.char != slot.char
                }
                .map { it.key }
        }
        assertEquals(listOf("s0"), changed(0L, 1L))            // 00:00 -> 00:01
        assertEquals(listOf("s1", "s0"), changed(9L, 10L))     // 00:09 -> 00:10
        assertEquals(listOf("m0", "s1", "s0"), changed(59L, 60L))  // 00:59 -> 01:00
    }
}
