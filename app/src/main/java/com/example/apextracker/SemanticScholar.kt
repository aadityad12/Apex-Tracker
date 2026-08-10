package com.example.apextracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

/**
 * Semantic Scholar Graph API — the single metadata backbone for the Papers feature (Plan.md
 * decision 3). Free, JSON, resolves arXiv ids / DOIs / URLs, and carries machine-generated
 * TL;DRs and open-access PDF links. The pure parts (id normalization, response parsing) are
 * top-level functions unit-tested in SemanticScholarTest; only [SemanticScholarClient.fetchPaper]
 * touches the network.
 */

/** Paper metadata as fetched from the API — the add-dialog preview, not yet a Room row. */
data class FetchedPaper(
    val s2Id: String,
    val title: String,
    val authors: String,
    val year: Int?,
    val venue: String,
    val abstractText: String,
    val tldr: String,
    val url: String,
    val pdfUrl: String
)

private val BARE_NEW_ARXIV = Regex("""^\d{4}\.\d{4,5}(v\d+)?$""")
private val BARE_OLD_ARXIV = Regex("""^[a-z][a-z-]*(\.[A-Z]{2})?/\d{7}(v\d+)?$""")
private val S2_SHA = Regex("""^[0-9a-f]{40}$""")
private val ARXIV_URL = Regex("""arxiv\.org/(?:abs|pdf)/([^?#]+?)(?:\.pdf)?/?$""", RegexOption.IGNORE_CASE)
private val DOI_URL = Regex("""(?:dx\.)?doi\.org/(.+)$""", RegexOption.IGNORE_CASE)
private val S2_URL = Regex("""semanticscholar\.org/paper/(?:[^/]+/)?([0-9a-f]{40})""", RegexOption.IGNORE_CASE)

private fun stripArxivVersion(id: String) = id.replace(Regex("""v\d+$"""), "")

/**
 * Keeps a URL only if it is plain http(s); everything else becomes "" (Issue #190).
 *
 * Paper URLs are third-party data — the API response, another device's Firestore doc, a restored
 * backup file — and end up in `Intent.ACTION_VIEW`, which launches whatever app claims the scheme.
 * A `file:` URI is worse than that: it throws FileUriExposedException on `startActivity`, which is
 * a RuntimeException, so it crashed the app rather than failing to open.
 *
 * Applied at the parse boundary so a bad URL never reaches Room, matching how the rest of the
 * codebase keeps its invariants in the pure, unit-tested parse functions. `openPaper` re-checks
 * anyway, for rows written before this existed.
 *
 * A deliberately dumb prefix test rather than `Uri.parse`: this must stay pure for the JVM tests,
 * and a scheme check is exactly a prefix check.
 */
fun sanitizeWebUrl(raw: String): String {
    val trimmed = raw.trim()
    val lower = trimmed.lowercase()
    return if (lower.startsWith("http://") || lower.startsWith("https://")) trimmed else ""
}

/**
 * Maps whatever the user pasted — an arXiv/DOI/S2 link, a bare arXiv id, a DOI, a raw S2 sha,
 * or any other paper URL — onto the id form the S2 `/paper/{id}` endpoint accepts. Returns null
 * only for input that can't plausibly identify a paper (blank / non-URL free text). Trailing
 * arXiv version suffixes are stripped: S2 indexes the paper, not the revision.
 */
fun normalizePaperIdInput(raw: String): String? {
    val input = raw.trim().removePrefix("arxiv:").removePrefix("arXiv:").trim()
    if (input.isEmpty()) return null

    if (BARE_NEW_ARXIV.matches(input) || BARE_OLD_ARXIV.matches(input)) {
        return "arXiv:" + stripArxivVersion(input)
    }
    if (S2_SHA.matches(input.lowercase())) return input.lowercase()
    if (input.startsWith("10.") && input.contains('/')) return "DOI:$input"

    if (input.startsWith("http://", ignoreCase = true) || input.startsWith("https://", ignoreCase = true)) {
        ARXIV_URL.find(input)?.let { return "arXiv:" + stripArxivVersion(it.groupValues[1]) }
        DOI_URL.find(input)?.let { return "DOI:" + it.groupValues[1].trimEnd('/') }
        S2_URL.find(input)?.let { return it.groupValues[1].lowercase() }
        return "URL:$input" // S2 resolves arbitrary indexed landing pages
    }
    return null
}

