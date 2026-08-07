package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class PapersDiscoveryScoringTest {

    private fun topic(
        id: Long,
        keyword: String = "k$id",
        paused: LocalDate? = null,
        lastChecked: LocalDate? = null,
        read: Int = 0,
        abandoned: Int = 0,
        ratingSum: Int = 0,
        ratingCount: Int = 0,
        consecutiveAbandons: Int = 0,
        cloudId: String = "cloud$id"
    ) = PaperTopic(
        id = id, field = "Computer Science", keyword = keyword, pausedAt = paused,
        lastCheckedDate = lastChecked, readCount = read, abandonedCount = abandoned,
        ratingSum = ratingSum, ratingCount = ratingCount, consecutiveAbandons = consecutiveAbandons,
        cloudId = cloudId
    )

    // --- topicEngagementScore ---

    @Test
    fun `engagement score is neutral with no outcomes yet`() {
        assertEquals(0.5, topicEngagementScore(topic(1)), 0.0001)
    }

    @Test
    fun `engagement score falls back to read ratio when nothing is rated`() {
        assertEquals(0.75, topicEngagementScore(topic(1, read = 3, abandoned = 1)), 0.0001)
    }

    @Test
    fun `engagement score blends read ratio and average rating when rated`() {
        // read ratio 1.0, avg rating 4/5 = 0.8 -> (1.0 + 0.8) / 2 = 0.9
        assertEquals(0.9, topicEngagementScore(topic(1, read = 2, abandoned = 0, ratingSum = 8, ratingCount = 2)), 0.0001)
    }

    @Test
    fun `all-abandoned topic scores at the bottom`() {
        assertEquals(0.0, topicEngagementScore(topic(1, read = 0, abandoned = 3)), 0.0001)
    }

    // --- activeTopics / guaranteedSlotTopic ---

    @Test
    fun `active topics excludes paused ones`() {
        val active = topic(1)
        val paused = topic(2, paused = LocalDate.of(2026, 8, 1))
        assertEquals(listOf(active), activeTopics(listOf(active, paused)))
    }

    @Test
    fun `guaranteed slot prefers never-checked topics over old ones`() {
        val neverChecked = topic(1, lastChecked = null)
        val checkedRecently = topic(2, lastChecked = LocalDate.of(2026, 8, 6))
        assertEquals(1L, guaranteedSlotTopic(listOf(checkedRecently, neverChecked))!!.id)
    }

    @Test
    fun `guaranteed slot prefers the least-recently-checked topic`() {
        val stale = topic(1, lastChecked = LocalDate.of(2026, 7, 1))
        val fresh = topic(2, lastChecked = LocalDate.of(2026, 8, 6))
        assertEquals(1L, guaranteedSlotTopic(listOf(fresh, stale))!!.id)
    }

    @Test
    fun `guaranteed slot is null with no active topics`() {
        assertNull(guaranteedSlotTopic(emptyList()))
    }

    // --- bonusSlotTopics ---

    @Test
    fun `bonus slots never exceed the candidate pool`() {
        val candidates = listOf(topic(1), topic(2))
        assertEquals(2, bonusSlotTopics(candidates, count = 5, random = Random(0)).size)
    }

    @Test
    fun `bonus slots pick distinct topics, no repeats`() {
        val candidates = listOf(topic(1), topic(2), topic(3))
        val picked = bonusSlotTopics(candidates, count = 3, random = Random(42))
        assertEquals(3, picked.map { it.id }.toSet().size)
    }

    @Test
    fun `a consistently-abandoned topic still has a nonzero chance via the weight floor`() {
        val strong = topic(1, read = 10, abandoned = 0)
        val weak = topic(2, read = 0, abandoned = 10)
        // Across many seeds the weak topic should still win at least once thanks to the floor.
        val everWon = (0 until 200).any { seed ->
            bonusSlotTopics(listOf(strong, weak), count = 1, random = Random(seed)).first().id == 2L
        }
        assertTrue(everWon)
    }

    // --- dailyTopicFetchPlan ---

    @Test
    fun `fetch plan is empty with no active topics`() {
        assertEquals(emptyList<PaperTopic>(), dailyTopicFetchPlan(emptyList(), Random(0)))
    }

    @Test
    fun `fetch plan is empty when every topic is paused`() {
        val paused = topic(1, paused = LocalDate.of(2026, 8, 1))
        assertEquals(emptyList<PaperTopic>(), dailyTopicFetchPlan(listOf(paused), Random(0)))
    }

    @Test
    fun `fetch plan caps at three topics and leads with the guaranteed slot`() {
        val topics = (1L..5L).map { topic(it, lastChecked = LocalDate.of(2026, 8, it.toInt())) }
        val plan = dailyTopicFetchPlan(topics, Random(1))
        assertEquals(3, plan.size)
        assertEquals(1L, plan.first().id) // oldest lastCheckedDate -> guaranteed slot
        assertEquals(3, plan.map { it.id }.toSet().size) // no duplicates
    }

    @Test
    fun `fetch plan with one topic is just that topic`() {
        val only = topic(1)
        assertEquals(listOf(only), dailyTopicFetchPlan(listOf(only), Random(0)))
    }

    // --- shouldPromptMute ---

    @Test
    fun `mute prompt trips at three consecutive abandons`() {
        assertFalse(shouldPromptMute(topic(1, consecutiveAbandons = 2)))
        assertTrue(shouldPromptMute(topic(1, consecutiveAbandons = 3)))
        assertTrue(shouldPromptMute(topic(1, consecutiveAbandons = 5)))
    }

    @Test
    fun `mute prompt never fires for an already-paused topic`() {
        assertFalse(shouldPromptMute(topic(1, paused = LocalDate.of(2026, 8, 1), consecutiveAbandons = 5)))
    }
}
