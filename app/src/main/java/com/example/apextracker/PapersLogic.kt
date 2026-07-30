package com.example.apextracker

import java.time.LocalDate

/**
 * Pure queue/pick logic for the Papers feature — no Android, no Room — unit-tested in
 * PapersLogicTest, following the DashboardScoring/CategoryLimits convention.
 */

/** The reading queue: WANT papers, oldest first, stable across recompositions. */
fun paperQueue(papers: List<Paper>): List<Paper> =
    papers.filter { it.status == PaperStatus.WANT }
        .sortedWith(compareBy({ it.addedDate }, { it.id }))

/** Reading history: READ/ABANDONED, most recently finished first (unfinished sort by addedDate). */
fun paperHistory(papers: List<Paper>): List<Paper> =
    papers.filter { it.status != PaperStatus.WANT }
        .sortedWith(
            compareByDescending<Paper> { it.readDate ?: it.addedDate }.thenByDescending { it.id }
        )

/**
 * Today's pick: one queue item promoted per day, rotating deterministically so an unread pick
 * changes each day instead of nagging (Plan.md decision 5). Index is the epoch day modulo the
 * queue size — no stored state, no Random. Adding/removing queue items can move the pick
 * mid-day; accepted, the pick is an invitation rather than an assignment.
 */
fun dailyPick(queue: List<Paper>, today: LocalDate): Paper? {
    if (queue.isEmpty()) return null
    val index = (today.toEpochDay() % queue.size).toInt()
    return queue[index]
}

/** Papers marked read per day — the PAPERS goal metric's input (see DashboardScoring). */
fun papersReadByDate(papers: List<Paper>): Map<LocalDate, Int> =
    papers.filter { it.status == PaperStatus.READ && it.readDate != null }
        .groupingBy { it.readDate!! }
        .eachCount()

/** Clamps a signal rating to the valid 1..5 range; null stays null (unrated). */
fun normalizeSignal(signal: Int?): Int? = signal?.coerceIn(1, 5)
