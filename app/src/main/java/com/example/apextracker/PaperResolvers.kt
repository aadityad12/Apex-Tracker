package com.example.apextracker

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Metadata sources used when Semantic Scholar can't answer.
 *
 * S2 is still preferred — it is the only one that carries a `tldr` and the `s2Id` that
 * PapersRecommendations needs as an example — but it is not the only one that *can* answer. Its
 * unauthenticated pool rate-limits hard (observed 429 on essentially every request, with no
 * `Retry-After` header), and when it does, an arXiv link or a DOI has an authoritative source that
 * is free, keyless and unthrottled. Before this file, that situation was a dead end: the add
 * dialog reported "check your connection" and the paper simply could not be added.
 *
 * Same shape as SemanticScholar.kt by design — pure top-level URL/parse functions (unit-tested on
 * the JVM in PaperResolversTest) plus thin clients that do nothing but the request.
 *
 * Both sources leave [FetchedPaper.s2Id] blank. That is an already-supported state: every bundled
 * seed has one, which is why PaperDao.getByUrl exists and why getByS2Id is written
 * `AND s2Id != ''`. It costs the paper nothing except eligibility as a recommendation example.
 */

/** Which authoritative source can resolve [normalizedId] when S2 fails, and the bare id to ask for. */
sealed interface PaperFallback {
    val bareId: String

    data class Arxiv(override val bareId: String) : PaperFallback
    data class Crossref(override val bareId: String) : PaperFallback
}

/**
 * Routes a `normalizePaperIdInput` result to its fallback source, or null when none exists —
 * a raw S2 sha or a `URL:` landing page can only be resolved by S2 itself.
 */
fun paperFallbackFor(normalizedId: String): PaperFallback? = when {
    normalizedId.startsWith("arXiv:", ignoreCase = true) ->
        PaperFallback.Arxiv(normalizedId.substring("arXiv:".length))
    normalizedId.startsWith("DOI:", ignoreCase = true) ->
        PaperFallback.Crossref(normalizedId.substring("DOI:".length))
    else -> null
}

// ---------------------------------------------------------------------------- arXiv

fun buildArxivQueryUrl(arxivId: String): String {
    val encoded = URLEncoder.encode(arxivId, "UTF-8").replace("+", "%20")
    return "https://export.arxiv.org/api/query?id_list=$encoded&max_results=1"
}

/**
 * Parses one entry out of an arXiv Atom feed.
 *
 * Two things worth knowing before editing this:
 *
 * 1. **An unknown id is not an HTTP error.** arXiv answers 200 with a well-formed feed containing
 *    `<opensearch:totalResults>0</opensearch:totalResults>` and no `<entry>` at all. Treating that
 *    as a parse failure would report "something went wrong" for the one case the user most needs
 *    named, so it maps to [PaperNotFoundException] like a real 404.
 * 2. **`<title>` and `<link>` exist at feed level too** — the feed's own title is an echo of the
 *    query string ("arXiv Query: search_query=..."). Everything is read from inside the `<entry>`
 *    element, never from the document root.
 *
 * DOM rather than XmlPullParser so this function stays pure and runs in plain JVM unit tests:
 * `javax.xml.parsers` is real on both Android and the JVM, and needs no new dependency.
 */
