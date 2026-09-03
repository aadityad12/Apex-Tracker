package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixtures are trimmed from real responses captured while diagnosing the "can't add an arXiv
 * paper" report — the structure (namespaced arxiv: elements, the feed-level <title> that echoes
 * the query, Crossref's array-valued fields) is what the parsers actually have to survive.
 */
class PaperResolversTest {

    private val arxivFeed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title type="html">ArXiv Query: search_query=&amp;id_list=1706.03762</title>
          <opensearch:totalResults xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/">1</opensearch:totalResults>
          <entry>
            <id>http://arxiv.org/abs/1706.03762v7</id>
            <title>Attention Is All You
      Need</title>
            <summary>  The dominant sequence transduction models are based on complex recurrent or
    convolutional neural networks.
    </summary>
            <published>2017-06-12T17:57:34Z</published>
            <author><name>Ashish Vaswani</name></author>
            <author><name>Noam Shazeer</name></author>
            <link href="https://arxiv.org/abs/1706.03762v7" rel="alternate" type="text/html"/>
            <link href="https://arxiv.org/pdf/1706.03762v7" rel="related" type="application/pdf" title="pdf"/>
          </entry>
        </feed>
    """.trimIndent()

    /** What arXiv really returns for an unknown id: HTTP 200, a valid feed, and no <entry>. */
    private val arxivEmptyFeed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title type="html">ArXiv Query: search_query=&amp;id_list=9999.99999</title>
          <opensearch:totalResults xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/">0</opensearch:totalResults>
        </feed>
    """.trimIndent()

    // ------------------------------------------------------------------ arXiv

    @Test
    fun `arxiv feed parses the entry, not the feed's own title`() {
        val paper = parseArxivFeedXml(arxivFeed)
        assertEquals("Attention Is All You Need", paper.title)
        assertEquals("Ashish Vaswani, Noam Shazeer", paper.authors)
        assertEquals(2017, paper.year)
        assertEquals("https://arxiv.org/abs/1706.03762v7", paper.url)
        assertEquals("https://arxiv.org/pdf/1706.03762v7", paper.pdfUrl)
        assertTrue(paper.abstractText.startsWith("The dominant sequence transduction models"))
    }

    @Test
    fun `arxiv hard-wrapped text is collapsed to single spaces`() {
        // The raw XML splits the title across two lines; a reader must not see the newline.
        assertTrue(!parseArxivFeedXml(arxivFeed).title.contains("\n"))
        assertTrue(!parseArxivFeedXml(arxivFeed).abstractText.contains("\n"))
    }

    @Test
    fun `an arxiv paper carries no s2Id or tldr`() {
        val paper = parseArxivFeedXml(arxivFeed)
        assertEquals("", paper.s2Id)
        assertEquals("", paper.tldr)
    }

    @Test(expected = PaperNotFoundException::class)
    fun `an empty arxiv feed is not found, not a parse error`() {
        parseArxivFeedXml(arxivEmptyFeed)
    }

    @Test
    fun `arxiv url builder encodes an old-style id's slash`() {
        assertTrue(buildArxivQueryUrl("cs/0112017").contains("id_list=cs%2F0112017"))
        assertTrue(buildArxivQueryUrl("1706.03762").contains("id_list=1706.03762"))
    }

    // --------------------------------------------------------------- Crossref

    private val crossrefWork = """
        {"status":"ok","message":{
          "title":["Optuna: A Next-generation Hyperparameter Optimization Framework"],
          "author":[{"given":"Takuya","family":"Akiba"},{"given":"Shotaro","family":"Sano"}],
          "container-title":["Proceedings of the 25th ACM SIGKDD International Conference"],
          "issued":{"date-parts":[[2019,7,25]]},
          "URL":"http://dx.doi.org/10.1145/3292500.3330701",
          "abstract":"<jats:p>We describe Optuna.</jats:p>"
        }}
    """.trimIndent()

