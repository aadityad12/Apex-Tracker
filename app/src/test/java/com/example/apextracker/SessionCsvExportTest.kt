package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SessionCsvExportTest {
    @Test
    fun `study CSV has a stable header and one row per subject total`() {
        val sessions = listOf(
            StudySession(LocalDate.of(2026, 8, 6), "Math", 3_600),
            StudySession(LocalDate.of(2026, 8, 5), "", 90)
        )

        assertEquals(
            "date,subject,duration_seconds\n2026-08-06,Math,3600\n2026-08-05,,90",
            buildStudyCsv(sessions)
        )
    }

    @Test
    fun `study CSV applies RFC 4180 escaping to subject names`() {
        val sessions = listOf(
            StudySession(LocalDate.of(2026, 8, 6), "Writing, \"Drafts\"", 120)
        )

        assertEquals(
            "date,subject,duration_seconds\n2026-08-06,\"Writing, \"\"Drafts\"\"\",120",
            buildStudyCsv(sessions)
        )
    }

    @Test
    fun `screen time CSV has a stable header and stored millisecond totals`() {
        val sessions = listOf(
            ScreenTimeSession(LocalDate.of(2026, 8, 6), 7_200_000),
            ScreenTimeSession(LocalDate.of(2026, 8, 5), 0)
        )

        assertEquals(
            "date,duration_millis\n2026-08-06,7200000\n2026-08-05,0",
            buildScreenTimeCsv(sessions)
        )
    }

    @Test
    fun `empty exports contain their header`() {
        assertEquals("date,subject,duration_seconds", buildStudyCsv(emptyList()))
        assertEquals("date,duration_millis", buildScreenTimeCsv(emptyList()))
    }
}