fun parseArxivFeedXml(xml: String): FetchedPaper {
    val factory = DocumentBuilderFactory.newInstance().apply {
        // Untrusted network input: no DTDs, no entity resolution, no XXE (Issue #190's boundary).
        //
        // Each feature is set independently and tolerantly, because the two platforms disagree
        // about which names exist: `disallow-doctype-decl` is an Apache Xerces feature, and
        // Android ships Harmony's parser, which throws ParserConfigurationException for it. Setting
        // them in one `apply` block killed every arXiv lookup on-device while the JVM unit tests
        // passed, since desktop Java *is* Xerces. The `isExpandEntityReferences`/XInclude settings
        // below are honoured everywhere and are what actually carry the protection on Android.
        setFeatureQuietly("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeatureQuietly("http://xml.org/sax/features/external-general-entities", false)
        setFeatureQuietly("http://xml.org/sax/features/external-parameter-entities", false)
        // Android's parser throws UnsupportedOperationException from this one rather than
        // ignoring it, for the same reason — hence the same tolerance.
        runCatching { isXIncludeAware = false }
        isExpandEntityReferences = false
        // Not namespace-aware, so the arxiv:-prefixed elements are addressable by literal name.
        isNamespaceAware = false
    }
    // A leading BOM or stray whitespace makes the parser reject the `<?xml …?>` declaration
    // outright ("processing instruction target matching [xX][mM][lL] is not allowed"), which would
    // surface as an unexplained failure rather than anything the user could act on.
    val cleaned = xml.trimStart('\uFEFF', ' ', '\t', '\n', '\r')
    val doc = factory.newDocumentBuilder()
        .parse(ByteArrayInputStream(cleaned.toByteArray(Charsets.UTF_8)))
    val entry = doc.getElementsByTagName("entry").item(0) as? Element
        ?: throw PaperNotFoundException()

    val title = entry.firstText("title").collapseWhitespace()
    if (title.isBlank()) throw PaperNotFoundException()

    val authors = entry.getElementsByTagName("author").let { nodes ->
        (0 until nodes.length).mapNotNull { i ->
            (nodes.item(i) as? Element)?.firstText("name")?.collapseWhitespace()?.takeIf { it.isNotBlank() }
        }
    }.joinToString(", ")

    // "2017-06-12T17:57:34Z" — the year is all this model keeps.
    val year = entry.firstText("published").take(4).toIntOrNull()

    // rel="alternate" is the abs landing page; the pdf link is the one titled "pdf". <id> is the
    // versioned abs URL and stands in when the links are missing.
    val links = entry.getElementsByTagName("link")
    var landing = ""
    var pdf = ""
    for (i in 0 until links.length) {
        val link = links.item(i) as? Element ?: continue
        val href = link.getAttribute("href").orEmpty()
        when {
            link.getAttribute("title") == "pdf" -> pdf = href
            link.getAttribute("rel") == "alternate" -> landing = href
        }
    }
    if (landing.isBlank()) landing = entry.firstText("id")

    return FetchedPaper(
        s2Id = "",
        title = title,
        authors = authors,
        year = year,
        venue = entry.firstText("arxiv:journal_ref").collapseWhitespace(),
        abstractText = entry.firstText("summary").collapseWhitespace(),
        tldr = "",
        url = sanitizeWebUrl(landing),
        pdfUrl = sanitizeWebUrl(pdf)
    )
}

/**
 * Applies a parser hardening feature where the platform's parser supports it, else skips it.
 *
 * Android and desktop Java disagree about which of these exist, and an unsupported one *throws*
 * rather than being ignored — so an un-guarded call fails the whole parse on one platform while
 * passing every unit test on the other.
 */
private fun DocumentBuilderFactory.setFeatureQuietly(name: String, value: Boolean) {
    runCatching { setFeature(name, value) }
}

/** Text of this element's first [tag] descendant, or "" — the arXiv feed's optional fields. */
private fun Element.firstText(tag: String): String =
    getElementsByTagName(tag).item(0)?.textContent.orEmpty()

/** arXiv hard-wraps titles and abstracts at ~80 columns; the newlines are formatting, not content. */
private fun String.collapseWhitespace(): String = trim().replace(Regex("""\s+"""), " ")

class ArxivClient {
    private companion object { const val TAG = "ArxivClient" }

    suspend fun fetchPaper(arxivId: String): Result<FetchedPaper> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(buildArxivQueryUrl(arxivId)).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Accept", "application/atom+xml")
                when (val code = conn.responseCode) {
                    HttpURLConnection.HTTP_OK ->
                        parseArxivFeedXml(conn.inputStream.bufferedReader().use { it.readText() })
                    else -> {
                        Log.w(TAG, "fetchPaper($arxivId): HTTP $code")
                        throw IllegalStateException("arXiv HTTP $code")
                    }
                }
            } finally {
                conn.disconnect()
            }
        }
    }
}

// ------------------------------------------------------------------------- Crossref

fun buildCrossrefUrl(doi: String): String {
    val encoded = URLEncoder.encode(doi, "UTF-8").replace("+", "%20")
    return "https://api.crossref.org/works/$encoded"
}

/**
 * Parses a Crossref `/works/{doi}` response. Crossref returns a clean 404 for an unknown DOI, so
 * unlike [parseArxivFeedXml] this has no empty-result case to translate.
 *
 * Every interesting field is an *array* in Crossref's model — a work can carry several titles and
 * container titles — and this app's [FetchedPaper] holds one of each, so the first is taken.
 */
fun parseCrossrefWorkJson(json: String): FetchedPaper {
    val message = JSONObject(json).getJSONObject("message")

    val title = decodeXmlEntities(message.optJSONArray("title")?.optString(0).orEmpty()).trim()
    if (title.isBlank()) throw PaperNotFoundException()

    val authorsArr = message.optJSONArray("author")
    val authors = if (authorsArr == null) "" else (0 until authorsArr.length()).mapNotNull { i ->
        val a = authorsArr.optJSONObject(i) ?: return@mapNotNull null
        // "given family" for a person; `name` is what Crossref uses for a consortium.
        listOf(a.optString("given"), a.optString("family"))
            .filter { it.isNotBlank() && it != "null" }
            .joinToString(" ")
            .ifBlank { a.optString("name").takeIf { it.isNotBlank() && it != "null" }.orEmpty() }
            .takeIf { it.isNotBlank() }
    }.joinToString(", ")

    // issued.date-parts is [[year, month, day]], with month/day frequently absent.
    val year = message.optJSONObject("issued")
        ?.optJSONArray("date-parts")?.optJSONArray(0)?.optInt(0)?.takeIf { it != 0 }

    return FetchedPaper(
        s2Id = "",
        title = title,
        authors = authors,
        year = year,
        venue = decodeXmlEntities(message.optJSONArray("container-title")?.optString(0).orEmpty()).trim(),
        // Crossref abstracts are JATS-XML fragments (<jats:p>…</jats:p>), not plain text.
        abstractText = stripJatsMarkup(message.optString("abstract")),
        tldr = "",
        url = sanitizeWebUrl(message.optString("URL")),
        pdfUrl = ""
    )
}

