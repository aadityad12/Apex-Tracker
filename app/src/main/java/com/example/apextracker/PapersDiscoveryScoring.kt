package com.example.apextracker

import java.time.LocalDate
import kotlin.random.Random

/**
 * Pure scoring/rotation logic for keyword-topic discovery — no Android, no Room, no network — so
 * every rule is unit-tested in PapersDiscoveryScoringTest (matches the DashboardScoring /
 * CategoryLimits convention). The ViewModel supplies [PaperTopic] rows pulled from Room;
 * everything here is a deterministic function of that data (aside from [bonusSlotTopics]'s
 * injected [Random], kept as a parameter precisely so tests can seed it).
 */

private const val NEUTRAL_ENGAGEMENT = 0.5
private const val BONUS_SLOT_COUNT = 2
private const val BONUS_WEIGHT_FLOOR = 0.1

/**
 * A topic's track record, blending how often you finish (vs abandon) its papers with how highly
 * you rate the ones you finish. 0.5 (neutral) with no outcomes yet, so a brand-new topic competes
 * fairly for bonus slots rather than starting at zero. Falls back to the read ratio alone when no
 * paper from this topic has been rated (rating is optional at completion time).
 */
fun topicEngagementScore(topic: PaperTopic): Double {
    val outcomes = topic.readCount + topic.abandonedCount
    if (outcomes == 0) return NEUTRAL_ENGAGEMENT
    val readRatio = topic.readCount.toDouble() / outcomes
    if (topic.ratingCount == 0) return readRatio
    val avgRating = topic.ratingSum.toDouble() / topic.ratingCount / 5.0
    return (readRatio + avgRating) / 2.0
}

/** Topics eligible for fetch rotation / pick-weighting — paused topics keep history but sit out. */
fun activeTopics(topics: List<PaperTopic>): List<PaperTopic> = topics.filter { it.pausedAt == null }

/**
 * The guaranteed-coverage slot: whichever active topic has gone longest without a real fetch
 * (never-checked topics sort first). Ties broken by id for determinism — matters mainly in tests.
 */
fun guaranteedSlotTopic(active: List<PaperTopic>): PaperTopic? =
    active.minWithOrNull(
        compareBy<PaperTopic, LocalDate?>(nullsFirst()) { it.lastCheckedDate }.thenBy { it.id }
    )

/**
 * Up to [count] more topics, weighted-random by [topicEngagementScore] with a floor so a
 * consistently-abandoned topic is never fully unreachable — "healthy rotation" means every topic
 * keeps some chance, not that poor performers vanish. [random] is a parameter (not a default of
 * Random.Default at every call site) so callers can seed it for deterministic tests.
 */
fun bonusSlotTopics(
    candidates: List<PaperTopic>,
    count: Int,
    random: Random
): List<PaperTopic> {
    val pool = candidates.toMutableList()
    val picked = mutableListOf<PaperTopic>()
    repeat(minOf(count, pool.size)) {
        val weights = pool.map { maxOf(topicEngagementScore(it), BONUS_WEIGHT_FLOOR) }
        val total = weights.sum()
        var roll = random.nextDouble() * total
        var index = 0
        for (i in weights.indices) {
            roll -= weights[i]
            if (roll <= 0.0) {
                index = i
                break
            }
            index = i
        }
        picked += pool.removeAt(index)
    }
    return picked
}

/**
 * Today's fetch plan: the guaranteed slot first (coverage), then up to [BONUS_SLOT_COUNT] more
 * chosen by engagement weight — at most 3 topics/day total (Q10). Empty when there are no active
 * topics. The ViewModel walks this list in order and stops early once the daily paper cap is hit,
 * so most days only the first one or two entries actually get queried.
 */
fun dailyTopicFetchPlan(topics: List<PaperTopic>, random: Random = Random.Default): List<PaperTopic> {
    val active = activeTopics(topics)
    if (active.isEmpty()) return emptyList()
    val guaranteed = guaranteedSlotTopic(active) ?: return emptyList()
    val remaining = active.filter { it.id != guaranteed.id }
    val bonus = bonusSlotTopics(remaining, BONUS_SLOT_COUNT, random)
    return listOf(guaranteed) + bonus
}

/** Three abandons in a row from one topic (never reset by a read) trips the mute-suggestion prompt. */
fun shouldPromptMute(topic: PaperTopic): Boolean =
    topic.pausedAt == null && topic.consecutiveAbandons >= 3

/** Shared with PapersLogic.dailyPick: a topic's score, or the neutral default off-topic/unresolved. */
internal fun engagementScoreFor(paper: Paper, topics: List<PaperTopic>): Double {
    if (paper.topicCloudId.isEmpty()) return NEUTRAL_ENGAGEMENT
    val topic = topics.firstOrNull { it.cloudId == paper.topicCloudId } ?: return NEUTRAL_ENGAGEMENT
    return topicEngagementScore(topic)
}
