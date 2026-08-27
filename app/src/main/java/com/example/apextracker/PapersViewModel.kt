package com.example.apextracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID

data class PapersUiState(
    val todayPick: Paper?,
    /** Everything queued, recommendations included — what [dailyPick] weighs. */
    val queue: List<Paper>,
    /**
     * The queue minus the recommendation shelf *and* minus [todayPick], so no paper renders
     * twice on the screen (Issue #210). [dailyPick] can select any WANT-status paper — SEED,
     * DAILY, or manually-added, not just RECOMMENDED — so excluding the shelf alone isn't enough:
     * a non-recommended pick used to still show up again as the queue's first row.
     */
    val queueRest: List<Paper>,
    val recommendations: RecommendationShelf?,
    val history: List<Paper>,
    val loaded: Boolean
) {
    companion object {
        val EMPTY = PapersUiState(null, emptyList(), emptyList(), null, emptyList(), loaded = false)
    }
}

/** The add-by-link dialog's fetch lifecycle. */
sealed interface PaperFetchState {
    data object Idle : PaperFetchState
    data object Loading : PaperFetchState
    /** Metadata resolved; awaiting the user's confirm. */
    data class Preview(val paper: FetchedPaper) : PaperFetchState
    /** The paper is already in the log — surfaced instead of silently duplicating. */
    data class Duplicate(val existing: Paper) : PaperFetchState
    data class Error(val notFound: Boolean) : PaperFetchState
}

/** A day-level summary across however many topic slots ran — no per-topic naming (up to 3 now). */
sealed interface DailyPaperFeedState {
    data object Idle : DailyPaperFeedState
    data object Loading : DailyPaperFeedState
    data class Added(val count: Int) : DailyPaperFeedState
    data object NoResults : DailyPaperFeedState
    data object Unavailable : DailyPaperFeedState
    data object RateLimited : DailyPaperFeedState
}

/**
 * What the "Because you read …" shelf shows (#150). [basis] names the liked papers the request
 * was built from, so a recommendation is always attributable; [papers] are the resulting
 * RECOMMENDED queue rows.
 */
data class RecommendationShelf(
    val basis: List<Paper>,
    val papers: List<Paper>
)

/** Status of the recommendations request; only the non-Idle values are surfaced to the user. */
sealed interface RecommendationState {
    data object Idle : RecommendationState
    data object Loading : RecommendationState
    data object Unavailable : RecommendationState
    data object RateLimited : RecommendationState
}

/**
 * Papers reading log. Room is the source of truth, same as every other module; mutations are
 * mirrored to Firestore when signed in and safely remain local when offline or signed out.
 */
class PapersViewModel(application: Application) : AndroidViewModel(application) {

    private val paperDao = AppDatabase.getDatabase(application).paperDao()
    private val paperTopicDao = AppDatabase.getDatabase(application).paperTopicDao()
    private val paperLinkDao = AppDatabase.getDatabase(application).paperLinkDao()
    private val client = SemanticScholarClient()
    private val firebaseManager = FirebaseManager(application)
    private val discoveryPrefs = PapersDiscoveryPrefs(application)
    private val dailyFetchMutex = Mutex()

    val discoveryTopics: StateFlow<List<PaperTopic>> = paperTopicDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paperLinks: StateFlow<List<PaperLink>> = paperLinkDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<PapersUiState> = combine(paperDao.getAllPapers(), discoveryTopics) { papers, topics ->
        val queue = paperQueue(papers)
        val recommended = recommendedFromQueue(queue)
        val pick = dailyPick(queue, topics)
        PapersUiState(
            todayPick = pick,
            queue = queue,
            queueRest = queueRestExcludingTodayPick(queue, pick?.id),
            recommendations = if (recommended.isEmpty()) null else {
                RecommendationShelf(basis = recommendationBasis(papers), papers = recommended)
            },
            history = paperHistory(papers),
            loaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PapersUiState.EMPTY)

    private val _fetchState = MutableStateFlow<PaperFetchState>(PaperFetchState.Idle)
    val fetchState: StateFlow<PaperFetchState> = _fetchState

    val discoveryPreferences: StateFlow<PapersDiscoveryPreferences> = discoveryPrefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PapersDiscoveryPreferences())

