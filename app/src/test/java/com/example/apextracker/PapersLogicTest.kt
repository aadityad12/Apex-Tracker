package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PapersLogicTest {

    private fun paper(
        id: Long,
        status: String = PaperStatus.WANT,
        added: LocalDate = LocalDate.of(2026, 7, 1),
        read: LocalDate? = null
    ) = Paper(id = id, title = "P$id", status = status, addedDate = added, readDate = read)

    @Test
    fun `queue is WANT only, oldest first`() {
        val papers = listOf(
            paper(1, added = LocalDate.of(2026, 7, 3)),
            paper(2, added = LocalDate.of(2026, 7, 1)),
            paper(3, status = PaperStatus.READ, read = LocalDate.of(2026, 7, 2))
        )
        assertEquals(listOf(2L, 1L), paperQueue(papers).map { it.id })
    }

    @Test
    fun `history is non-WANT, most recently read first`() {
        val papers = listOf(
            paper(1, status = PaperStatus.READ, read = LocalDate.of(2026, 7, 2)),
            paper(2, status = PaperStatus.READ, read = LocalDate.of(2026, 7, 5)),
            paper(3, status = PaperStatus.ABANDONED, added = LocalDate.of(2026, 7, 4)),
            paper(4)
        )
        assertEquals(listOf(2L, 3L, 1L), paperHistory(papers).map { it.id })
    }

    @Test
    fun `daily pick is null on empty queue`() {
        assertNull(dailyPick(emptyList(), emptyList()))
    }

    @Test
    fun `daily pick ties break to the oldest queued paper`() {
        // No topics at all -> every paper scores the neutral default, so the tie goes to
        // paperQueue's own oldest-first order.
        val queue = paperQueue(listOf(paper(1, added = LocalDate.of(2026, 7, 3)), paper(2, added = LocalDate.of(2026, 7, 1))))
        assertEquals(2L, dailyPick(queue, emptyList())!!.id)
    }

    @Test
    fun `daily pick prefers the paper from the higher-engagement topic`() {
        val good = PaperTopic(id = 1, field = "Computer Science", keyword = "diffusion", cloudId = "good", readCount = 4, abandonedCount = 0)
        val bad = PaperTopic(id = 2, field = "Computer Science", keyword = "obscure topic", cloudId = "bad", readCount = 0, abandonedCount = 4)
        val queue = paperQueue(
            listOf(
                paper(1, added = LocalDate.of(2026, 7, 1)).copy(topicCloudId = "bad"),
                paper(2, added = LocalDate.of(2026, 7, 5)).copy(topicCloudId = "good")
            )
        )
        assertEquals(2L, dailyPick(queue, listOf(good, bad))!!.id)
    }

    @Test
    fun `daily pick falls back to neutral score for an unresolved topic`() {
        val queue = paperQueue(listOf(paper(1).copy(topicCloudId = "deleted-topic")))
        assertEquals(1L, dailyPick(queue, emptyList())!!.id)
    }

    @Test
    fun `single-item queue is always picked`() {
        val queue = paperQueue(listOf(paper(7)))
        assertEquals(7L, dailyPick(queue, emptyList())!!.id)
    }

    @Test
    fun `papersReadByDate counts READ rows by readDate only`() {
        val d1 = LocalDate.of(2026, 7, 1)
        val d2 = LocalDate.of(2026, 7, 2)
        val papers = listOf(
            paper(1, status = PaperStatus.READ, read = d1),
            paper(2, status = PaperStatus.READ, read = d1),
            paper(3, status = PaperStatus.READ, read = d2),
            paper(4, status = PaperStatus.ABANDONED),
            paper(5) // WANT, no readDate
        )
        val byDate = papersReadByDate(papers)
        assertEquals(2, byDate[d1])
        assertEquals(1, byDate[d2])
        assertEquals(2, byDate.size)
    }

    @Test
    fun `signal clamps to 1-5 and preserves null`() {
        assertEquals(1, normalizeSignal(0))
        assertEquals(5, normalizeSignal(9))
        assertEquals(3, normalizeSignal(3))
        assertNull(normalizeSignal(null))
    }

    @Test
    fun `PAPERS auto goal met at or above threshold`() {
        val goal = Goal(
            name = "Read daily", type = GoalType.AUTO,
            metric = GoalMetric.PAPERS, comparator = GoalComparator.OVER, threshold = 1.0
        )
        assertTrue(evaluateAutoGoal(goal, DayMetrics(papersRead = 1)))
        assertTrue(evaluateAutoGoal(goal, DayMetrics(papersRead = 3)))
        assertEquals(false, evaluateAutoGoal(goal, DayMetrics(papersRead = 0)))
    }
}