/**
 * Crossref stores its text as HTML/XML-escaped source, entities and all — a title or a venue comes
 * back holding a literal `&amp;`, which would render to the reader as "&amp;" rather than "&".
 */
internal fun decodeXmlEntities(raw: String): String =
    raw.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
        .replace("&apos;", "'").replace("&#39;", "'").replace("&nbsp;", " ")
        // Ampersand last, so "&amp;lt;" decodes to "&lt;" rather than being re-decoded to "<".
        .replace("&amp;", "&")

/** Crossref abstracts arrive as JATS XML; the tags are noise in a plain-text abstract field. */
internal fun stripJatsMarkup(raw: String): String {
    if (raw.isBlank() || raw == "null") return ""
    return decodeXmlEntities(raw.replace(Regex("""<[^>]*>"""), " "))
        .replace(Regex("""\s+"""), " ")
        .trim()
        .removePrefix("Abstract ")
}

class CrossrefClient {
    private companion object {
        const val TAG = "CrossrefClient"
        // Crossref asks callers to identify themselves; a named agent gets the "polite pool".
        // Deliberately no mailto — the user's address is not this service's business.
        const val USER_AGENT = "ApexTracker/1.0 (https://github.com/aadityad12/Apex-Tracker)"
    }

    suspend fun fetchPaper(doi: String): Result<FetchedPaper> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(buildCrossrefUrl(doi)).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", USER_AGENT)
                when (val code = conn.responseCode) {
                    HttpURLConnection.HTTP_OK ->
                        parseCrossrefWorkJson(conn.inputStream.bufferedReader().use { it.readText() })
                    HttpURLConnection.HTTP_NOT_FOUND -> throw PaperNotFoundException()
                    else -> {
                        Log.w(TAG, "fetchPaper($doi): HTTP $code")
                        throw IllegalStateException("Crossref HTTP $code")
                    }
                }
            } finally {
                conn.disconnect()
            }
        }
    }
}

// -------------------------------------------------------------------------- the chain

/**
 * Resolves pasted input to metadata, preferring Semantic Scholar and falling back to whichever
 * source is authoritative for that id.
 *
 * Two rules make the behaviour predictable:
 *
 * - **A known-blocked S2 is skipped, not retried.** The 429 backoff window is already persisted
 *   and shared across the app (PapersDiscoverySettings.blockedUntilMillis); when it is in effect
 *   and a fallback exists, there is nothing to learn from asking S2 again. With no fallback the
 *   request still goes out — a user who tapped "Look up" deserves the attempt, and the window is
 *   a 6-hour guess whenever S2 omits `Retry-After`, which it does.
 * - **The fallback's verdict wins.** arXiv is authoritative for an arXiv id and Crossref for a
 *   DOI, so if one of them says 404 the paper really does not exist and the user is told so,
 *   rather than being handed S2's rate-limit message for a question S2 never answered.
 */
class PaperResolver(
    private val s2: SemanticScholarClient = SemanticScholarClient(),
    private val arxiv: ArxivClient = ArxivClient(),
    private val crossref: CrossrefClient = CrossrefClient()
) {
    suspend fun resolve(
        normalizedId: String,
        s2BlockedUntilMillis: Long = 0L,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<FetchedPaper> {
        val fallback = paperFallbackFor(normalizedId)
        val skipS2 = fallback != null && nowMillis < s2BlockedUntilMillis

        var s2Failure: Throwable? = null
        if (!skipS2) {
            s2.fetchPaper(normalizedId)
                .onSuccess { return Result.success(it) }
                .onFailure { s2Failure = it }
        }

        if (fallback == null) {
            return Result.failure(s2Failure ?: SemanticScholarRateLimitedException(null))
        }
        Log.i(TAG, "Semantic Scholar unavailable for $normalizedId; falling back to $fallback")
        return when (fallback) {
            is PaperFallback.Arxiv -> arxiv.fetchPaper(fallback.bareId)
            is PaperFallback.Crossref -> crossref.fetchPaper(fallback.bareId)
        }
    }

    private companion object { const val TAG = "PaperResolver" }
}
