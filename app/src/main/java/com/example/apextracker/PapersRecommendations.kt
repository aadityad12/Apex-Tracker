package com.example.apextracker

/**
 * Turning the reading log into examples for Semantic Scholar's recommendations API (Issue #150) —
 * the fourth discovery source, after manual adds, the seed list and topic search.
 *
 * Pure, like the rest of the Papers logic (`PapersLogic.kt`, `PapersDiscoveryScoring.kt`): there
 * is no local model here, only the choice of *which* papers to hand the API as evidence. That
 * choice is the whole feature — recommend from the wrong examples and the shelf is noise.
 */

/** Papers to recommend from, as Semantic Scholar paper ids. */
data class RecommendationExamples(
    val positive: List<String>,
    val negative: List<String>
)

/**
 * A paper counts as *liked* at 4–5 on the 1–5 signal. The memo's signal was added for exactly
 * this (Plan.md decision 4), so nothing else needs to be inferred from behaviour.
 */
const val RECOMMENDATION_POSITIVE_SIGNAL = 4

/** …and *disliked* at 1–2. A 3 is deliberately neither: it's the "fine, I suppose" rating. */
const val RECOMMENDATION_NEGATIVE_SIGNAL = 2

/**
 * How many rated papers it takes before the shelf appears.
 *
 * Two rather than one: a shelf headed "Because you read …" that is really "because you liked one
 * paper once" gives the user no way to tell a good recommendation from a coincidence, and the
 * API's own quality is visibly thinner from a single example. By the ~5 rated papers the issue
 * describes, a reader who likes anything at all has cleared this.
 */
const val MIN_POSITIVE_EXAMPLES = 2

/**
 * The API caps each example list, and older ratings describe a reader who has since moved on, so
 * only the most recent few are sent.
 */
const val MAX_EXAMPLES_PER_LIST = 10

/**
 * Splits the reading log into positive and negative examples, most recently finished first.
 *
 * Papers without an `s2Id` are dropped: the API identifies examples by Semantic Scholar id, and a
 * bundled seed has none. (They *could* be resolved lazily through `normalizePaperIdInput(url)` →
 * `fetchPaper`, but that spends a request per seed on the unauthenticated pool that already
 * rate-limits this feature — see `SemanticScholarRateLimitedException` — to add examples the user
 * never rated anyway.)
 *
 * ABANDONED papers are weak negatives whether or not they carry a rating: dropping a paper is a
 * judgement about it, just a less precise one than a 1.
 */
fun recommendationExamples(
    papers: List<Paper>,
    maxPerList: Int = MAX_EXAMPLES_PER_LIST
): RecommendationExamples {
    val usable = papers.filter { it.s2Id.isNotBlank() }
        .sortedWith(compareByDescending<Paper> { it.readDate ?: it.addedDate }.thenByDescending { it.id })
    val positive = usable.filter {
        it.status == PaperStatus.READ && (it.signal ?: 0) >= RECOMMENDATION_POSITIVE_SIGNAL
    }
    val negative = usable.filter {
        it.status == PaperStatus.ABANDONED ||
            (it.signal != null && it.signal in 1..RECOMMENDATION_NEGATIVE_SIGNAL)
    }
    return RecommendationExamples(
        positive = positive.take(maxPerList).map { it.s2Id },
        negative = negative.take(maxPerList).map { it.s2Id }
    )
}

/** Whether there is enough evidence to ask for recommendations at all. */
fun canRecommend(examples: RecommendationExamples): Boolean =
    examples.positive.size >= MIN_POSITIVE_EXAMPLES

/**
 * The recommended rows inside the reading queue, newest first — the shelf's contents.
 *
 * Recommendations are ordinary WANT rows (Plan.md decision 4), so `paperQueue`/`dailyPick` treat
 * them exactly like any other queued paper; the shelf is a *view* over the same rows, not a
 * separate holding pen. Which is why the queue section below has to exclude them — otherwise the
 * same paper appears twice on one screen.
 */
fun recommendedFromQueue(queue: List<Paper>): List<Paper> =
    queue.filter { it.source == PaperSource.RECOMMENDED }
        .sortedWith(compareByDescending<Paper> { it.addedDate }.thenByDescending { it.id })

/** The rest of the queue, for the section under the shelf. */
fun queueExcludingRecommendations(queue: List<Paper>): List<Paper> =
    queue.filterNot { it.source == PaperSource.RECOMMENDED }

/**
 * The papers the shelf names — the most recent liked ones, in the same order
 * [recommendationExamples] picked them, so the heading and the request agree about what "because
 * you read" refers to.
 */
fun recommendationBasis(papers: List<Paper>, limit: Int = 2): List<Paper> =
    papers.filter {
        it.s2Id.isNotBlank() &&
            it.status == PaperStatus.READ &&
            (it.signal ?: 0) >= RECOMMENDATION_POSITIVE_SIGNAL
    }
        .sortedWith(compareByDescending<Paper> { it.readDate ?: it.addedDate }.thenByDescending { it.id })
        .take(limit)