    private val _dailyFeedState = MutableStateFlow<DailyPaperFeedState>(DailyPaperFeedState.Idle)
    val dailyFeedState: StateFlow<DailyPaperFeedState> = _dailyFeedState

    /** Set right after an abandon trips [shouldPromptMute]; null once dismissed or acted on. */
    private val _muteSuggestion = MutableStateFlow<PaperTopic?>(null)
    val muteSuggestion: StateFlow<PaperTopic?> = _muteSuggestion

    private val _recommendationState = MutableStateFlow<RecommendationState>(RecommendationState.Idle)
    val recommendationState: StateFlow<RecommendationState> = _recommendationState

    private val recommendationMutex = Mutex()

    init {
        viewModelScope.launch {
            // Also keyed on discoveryTopics: adding/resuming a topic must retrigger the check
            // immediately (not wait for tomorrow), same as the old setFields() did by clearing
            // lastFetchDate. Plain collect, not collectLatest: recordAttempt() and per-topic
            // lastCheckedDate writes both feed back into this combine, and collectLatest would
            // cancel an in-flight fetch on its own feedback emission and strand the UI in
            // Loading. The dailyFetchMutex plus the day-level gate keep the feedback benign — a
            // re-entrant trigger just re-checks shouldFetchDailyPapers and no-ops once it's false.
            combine(discoveryPrefs.preferences, discoveryTopics) { preferences, _ -> preferences }
                .collect { preferences ->
                    fetchDailyPapersIfNeeded(preferences)
                }
        }
        viewModelScope.launch {
            // Keyed on the papers table because that is what makes recommendations possible:
            // rating the second paper 4+ should fill the shelf that visit, not tomorrow's. Same
            // plain-collect + mutex + day-gate discipline as the topic fetch above, and for the
            // same reason — inserting the recommended rows feeds straight back into this flow.
            combine(discoveryPrefs.preferences, paperDao.getAllPapers()) { preferences, papers ->
                preferences to papers
            }.collect { (preferences, papers) ->
                fetchRecommendationsIfNeeded(preferences, papers)
            }
        }
    }

    /**
     * Asks Semantic Scholar for papers like the ones the reader finished and rated highly
     * (Issue #150), inserting them as RECOMMENDED WANT rows.
     *
     * Deliberately capped at [RECOMMENDATION_LIMIT] a day and gated on its own date marker: the
     * shelf is a suggestion, and a queue that grows faster than it is read stops being a queue.
     */
    private suspend fun fetchRecommendationsIfNeeded(
        preferences: PapersDiscoveryPreferences,
        papers: List<Paper>
    ) {
        recommendationMutex.withLock {
            val today = LocalDate.now()
            val now = System.currentTimeMillis()
            val examples = recommendationExamples(papers)
            if (!canRecommend(examples)) {
                _recommendationState.value = RecommendationState.Idle
                return
            }
            if (!shouldFetchRecommendations(preferences, today, now)) return

            _recommendationState.value = RecommendationState.Loading
            val result = client.fetchRecommendations(examples, RECOMMENDATION_LIMIT)
            val recommended = result.getOrNull()
            if (recommended == null) {
                val error = result.exceptionOrNull()
                if (error is SemanticScholarRateLimitedException) {
                    _recommendationState.value = RecommendationState.RateLimited
                    discoveryPrefs.recordRecommendationAttempt(
                        today,
                        semanticScholarBlockedUntil(error.retryAfterSeconds, now)
                    )
                } else {
                    // Network/server failure. Still counts as today's attempt, matching how the
                    // topic fetch treats a failed day — retrying every recomposition would hammer
                    // an endpoint that is already refusing us.
                    _recommendationState.value = RecommendationState.Unavailable
                    discoveryPrefs.recordRecommendationAttempt(today)
                }
                return
            }

            val added = mutableListOf<Paper>()
            recommended.forEach { candidate ->
                // Dedup against the whole log, not just the queue: something already read or
                // abandoned must not come back as a recommendation.
                if (added.size < RECOMMENDATION_LIMIT && paperDao.getByS2Id(candidate.s2Id) == null) {
                    val paper = Paper(
                        s2Id = candidate.s2Id,
                        title = candidate.title,
                        authors = candidate.authors,
                        year = candidate.year,
                        venue = candidate.venue,
                        abstractText = candidate.abstractText,
                        tldr = candidate.tldr,
                        url = candidate.url,
                        pdfUrl = candidate.pdfUrl,
                        source = PaperSource.RECOMMENDED,
                        addedDate = today,
                        cloudId = UUID.randomUUID().toString(),
                        modifiedAt = System.currentTimeMillis()
                    )
                    paperDao.insertPaper(paper)
                    added += paper
                }
            }
            _recommendationState.value = RecommendationState.Idle
            discoveryPrefs.recordRecommendationAttempt(today)
            viewModelScope.launch {
                added.forEach { paper ->
                    safeCloudCall(TAG, "pushPaper") { firebaseManager.pushPaper(paper) }
                }
            }
        }
    }