/**
 * Parses a Graph API paper document. Throws on anything without a paperId+title — per-call
 * try/catch at the call site, matching the parseXDoc convention in FirebaseManager. Absent
 * optional fields (tldr, openAccessPdf, venue…) map to empty/null, never throw.
 */
fun parseS2PaperJson(json: String): FetchedPaper {
    val obj = JSONObject(json)
    val s2Id = obj.getString("paperId")
    val title = obj.getString("title")
    val authorsArr = obj.optJSONArray("authors")
    val authors = if (authorsArr == null) "" else
        (0 until authorsArr.length()).mapNotNull { i ->
            authorsArr.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }
        }.joinToString(", ")
    val year = if (obj.isNull("year")) null else obj.optInt("year").takeIf { it != 0 }
    val tldrObj = obj.optJSONObject("tldr")
    val pdfObj = obj.optJSONObject("openAccessPdf")
    return FetchedPaper(
        s2Id = s2Id,
        title = title,
        authors = authors,
        year = year,
        venue = if (obj.isNull("venue")) "" else obj.optString("venue"),
        abstractText = if (obj.isNull("abstract")) "" else obj.optString("abstract"),
        tldr = tldrObj?.optString("text")?.takeIf { it != "null" } ?: "",
        // Non-web schemes are dropped here so they never reach Room (Issue #190).
        url = sanitizeWebUrl(if (obj.isNull("url")) "" else obj.optString("url")),
        pdfUrl = sanitizeWebUrl(pdfObj?.optString("url")?.takeIf { it != "null" } ?: "")
    )
}

/** Parses the `data` array returned by paper relevance search, skipping malformed individual rows. */
fun parseS2SearchJson(json: String): List<FetchedPaper> {
    val data = JSONObject(json).getJSONArray("data")
    return (0 until data.length()).mapNotNull { index ->
        data.optJSONObject(index)?.let { row -> runCatching { parseS2PaperJson(row.toString()) }.getOrNull() }
    }
}

/**
 * Parses the `recommendedPapers` array from the recommendations API (Issue #150). Same
 * skip-the-bad-row handling as [parseS2SearchJson]; only the wrapper key differs.
 */
fun parseS2RecommendationsJson(json: String): List<FetchedPaper> {
    val papers = JSONObject(json).getJSONArray("recommendedPapers")
    return (0 until papers.length()).mapNotNull { index ->
        papers.optJSONObject(index)?.let { row -> runCatching { parseS2PaperJson(row.toString()) }.getOrNull() }
    }
}

/**
 * The request body for the batch recommendations endpoint. Built with `JSONObject`/`JSONArray`
 * rather than string concatenation because paper ids come from stored rows, which came from a
 * remote API — and hand-built JSON is how an unexpected character becomes a malformed request.
 */
fun buildRecommendationsBody(examples: RecommendationExamples): String =
    JSONObject()
        .put("positivePaperIds", JSONArray(examples.positive))
        .put("negativePaperIds", JSONArray(examples.negative))
        .toString()

private const val S2_FIELDS = "title,authors,year,venue,abstract,tldr,url,openAccessPdf"

/**
 * The `/paper/search` URL for a topic: [keyword] is the actual query text (a PaperTopic's free-text
 * interest, e.g. "diffusion models"), [field] narrows via `fieldsOfStudy`. Pulled out as a pure
 * function — same convention as the rest of this file — so query construction is unit-tested
 * without a network call.
 */
fun buildSearchUrl(field: String, keyword: String, today: LocalDate): String {
    val query = URLEncoder.encode(keyword, "UTF-8").replace("+", "%20")
    val encodedField = URLEncoder.encode(field, "UTF-8").replace("+", "%20")
    val dateRange = "${today.minusYears(1)}:"
    return "https://api.semanticscholar.org/graph/v1/paper/search" +
        "?query=$query&fields=$S2_FIELDS&limit=10" +
        "&publicationDateOrYear=$dateRange&fieldsOfStudy=$encodedField"
}

