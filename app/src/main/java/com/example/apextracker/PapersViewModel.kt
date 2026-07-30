package com.example.apextracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class PapersUiState(
    val todayPick: Paper?,
    val queue: List<Paper>,
    val history: List<Paper>,
    val loaded: Boolean
) {
    companion object {
        val EMPTY = PapersUiState(null, emptyList(), emptyList(), loaded = false)
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

/**
 * Papers reading log. Room is the source of truth, same as every other module. Cloud sync is
 * deliberately not wired in v1 (Plan.md decision 13 — filed as a follow-up issue); rows still
 * get cloudId/modifiedAt at creation so the sync lands as a pure FirebaseManager change later.
 */
class PapersViewModel(application: Application) : AndroidViewModel(application) {

    private val paperDao = AppDatabase.getDatabase(application).paperDao()
    private val client = SemanticScholarClient()

    val uiState: StateFlow<PapersUiState> = paperDao.getAllPapers()
        .map { papers ->
            val queue = paperQueue(papers)
            PapersUiState(
                todayPick = dailyPick(queue, LocalDate.now()),
                queue = queue,
                history = paperHistory(papers),
                loaded = true
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PapersUiState.EMPTY)

    private val _fetchState = MutableStateFlow<PaperFetchState>(PaperFetchState.Idle)
    val fetchState: StateFlow<PaperFetchState> = _fetchState

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

    /** Confirm the previewed paper into the queue. */
    fun addFetched(fetched: FetchedPaper) {
        viewModelScope.launch {
            if (paperDao.getByS2Id(fetched.s2Id) == null) {
                paperDao.insertPaper(
                    Paper(
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
                )
            }
            _fetchState.value = PaperFetchState.Idle
        }
    }

    /** Import the bundled starter list, skipping any seed already present (by landing URL). */
    fun importSeeds() {
        viewModelScope.launch {
            PAPER_SEEDS.forEach { seed ->
                if (paperDao.getByUrl(seed.url) == null) {
                    paperDao.insertPaper(
                        Paper(
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
                    )
                }
            }
        }
    }

    /**
     * Finish a paper: status READ, readDate today (feeds the PAPERS goal metric), plus the
     * structured memo. Also the edit path for a paper already read — readDate is only set once.
     */
    fun markRead(paper: Paper, memo: String, signal: Int?) {
        viewModelScope.launch {
            paperDao.updatePaper(
                paper.copy(
                    status = PaperStatus.READ,
                    readDate = paper.readDate ?: LocalDate.now(),
                    memo = memo.trim(),
                    signal = normalizeSignal(signal),
                    modifiedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** Drop a paper from the queue without pretending it was read. Memo optional ("why I bailed"). */
    fun abandon(paper: Paper, memo: String) {
        viewModelScope.launch {
            paperDao.updatePaper(
                paper.copy(
                    status = PaperStatus.ABANDONED,
                    memo = memo.trim(),
                    modifiedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** Send a history item back to the queue (re-read / gave-up-too-soon). Clears the read mark. */
    fun requeue(paper: Paper) {
        viewModelScope.launch {
            paperDao.updatePaper(
                paper.copy(
                    status = PaperStatus.WANT,
                    readDate = null,
                    modifiedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deletePaper(paper: Paper) {
        viewModelScope.launch { paperDao.deletePaper(paper) }
    }
}
