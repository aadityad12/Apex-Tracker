package com.example.apextracker

import java.time.format.DateTimeFormatter

/** Pure Study history export: one row per date/subject total, with durations kept in stored seconds. */
fun buildStudyCsv(sessions: List<StudySession>): String {
    val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE
    val header = "date,subject,duration_seconds"
    val rows = sessions.map { session ->
        listOf(
            session.date.format(dateFormat),
            csvEscape(session.subject),
            session.durationSeconds.toString()
        ).joinToString(",")
    }
    return (listOf(header) + rows).joinToString("\n")
}

/** Pure Screen Time history export: one row per daily total, with durations kept in stored millis. */
fun buildScreenTimeCsv(sessions: List<ScreenTimeSession>): String {
    val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE
    val header = "date,duration_millis"
    val rows = sessions.map { session ->
        listOf(
            session.date.format(dateFormat),
            session.durationMillis.toString()
        ).joinToString(",")
    }
    return (listOf(header) + rows).joinToString("\n")
}
