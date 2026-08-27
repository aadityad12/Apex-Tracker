package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Choosing which papers become recommendation evidence (Issue #150). */
class PapersRecommendationsTest {

    private val today = LocalDate.of(2026, 8, 10)

    private fun paper(
        id: Long,
        s2Id: String,
        status: String = PaperStatus.READ,
        signal: Int? = null,
        readDate: LocalDate? = today,
        source: String = PaperSource.MANUAL,
        title: String = "Paper $id"
    ) = Paper(
        id = id,
        s2Id = s2Id,
        title = title,
        source = source,
        status = status,
        addedDate = today.minusDays(30),
        readDate = if (status == PaperStatus.READ) readDate else null,
        signal = signal
    )

    // --- example selection --------------------------------------------------

    @Test
    fun `papers rated four and five are positive examples`() {
        val examples = recommendationExamples(
            listOf(
                paper(1, "a", signal = 5),
                paper(2, "b", signal = 4),
                paper(3, "c", signal = 3)
            )
        )
        assertEquals(setOf("a", "b"), examples.positive.toSet())
    }

    @Test
    fun `papers rated one and two are negative examples, three is neither`() {
        val examples = recommendationExamples(
            listOf(
                paper(1, "a", signal = 1),
                paper(2, "b", signal = 2),
                paper(3, "c", signal = 3),
                paper(4, "d", signal = 5)
            )
        )
        assertEquals(setOf("a", "b"), examples.negative.toSet())
        assertEquals(listOf("d"), examples.positive)
    }

    @Test
    fun `abandoned papers are negative examples even when unrated`() {
        val examples = recommendationExamples(
            listOf(paper(1, "a", status = PaperStatus.ABANDONED, signal = null))
        )
        assertEquals(listOf("a"), examples.negative)
        assertTrue(examples.positive.isEmpty())
    }

    @Test
    fun `seeds without an s2Id are skipped entirely`() {
        // The API identifies examples by Semantic Scholar id; a bundled seed has none, and
        // resolving one would cost a request on the pool that already rate-limits this feature.
        val examples = recommendationExamples(
            listOf(
                paper(1, "", signal = 5, source = PaperSource.SEED),
                paper(2, "", signal = 1, source = PaperSource.SEED),
                paper(3, "keep", signal = 5)
            )
        )
        assertEquals(listOf("keep"), examples.positive)
        assertTrue(examples.negative.isEmpty())
    }

    @Test
    fun `queued papers are not examples — nothing has been judged yet`() {
        val examples = recommendationExamples(
            listOf(paper(1, "a", status = PaperStatus.WANT, signal = null))
        )
        assertTrue(examples.positive.isEmpty())
        assertTrue(examples.negative.isEmpty())
    }

    @Test
    fun `only the most recent examples are sent, newest first`() {
        val papers = (1L..5L).map { i ->
            paper(i, "s$i", signal = 5, readDate = today.minusDays(6 - i))
        }
        val examples = recommendationExamples(papers, maxPerList = 2)
        assertEquals(listOf("s5", "s4"), examples.positive)
    }

    // --- readiness ---------------------------------------------------------

    @Test
    fun `one liked paper is not enough to fill a shelf`() {
        assertFalse(canRecommend(recommendationExamples(listOf(paper(1, "a", signal = 5)))))
    }

    @Test
    fun `two liked papers are enough`() {
        val examples = recommendationExamples(listOf(paper(1, "a", signal = 5), paper(2, "b", signal = 4)))
        assertTrue(canRecommend(examples))
    }

    @Test
    fun `negatives alone can never trigger a request`() {
        // The API needs something to recommend *from*; dislikes only steer.
        val examples = recommendationExamples(
            listOf(paper(1, "a", signal = 1), paper(2, "b", status = PaperStatus.ABANDONED))
        )
        assertFalse(canRecommend(examples))
    }

    // --- shelf partitioning ------------------------------------------------

    @Test
    fun `the shelf and the queue section never show the same paper`() {
        val queue = listOf(
            paper(1, "a", status = PaperStatus.WANT, source = PaperSource.RECOMMENDED),
            paper(2, "b", status = PaperStatus.WANT, source = PaperSource.DAILY),
            paper(3, "c", status = PaperStatus.WANT, source = PaperSource.MANUAL)
        )
        assertEquals(listOf(1L), recommendedFromQueue(queue).map { it.id })
        assertEquals(listOf(2L, 3L), queueExcludingRecommendations(queue).map { it.id })
    }

    @Test
    fun `queueRestExcludingTodayPick also drops a non-recommended pick`() {
        // Issue #210: dailyPick can choose a SEED/DAILY/manually-added paper, not just a
        // RECOMMENDED one — queueExcludingRecommendations alone left it rendering twice.
        val queue = listOf(
            paper(1, "a", status = PaperStatus.WANT, source = PaperSource.RECOMMENDED),
            paper(2, "b", status = PaperStatus.WANT, source = PaperSource.DAILY),
            paper(3, "c", status = PaperStatus.WANT, source = PaperSource.MANUAL)
        )
        assertEquals(listOf(3L), queueRestExcludingTodayPick(queue, todayPickId = 2L).map { it.id })
    }

    @Test
    fun `queueRestExcludingTodayPick still drops recommended rows when the pick is one of them`() {
        val queue = listOf(
            paper(1, "a", status = PaperStatus.WANT, source = PaperSource.RECOMMENDED),
            paper(2, "b", status = PaperStatus.WANT, source = PaperSource.DAILY)
        )
        assertEquals(listOf(2L), queueRestExcludingTodayPick(queue, todayPickId = 1L).map { it.id })
    }

    @Test
    fun `queueRestExcludingTodayPick with no pick behaves like queueExcludingRecommendations`() {
        val queue = listOf(
            paper(1, "a", status = PaperStatus.WANT, source = PaperSource.RECOMMENDED),
            paper(2, "b", status = PaperStatus.WANT, source = PaperSource.DAILY)
        )
        assertEquals(listOf(2L), queueRestExcludingTodayPick(queue, todayPickId = null).map { it.id })
    }

    // --- heading basis -----------------------------------------------------

    @Test
    fun `the heading names the most recently liked papers`() {
        val papers = listOf(
            paper(1, "a", signal = 5, readDate = today.minusDays(9), title = "Old favourite"),
            paper(2, "b", signal = 5, readDate = today, title = "Just finished"),
            paper(3, "c", signal = 2, readDate = today, title = "Disliked")
        )
        assertEquals(listOf("Just finished", "Old favourite"), recommendationBasis(papers).map { it.title })
    }

    @Test
    fun `the heading basis is empty when nothing has been liked`() {
        assertTrue(recommendationBasis(listOf(paper(1, "a", signal = 2))).isEmpty())
    }
}
