package com.example.apextracker.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.apextracker.Note
import com.example.apextracker.NoteRow
import com.example.apextracker.Recurrence
import com.example.apextracker.RecurrenceEndType
import com.example.apextracker.RecurrenceFrequency
import com.example.apextracker.Reminder
import com.example.apextracker.ReminderItemModern
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.ApexTrackerTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Reference renders of the two list-row anatomies (Notes and Reminders).
 *
 * These exist because verifying light mode for these screens through the emulator UI meant tapping
 * through a settings sheet on a CPU-starved AVD, which was both slow and unreliable. A preview pair
 * is deterministic, runs host-side, and keeps working as a regression net — which is strictly better
 * than a one-off screenshot.
 *
 * The rows are the interesting part: every state that carries colour (overdue, high priority,
 * completed, pinned) is rendered here so a semantic regression shows up as an image diff.
 */

private val fixedNow: LocalDateTime = LocalDateTime.of(2026, 7, 24, 1, 34)

private fun reminder(
    name: String,
    date: LocalDate,
    time: LocalTime? = LocalTime.of(9, 0),
    priority: String = "NORMAL",
    completed: Boolean = false,
    recurrence: Recurrence? = null
) = Reminder(
    id = 1,
    name = name,
    date = date,
    time = time,
    isCompleted = completed,
    recurrence = recurrence,
    occurrencesCompleted = 0,
    cloudId = "preview",
    modifiedAt = 0L,
    priority = priority
)

@Composable
private fun ReminderRows() {
    // A fixed "today" is passed via the isOverdue flag rather than LocalDate.now(), so these
    // baselines do not rot overnight. The row itself still formats "Today" from the real date, so
    // one row deliberately uses today's date to exercise that branch.
    Column(Modifier.fillMaxWidth().padding(ApexSpacing.l)) {
        ApexSectionHeader("Active")
        ReminderItemModern(
            reminder = reminder("Renew library books", LocalDate.of(2026, 7, 27)),
            isOverdue = true,
            onToggle = {}
        )
        ApexDivider()
        ReminderItemModern(
            reminder = reminder("Submit physics lab report", LocalDate.of(2026, 7, 29), LocalTime.of(21, 0), "HIGH"),
            isOverdue = false,
            onToggle = {}
        )
        ApexDivider()
        ReminderItemModern(
            reminder = reminder(
                "Weekly review", LocalDate.of(2026, 7, 31), LocalTime.of(18, 0), "LOW",
                recurrence = Recurrence(
                    frequency = RecurrenceFrequency.WEEKLY,
                    endType = RecurrenceEndType.NEVER
                )
            ),
            isOverdue = false,
            onToggle = {}
        )
        ApexDivider()
        ReminderItemModern(
            reminder = reminder("Book dentist", LocalDate.of(2026, 7, 28), completed = true),
            isOverdue = false,
            onToggle = {}
        )
    }
}

@Composable
private fun NoteRows() {
    Column(Modifier.fillMaxWidth().padding(ApexSpacing.l)) {
        ApexSectionHeader("Notes")
        NoteRow(
            note = Note(
                id = 1,
                title = "Reading list",
                content = "Gödel, Escher, Bach — chapter 3 onward. Also finish the shape-morphing article.",
                createdAt = fixedNow, modifiedAt = fixedNow, isPinned = true
            ),
            onClick = {}, onDelete = {}, onTogglePin = {}
        )
        ApexDivider()
        NoteRow(
            note = Note(
                id = 2, title = "Whiteboard", content = "",
                createdAt = fixedNow, modifiedAt = fixedNow, attachments = "shot1.png"
            ),
            onClick = {}, onDelete = {}, onTogglePin = {}
        )
        ApexDivider()
        NoteRow(
            note = Note(
                id = 3, title = "", content = "An untitled note, to check the placeholder title.",
                createdAt = fixedNow, modifiedAt = fixedNow
            ),
            onClick = {}, onDelete = {}, onTogglePin = {}
        )
    }
}

@Composable
private fun Framed(dark: Boolean, content: @Composable () -> Unit) {
    ApexTrackerTheme(darkTheme = dark) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) { content() }
    }
}

@PreviewTest
@Preview(name = "reminder-rows-dark", widthDp = 400)
@Composable fun ReminderRowsDark() = Framed(dark = true) { ReminderRows() }

@PreviewTest
@Preview(name = "reminder-rows-light", widthDp = 400)
@Composable fun ReminderRowsLight() = Framed(dark = false) { ReminderRows() }

@PreviewTest
@Preview(name = "reminder-rows-dark-font200", widthDp = 400, fontScale = 2.0f)
@Composable fun ReminderRowsDarkLargeFont() = Framed(dark = true) { ReminderRows() }

@PreviewTest
@Preview(name = "note-rows-dark", widthDp = 400)
@Composable fun NoteRowsDark() = Framed(dark = true) { NoteRows() }

@PreviewTest
@Preview(name = "note-rows-light", widthDp = 400)
@Composable fun NoteRowsLight() = Framed(dark = false) { NoteRows() }

@PreviewTest
@Preview(name = "note-rows-dark-font200", widthDp = 400, fontScale = 2.0f)
@Composable fun NoteRowsDarkLargeFont() = Framed(dark = true) { NoteRows() }