/** The batch recommendations endpoint, which takes positive *and* negative examples (Issue #150). */
fun buildRecommendationsUrl(limit: Int): String =
    "https://api.semanticscholar.org/recommendations/v1/papers?fields=$S2_FIELDS&limit=$limit"

class SemanticScholarClient {
    /**
     * One GET against `/graph/v1/paper/{id}`. Returns a failed Result (never throws) so the
     * ViewModel can surface "not found" / "no connection" as dialog states. The id path segment
     * is percent-encoded because DOIs contain slashes.
     */
    suspend fun fetchPaper(normalizedId: String): Result<FetchedPaper> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(normalizedId, "UTF-8").replace("+", "%20")
            val url = URL("https://api.semanticscholar.org/graph/v1/paper/$encoded?fields=$S2_FIELDS")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Accept", "application/json")
                when (val code = conn.responseCode) {
                    HttpURLConnection.HTTP_OK ->
                        parseS2PaperJson(conn.inputStream.bufferedReader().use { it.readText() })
                    HttpURLConnection.HTTP_NOT_FOUND ->
                        throw PaperNotFoundException()
                    else -> throw IllegalStateException("Semantic Scholar HTTP $code")
                }
            } finally {
                conn.disconnect()
            }
        }
    }

    /**
     * One relevance-search request for a topic's field+keyword. The caller caps inserted rows and
     * persists the daily gate. Results cover the last 12 months so a quiet topic can still return
     * useful work without turning this into a broad historical search.
     */
    suspend fun searchRecent(field: String, keyword: String, today: LocalDate): Result<List<FetchedPaper>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(buildSearchUrl(field, keyword, today))
                val conn = url.openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 15_000
                    conn.setRequestProperty("Accept", "application/json")
                    when (val code = conn.responseCode) {
                        HttpURLConnection.HTTP_OK ->
                            parseS2SearchJson(conn.inputStream.bufferedReader().use { it.readText() })
                        429 -> {
                            val retrySeconds = conn.getHeaderField("Retry-After")?.toLongOrNull()
                            throw SemanticScholarRateLimitedException(retrySeconds)
                        }
                        else -> throw IllegalStateException("Semantic Scholar HTTP $code")
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }

    /**
     * Papers similar to what the reader actually finished and liked (Issue #150).
     *
     * The batch `POST /recommendations/v1/papers` form rather than `forpaper/{id}`, because the
     * whole point is to weigh several liked papers together *and* to say what was disliked —
     * `forpaper` takes neither. Shares the 429 handling with [searchRecent]: both draw on the same
     * unauthenticated pool, so a backoff earned by one applies to the other.
     */
    suspend fun fetchRecommendations(
        examples: RecommendationExamples,
        limit: Int
    ): Result<List<FetchedPaper>> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(buildRecommendationsUrl(limit)).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(buildRecommendationsBody(examples).toByteArray()) }
                when (val code = conn.responseCode) {
                    HttpURLConnection.HTTP_OK ->
                        parseS2RecommendationsJson(conn.inputStream.bufferedReader().use { it.readText() })
                    429 -> {
                        val retrySeconds = conn.getHeaderField("Retry-After")?.toLongOrNull()
                        throw SemanticScholarRateLimitedException(retrySeconds)
                    }
                    else -> throw IllegalStateException("Semantic Scholar HTTP $code")
                }
            } finally {
                conn.disconnect()
            }
        }
    }
}

class PaperNotFoundException : Exception("Paper not found")

class SemanticScholarRateLimitedException(val retryAfterSeconds: Long?) :
    Exception("Semantic Scholar rate limited the request")

fun semanticScholarBlockedUntil(
    retryAfterSeconds: Long?,
    nowMillis: Long
): Long = nowMillis + (retryAfterSeconds ?: 21_600L).coerceIn(60L, 86_400L) * 1_000L
