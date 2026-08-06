package com.example.apextracker

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Paper.status values. Plain strings, matching the GoalType/GoalMetric convention. */
object PaperStatus {
    const val WANT = "WANT"           // in the reading queue
    const val READ = "READ"           // finished, counts toward the reading goal on readDate
    const val ABANDONED = "ABANDONED" // deliberately dropped; kept in history, never resurfaces
}

/** Where a paper entered the app. RECOMMENDED is reserved for the v3 source. */
object PaperSource {
    const val MANUAL = "MANUAL" // pasted link / id
    const val SEED = "SEED"     // the bundled starter list
    const val DAILY = "DAILY"   // daily Semantic Scholar topic discovery
}

/**
 * An academic paper in the reading log. The app is deliberately NOT a PDF reader — it owns the
 * knowledge layer (metadata, TL;DR, the queue, the memo written after reading) and hands the
 * actual document to the browser/PDF viewer via [url]/[pdfUrl] (see Plan.md decision 2).
 *
 * [s2Id] is the Semantic Scholar paperId when the paper was fetched from the API ("" for offline
 * seeds) and is the dedup key for re-adds. [readDate] is set when [status] becomes READ and is
 * what the Dashboard's PAPERS goal metric counts (see DashboardScoring). [memo] and [signal]
 * (1–5, null = unrated) are the structured "what I learned" layer written at completion time.
 */
@Entity(tableName = "papers")
data class Paper(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val s2Id: String = "",
    val title: String,
    val authors: String = "",      // display string, "First Author, Second Author, …"
    val year: Int? = null,
    val venue: String = "",
    val abstractText: String = "", // `abstract` is a Kotlin keyword
    val tldr: String = "",
    val url: String = "",          // landing page (S2 / arXiv abs / publisher)
    val pdfUrl: String = "",       // open-access PDF when known; "" = fall back to url
    val source: String = PaperSource.MANUAL,
    val status: String = PaperStatus.WANT,
    val addedDate: LocalDate = LocalDate.now(),
    val readDate: LocalDate? = null,
    val memo: String = "",
    val signal: Int? = null,       // 1..5; feeds the v3 recommender
    val cloudId: String = "",
    val modifiedAt: Long = 0L
)