    /** Resolve pasted input to metadata for the add dialog's preview step. */
    fun fetchForAdd(input: String) {
        val normalized = normalizePaperIdInput(input)
        if (normalized == null) {
            _fetchState.value = PaperFetchState.Error(notFound = true)
            return
        }
        _fetchState.value = PaperFetchState.Loading
        viewModelScope.launch {
            client.fetchPaper(normalized)
                .onSuccess { fetched ->
                    val existing = paperDao.getByS2Id(fetched.s2Id)
                    _fetchState.value =
                        if (existing != null) PaperFetchState.Duplicate(existing)
                        else PaperFetchState.Preview(fetched)
                }
                .onFailure { e ->
                    _fetchState.value = PaperFetchState.Error(notFound = e is PaperNotFoundException)
                }
        }
    }

    fun resetFetch() {
        _fetchState.value = PaperFetchState.Idle
    }

    fun dismissOnboarding() {
        viewModelScope.launch { discoveryPrefs.dismissOnboarding() }
    }

    /** Adds a topic; silently refuses a blank keyword, an unknown field, or the 8-topic cap. */
    fun addTopic(field: String, keyword: String) {
        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.isEmpty() || field !in PAPER_DISCOVERY_FIELDS) return
        if (discoveryTopics.value.size >= MAX_PAPER_TOPICS) return
        viewModelScope.launch {
            val topic = PaperTopic(
                field = field,
                keyword = trimmedKeyword,
                createdDate = LocalDate.now(),
                cloudId = UUID.randomUUID().toString(),
                modifiedAt = System.currentTimeMillis()
            )
            paperTopicDao.insertTopic(topic)
            safeCloudCall(TAG, "pushPaperTopic") { firebaseManager.pushPaperTopic(topic) }
        }
    }

    /** Excludes the topic from fetch rotation and pick-weighting, keeping its history. Also the mute action. */
    fun pauseTopic(topic: PaperTopic) {
        viewModelScope.launch {
            val updated = topic.copy(pausedAt = LocalDate.now(), modifiedAt = System.currentTimeMillis())
            paperTopicDao.updateTopic(updated)
            safeCloudCall(TAG, "pushPaperTopic") { firebaseManager.pushPaperTopic(updated) }
        }
        if (_muteSuggestion.value?.id == topic.id) _muteSuggestion.value = null
    }

    fun resumeTopic(topic: PaperTopic) {
        viewModelScope.launch {
            val updated = topic.copy(pausedAt = null, modifiedAt = System.currentTimeMillis())
            paperTopicDao.updateTopic(updated)
            safeCloudCall(TAG, "pushPaperTopic") { firebaseManager.pushPaperTopic(updated) }
        }
    }

    fun deleteTopic(topic: PaperTopic) {
        viewModelScope.launch {
            paperTopicDao.deleteTopic(topic)
            safeCloudCall(TAG, "deletePaperTopic") { firebaseManager.deletePaperTopic(topic.cloudId) }
        }
    }

    fun dismissMuteSuggestion() {
        _muteSuggestion.value = null
    }

    /**
     * Links two papers (Issue #223). Silently refuses an invalid pair — see [canLinkPapers] — so
     * a stale picker row (the other paper deleted, or linked by another device since this list
     * loaded) can't insert garbage; the DAO's own [PaperLinkDao.findExisting] check backs this up
     * against a race the in-memory [canLinkPapers] check alone can't see.
     */
    fun addLink(paper: Paper, related: Paper) {
        if (!canLinkPapers(paper.cloudId, related.cloudId, paperLinks.value)) return
        viewModelScope.launch {
            if (paperLinkDao.findExisting(paper.cloudId, related.cloudId) != null) return@launch
            val link = PaperLink(
                paperCloudId = paper.cloudId,
                relatedPaperCloudId = related.cloudId,
                createdDate = LocalDate.now(),
                cloudId = UUID.randomUUID().toString(),
                modifiedAt = System.currentTimeMillis()
            )
            paperLinkDao.insertLink(link)
            safeCloudCall(TAG, "pushPaperLink") { firebaseManager.pushPaperLink(link) }
        }
    }

    fun removeLink(link: PaperLink) {
        viewModelScope.launch {
            paperLinkDao.deleteLink(link)
            safeCloudCall(TAG, "deletePaperLink") { firebaseManager.deletePaperLink(link.cloudId) }
        }
    }

    private suspend fun fetchDailyPapersIfNeeded(preferences: PapersDiscoveryPreferences) {
        dailyFetchMutex.withLock {
            val today = LocalDate.now()
            val now = System.currentTimeMillis()
            val active = activeTopics(paperTopicDao.getAllOneShot())
            if (active.isEmpty()) {
                _dailyFeedState.value = DailyPaperFeedState.Idle
                return
            }
            if (!shouldFetchDailyPapers(preferences, today, now)) return
            val plan = dailyTopicFetchPlan(active)
            _dailyFeedState.value = DailyPaperFeedState.Loading

            var totalInserted = 0
            var anyRequestSucceeded = false
            val allAdded = mutableListOf<Paper>()
            var rateLimited: SemanticScholarRateLimitedException? = null

            for (topic in plan) {
                if (totalInserted >= DAILY_TOTAL_CAP) break
                val result = client.searchRecent(topic.field, topic.keyword, today)
                val results = result.getOrNull()
                if (results == null) {
                    val error = result.exceptionOrNull()
                    if (error is SemanticScholarRateLimitedException) {
                        rateLimited = error
                        break
                    }
                    // Network/server failure for this slot: leave lastCheckedDate untouched so it
                    // stays "most overdue" and gets first crack at tomorrow's guaranteed slot,
                    // rather than being unfairly marked checked for a request that never landed.
                    continue
                }
                anyRequestSucceeded = true
                val slotBudget = minOf(PER_SLOT_LIMIT, DAILY_TOTAL_CAP - totalInserted)
                val slotAdded = mutableListOf<Paper>()
                results.forEach { candidate ->
                    if (slotAdded.size < slotBudget && paperDao.getByS2Id(candidate.s2Id) == null) {
                        val paper = Paper(
                            s2Id = candidate.s2Id,
                            title = candidate.title,
                            authors = candidate.authors,
                            year = candidate.year,
                            venue = candidate.venue,
                            abstractText = candidate.abstractText,
                            tldr = candidate.tldr,
                            url = candidate.url,
                            pdfUrl = candidate.pdfUrl,
                            source = PaperSource.DAILY,
                            addedDate = today,
                            topicCloudId = topic.cloudId,
                            cloudId = UUID.randomUUID().toString(),
                            modifiedAt = System.currentTimeMillis()
                        )
                        paperDao.insertPaper(paper)
                        slotAdded += paper
                    }
                }
                totalInserted += slotAdded.size
                allAdded += slotAdded
                paperTopicDao.updateTopic(topic.copy(lastCheckedDate = today, modifiedAt = System.currentTimeMillis()))
            }

            when {
                rateLimited != null -> {
                    _dailyFeedState.value = DailyPaperFeedState.RateLimited
                    discoveryPrefs.recordAttempt(
                        today,
                        semanticScholarBlockedUntil(rateLimited.retryAfterSeconds, now)
                    )
                }
                allAdded.isNotEmpty() -> {
                    _dailyFeedState.value = DailyPaperFeedState.Added(allAdded.size)
                    discoveryPrefs.recordAttempt(today)
                }
                anyRequestSucceeded -> {
                    _dailyFeedState.value = DailyPaperFeedState.NoResults
                    discoveryPrefs.recordAttempt(today)
                }
                else -> {
                    // Every slot's request failed at the network/HTTP level — quiet, still counts
                    // as today's single attempt (matches the pre-existing whole-day semantics).
                    _dailyFeedState.value = DailyPaperFeedState.Unavailable
                    discoveryPrefs.recordAttempt(today)
                }
            }
            // Room and the visible state complete first; cloud failures never hold back the feed.
            viewModelScope.launch {
                allAdded.forEach { paper ->
                    safeCloudCall(TAG, "pushPaper") { firebaseManager.pushPaper(paper) }
                }
            }
        }
    }

    /** Confirm the previewed paper into the queue. */
    fun addFetched(fetched: FetchedPaper) {
        viewModelScope.launch {
            if (paperDao.getByS2Id(fetched.s2Id) == null) {
                val paper = Paper(
                    s2Id = fetched.s2Id,
                    title = fetched.title,
                    authors = fetched.authors,
                    year = fetched.year,
                    venue = fetched.venue,
                    abstractText = fetched.abstractText,
                    tldr = fetched.tldr,
                    url = fetched.url,
                    pdfUrl = fetched.pdfUrl,
                    source = PaperSource.MANUAL,
                    addedDate = LocalDate.now(),
                    cloudId = UUID.randomUUID().toString(),
                    modifiedAt = System.currentTimeMillis()
                )
                paperDao.insertPaper(paper)
                safeCloudCall(TAG, "pushPaper") { firebaseManager.pushPaper(paper) }
            }
            _fetchState.value = PaperFetchState.Idle
        }
    }

    /** Import the bundled starter list, skipping any seed already present (by landing URL). */
    fun importSeeds() {
        viewModelScope.launch {
            val added = mutableListOf<Paper>()
            PAPER_SEEDS.forEach { seed ->
                if (paperDao.getByUrl(seed.url) == null) {
                    val paper = Paper(
                        title = seed.title,
                        authors = seed.authors,
                        year = seed.year,
                        venue = seed.venue,
                        tldr = seed.tldr,
                        url = seed.url,
                        pdfUrl = seed.pdfUrl,
                        source = PaperSource.SEED,
                        addedDate = LocalDate.now(),
                        cloudId = UUID.randomUUID().toString(),
                        modifiedAt = System.currentTimeMillis()
                    )
                    paperDao.insertPaper(paper)
                    added += paper
                }
            }
            // Complete the local import before touching the network so a signed-in but offline
            // user still gets the whole starter list immediately.
            added.forEach { paper ->
                safeCloudCall(TAG, "pushPaper") { firebaseManager.pushPaper(paper) }
            }
        }
    }

    /**
     * Finish a paper: status READ, readDate today (feeds the PAPERS goal metric), plus the
     * structured memo. Also the edit path for a paper already read — readDate is only set once.
     */
    fun markRead(paper: Paper, memo: String, signal: Int?) {
        val normalizedSignal = normalizeSignal(signal)
        viewModelScope.launch {
            val updated = paper.copy(
                status = PaperStatus.READ,
                readDate = paper.readDate ?: LocalDate.now(),
                memo = memo.trim(),
                signal = normalizedSignal,
                cloudId = paper.cloudId.ifEmpty { UUID.randomUUID().toString() },
                modifiedAt = System.currentTimeMillis()
            )
            paperDao.updatePaper(updated)
            safeCloudCall(TAG, "pushPaper") { firebaseManager.pushPaper(updated) }
            recordTopicOutcome(paper.topicCloudId, read = true, signal = normalizedSignal)
        }
    }

    /** Drop a paper from the queue without pretending it was read. Memo optional ("why I bailed"). */
    fun abandon(paper: Paper, memo: String) {
        viewModelScope.launch {
            val updated = paper.copy(
                status = PaperStatus.ABANDONED,
                memo = memo.trim(),
                cloudId = paper.cloudId.ifEmpty { UUID.randomUUID().toString() },
                modifiedAt = System.currentTimeMillis()
            )
            paperDao.updatePaper(updated)
            safeCloudCall(TAG, "pushPaper") { firebaseManager.pushPaper(updated) }
            recordTopicOutcome(paper.topicCloudId, read = false, signal = null)
        }
    }

    /**
     * Shared by markRead/abandon: updates the paper's origin topic's engagement counters (a no-op
     * for MANUAL/SEED papers or a since-deleted topic) and surfaces [muteSuggestion] when an
     * abandon trips the 3-in-a-row threshold (see shouldPromptMute in PapersDiscoveryScoring.kt).
     */
    private suspend fun recordTopicOutcome(topicCloudId: String, read: Boolean, signal: Int?) {
        if (topicCloudId.isEmpty()) return
        val topic = paperTopicDao.getByCloudId(topicCloudId) ?: return
        val updated = if (read) {
            topic.copy(
                readCount = topic.readCount + 1,
                ratingSum = topic.ratingSum + (signal ?: 0),
                ratingCount = topic.ratingCount + (if (signal != null) 1 else 0),
                consecutiveAbandons = 0,
                modifiedAt = System.currentTimeMillis()
            )
        } else {
            topic.copy(
                abandonedCount = topic.abandonedCount + 1,
                consecutiveAbandons = topic.consecutiveAbandons + 1,
                modifiedAt = System.currentTimeMillis()
            )
        }
        paperTopicDao.updateTopic(updated)
        safeCloudCall(TAG, "pushPaperTopic") { firebaseManager.pushPaperTopic(updated) }
        if (!read && shouldPromptMute(updated)) {
            _muteSuggestion.value = updated
        }
    }

    /** Send a history item back to the queue (re-read / gave-up-too-soon). Clears the read mark. */
    fun requeue(paper: Paper) {
        viewModelScope.launch {
            val updated = paper.copy(
                status = PaperStatus.WANT,
                readDate = null,
                cloudId = paper.cloudId.ifEmpty { UUID.randomUUID().toString() },
                modifiedAt = System.currentTimeMillis()
            )
            paperDao.updatePaper(updated)
            safeCloudCall(TAG, "pushPaper") { firebaseManager.pushPaper(updated) }
        }
    }

    fun deletePaper(paper: Paper) {
        viewModelScope.launch {
            paperDao.deletePaper(paper)
            safeCloudCall(TAG, "deletePaper") { firebaseManager.deletePaper(paper.cloudId) }
        }
    }

    companion object {
        private const val TAG = "PapersViewModel"
        private const val PER_SLOT_LIMIT = 3
        private const val DAILY_TOTAL_CAP = 5
        /** Recommendations added per day — a shelf, not a firehose (#150). */
        private const val RECOMMENDATION_LIMIT = 3
    }
}
