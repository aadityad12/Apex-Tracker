package com.example.apextracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        url = if (obj.isNull("url")) "" else obj.optString("url"),
        pdfUrl = pdfObj?.optString("url")?.takeIf { it != "null" } ?: ""
    )
}

/** Parses the `data` array returned by paper relevance search, skipping malformed individual rows. */
fun parseS2SearchJson(json: String): List<FetchedPaper> {
    val data = JSONObject(json).getJSONArray("data")
    return (0 until data.length()).mapNotNull { index ->
        data.optJSONObject(index)?.let { row -> runCatching { parseS2PaperJson(row.toString()) }.getOrNull() }
    }
}

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
}

class PaperNotFoundException : Exception("Paper not found")

class SemanticScholarRateLimitedException(val retryAfterSeconds: Long?) :
    Exception("Semantic Scholar rate limited the request")

fun semanticScholarBlockedUntil(
    retryAfterSeconds: Long?,
    nowMillis: Long
): Long = nowMillis + (retryAfterSeconds ?: 21_600L).coerceIn(60L, 86_400L) * 1_000L