    @Test
    fun `crossref work parses title, authors, venue and year`() {
        val paper = parseCrossrefWorkJson(crossrefWork)
        assertEquals("Optuna: A Next-generation Hyperparameter Optimization Framework", paper.title)
        assertEquals("Takuya Akiba, Shotaro Sano", paper.authors)
        assertEquals(2019, paper.year)
        assertTrue(paper.venue.startsWith("Proceedings of the 25th ACM SIGKDD"))
        assertEquals("http://dx.doi.org/10.1145/3292500.3330701", paper.url)
    }

    @Test
    fun `crossref JATS markup is stripped from the abstract`() {
        assertEquals("We describe Optuna.", parseCrossrefWorkJson(crossrefWork).abstractText)
    }

    @Test
    fun `crossref html entities are decoded in title and venue`() {
        // Crossref really does return "Knowledge Discovery &amp; Data Mining"; undecoded, the
        // reader sees the entity itself.
        val json = """{"message":{"title":["A &amp; B"],"container-title":["Discovery &amp; Mining"]}}"""
        val paper = parseCrossrefWorkJson(json)
        assertEquals("A & B", paper.title)
        assertEquals("Discovery & Mining", paper.venue)
    }

    @Test
    fun `crossref optional fields absent map to empty, not throw`() {
        val minimal = """{"message":{"title":["A Bare Record"]}}"""
        val paper = parseCrossrefWorkJson(minimal)
        assertEquals("A Bare Record", paper.title)
        assertEquals("", paper.authors)
        assertEquals("", paper.venue)
        assertEquals("", paper.abstractText)
        assertNull(paper.year)
    }

    @Test
    fun `a crossref consortium author falls back to its name field`() {
        val json = """{"message":{"title":["T"],"author":[{"name":"The ATLAS Collaboration"}]}}"""
        assertEquals("The ATLAS Collaboration", parseCrossrefWorkJson(json).authors)
    }

    @Test(expected = PaperNotFoundException::class)
    fun `a crossref record with no title is not found`() {
        parseCrossrefWorkJson("""{"message":{"title":[]}}""")
    }

    @Test
    fun `crossref url builder encodes the doi's slash`() {
        assertTrue(buildCrossrefUrl("10.1145/3292500.3330701").endsWith("10.1145%2F3292500.3330701"))
    }

    // ---------------------------------------------------------- fallback routing

    @Test
    fun `an arxiv id routes to arxiv, stripped of its prefix`() {
        assertEquals(PaperFallback.Arxiv("1706.03762"), paperFallbackFor("arXiv:1706.03762"))
    }

    @Test
    fun `a doi routes to crossref, stripped of its prefix`() {
        assertEquals(
            PaperFallback.Crossref("10.1145/3292500.3330701"),
            paperFallbackFor("DOI:10.1145/3292500.3330701")
        )
    }

    @Test
    fun `a raw sha or landing page has no fallback — only S2 can resolve those`() {
        assertNull(paperFallbackFor("204e3073870fae3d05bcbc2f6a8e263d9b72e776"))
        assertNull(paperFallbackFor("URL:https://raft.github.io/raft.pdf"))
    }

    // ------------------------------------------------------------ error mapping

    @Test
    fun `a rate limit is reported as rate-limited, not as a connection failure`() {
        // The whole point of the taxonomy: this used to render "check your connection".
        assertEquals(
            PaperFetchError.RATE_LIMITED,
            paperFetchErrorFor(SemanticScholarRateLimitedException(null))
        )
    }

    @Test
    fun `each failure maps to the state the dialog can explain`() {
        assertEquals(PaperFetchError.NOT_FOUND, paperFetchErrorFor(PaperNotFoundException()))
        assertEquals(
            PaperFetchError.OFFLINE,
            paperFetchErrorFor(java.net.UnknownHostException("api.semanticscholar.org"))
        )
        assertEquals(
            PaperFetchError.OFFLINE,
            paperFetchErrorFor(java.net.SocketTimeoutException("timeout"))
        )
        assertEquals(
            PaperFetchError.UNAVAILABLE,
            paperFetchErrorFor(IllegalStateException("Semantic Scholar HTTP 503"))
        )
    }
}
