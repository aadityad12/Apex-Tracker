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
 * Today's pick: the WANT-queue item whose topic has the best engagement track record (see
 * PapersDiscoveryScoring.topicEngagementScore) — ties (most papers have no topic, or an
 * unstarted one, and so share the neutral score) broken by [queue]'s own oldest-first order.
 *
 * No date parameter, unlike the old epoch-day rotation: this is a pure function of current queue
 * state. Marking the current pick READ/ABANDONED removes it from [queue] (paperQueue filters by
 * status), so the next call naturally promotes the next-best paper — same-day chaining falls out
 * of this for free. Adding/removing queue items can still move the pick mid-day; accepted, same
 * as before — the pick is an invitation, not an assignment.
 */
fun dailyPick(queue: List<Paper>, topics: List<PaperTopic>): Paper? {
    if (queue.isEmpty()) return null
    return queue.maxByOrNull { engagementScoreFor(it, topics) }
}

/** Papers marked read per day — the PAPERS goal metric's input (see DashboardScoring). */
fun papersReadByDate(papers: List<Paper>): Map<LocalDate, Int> =
    papers.filter { it.status == PaperStatus.READ && it.readDate != null }
        .groupingBy { it.readDate!! }
        .eachCount()

/** Clamps a signal rating to the valid 1..5 range; null stays null (unrated). */
fun normalizeSignal(signal: Int?): Int? = signal?.coerceIn(1, 5)
