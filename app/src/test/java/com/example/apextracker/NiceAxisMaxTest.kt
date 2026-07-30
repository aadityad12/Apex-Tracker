package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The duration-chart y-axis maximum (Study weekly chart, 2026-07-29 redesign). */
class NiceAxisMaxTest {

    @Test
    fun `is always strictly greater than the peak, so a bar never touches the top edge`() {
        listOf(0, 1, 9, 10, 59, 60, 61, 239, 240, 241, 1000).forEach { peak ->
            assertTrue("peak=$peak", niceAxisMaxMinutes(peak) > peak)
        }
    }

    @Test
    fun `rounds to ten-minute steps under an hour`() {
        assertEquals(10, niceAxisMaxMinutes(0))
        assertEquals(10, niceAxisMaxMinutes(3))
        assertEquals(20, niceAxisMaxMinutes(12))
        assertEquals(60, niceAxisMaxMinutes(59))
    }

    @Test
    fun `rounds to half-hour steps up to four hours`() {
        // The default 60-minute goal used to produce a max of exactly 60, putting the target line
        // on the top edge; it now lands at 90 with the line comfortably inside the plot.
        assertEquals(90, niceAxisMaxMinutes(60))
        assertEquals(120, niceAxisMaxMinutes(95))
        assertEquals(240, niceAxisMaxMinutes(239))
    }

    @Test
    fun `rounds to whole hours beyond four hours`() {
        assertEquals(300, niceAxisMaxMinutes(240))
        assertEquals(360, niceAxisMaxMinutes(301))
    }

    @Test
    fun `a negative peak cannot produce a zero-height axis`() {
        assertTrue(niceAxisMaxMinutes(-5) > 0)
    }
}
