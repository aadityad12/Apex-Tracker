package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SemanticScholarTest {

    // --- normalizePaperIdInput ---

    @Test
    fun `arxiv abs url maps to arXiv id`() {
        assertEquals("arXiv:1706.03762", normalizePaperIdInput("https://arxiv.org/abs/1706.03762"))
    }

    @Test
    fun `arxiv pdf url with version maps to unversioned arXiv id`() {
        assertEquals("arXiv:1706.03762", normalizePaperIdInput("https://arxiv.org/pdf/1706.03762v5.pdf"))
    }

    @Test
    fun `bare new-style arxiv id`() {
        assertEquals("arXiv:2201.11903", normalizePaperIdInput("2201.11903"))
    }

    @Test
    fun `bare arxiv id with prefix and version`() {
        assertEquals("arXiv:2201.11903", normalizePaperIdInput("arXiv:2201.11903v2"))
    }

    @Test
    fun `old-style arxiv id`() {
        assertEquals("arXiv:cs/0112017", normalizePaperIdInput("cs/0112017"))
    }

    @Test
    fun `doi url maps to DOI id`() {
        assertEquals(
            "DOI:10.1145/3292500.3330701",
            normalizePaperIdInput("https://doi.org/10.1145/3292500.3330701")
        )
    }

    @Test
    fun `bare doi maps to DOI id`() {
        assertEquals("DOI:10.1038/nature14539", normalizePaperIdInput("10.1038/nature14539"))
    }

    @Test
    fun `semantic scholar paper url maps to sha`() {
        assertEquals(
            "204e3073870fae3d05bcbc2f6a8e263d9b72e776",
            normalizePaperIdInput("https://www.semanticscholar.org/paper/Attention-is-All-you-Need-Vaswani-Shazeer/204e3073870fae3d05bcbc2f6a8e263d9b72e776")
        )
    }

    @Test
    fun `raw sha passes through lowercased`() {
        assertEquals(
            "204e3073870fae3d05bcbc2f6a8e263d9b72e776",
            normalizePaperIdInput("204E3073870FAE3D05BCBC2F6A8E263D9B72E776")
        )
    }

    @Test
    fun `other http url wraps in URL prefix`() {
        assertEquals(
            "URL:https://raft.github.io/raft.pdf",
            normalizePaperIdInput("https://raft.github.io/raft.pdf")
        )
    }

    @Test
    fun `free text and blank are rejected`() {
        assertNull(normalizePaperIdInput("attention is all you need"))
        assertNull(normalizePaperIdInput("   "))
    }

    // --- parseS2PaperJson ---

    @Test
    fun `full document parses`() {
        val json = """
            {
              "paperId": "204e3073870fae3d05bcbc2f6a8e263d9b72e776",
              "title": "Attention is All you Need",
              "authors": [{"authorId": "1", "name": "Ashish Vaswani"}, {"authorId": "2", "name": "Noam Shazeer"}],
              "year": 2017,
              "venue": "NeurIPS",
              "abstract": "The dominant sequence transduction models…",
              "tldr": {"model": "tldr@v2.0.0", "text": "A new architecture, the Transformer."},
              "url": "https://www.semanticscholar.org/paper/204e",
              "openAccessPdf": {"url": "https://arxiv.org/pdf/1706.03762", "status": "GREEN"}
            }
        """.trimIndent()
        val p = parseS2PaperJson(json)
        assertEquals("204e3073870fae3d05bcbc2f6a8e263d9b72e776", p.s2Id)
        assertEquals("Attention is All you Need", p.title)
        assertEquals("Ashish Vaswani, Noam Shazeer", p.authors)
        assertEquals(2017, p.year)
        assertEquals("NeurIPS", p.venue)
        assertEquals("A new architecture, the Transformer.", p.tldr)
        assertEquals("https://arxiv.org/pdf/1706.03762", p.pdfUrl)
    }

    @Test
    fun `absent optional fields map to empty, not throw`() {
        val json = """
            {
              "paperId": "abc",
              "title": "Minimal",
              "authors": [],
              "year": null,
              "venue": null,
              "abstract": null,
              "tldr": null,
              "url": null,
              "openAccessPdf": null
            }
        """.trimIndent()
        val p = parseS2PaperJson(json)
        assertEquals("abc", p.s2Id)
        assertEquals("Minimal", p.title)
        assertEquals("", p.authors)
        assertNull(p.year)
        assertEquals("", p.venue)
        assertEquals("", p.abstractText)
        assertEquals("", p.tldr)
        assertEquals("", p.url)
        assertEquals("", p.pdfUrl)
    }

    @Test
    fun `document without title throws`() {
        assertThrows(Exception::class.java) {
            parseS2PaperJson("""{"paperId": "abc"}""")
        }
    }

    @Test
    fun `search response parses valid rows and skips malformed rows`() {
        val papers = parseS2SearchJson(
            """
            {
              "total": 2,
              "data": [
                {"paperId":"p1","title":"Fresh Paper","authors":[],"year":2026},
                {"paperId":"broken"}
              ]
            }
            """.trimIndent()
        )
        assertEquals(1, papers.size)
        assertEquals("p1", papers.single().s2Id)
        assertEquals("Fresh Paper", papers.single().title)
    }

    @Test
    fun `rate limit backoff uses header and clamps extremes`() {
        assertEquals(1_060_000L, semanticScholarBlockedUntil(60L, 1_000_000L))
        assertEquals(1_060_000L, semanticScholarBlockedUntil(1L, 1_000_000L))
        assertEquals(87_400_000L, semanticScholarBlockedUntil(999_999L, 1_000_000L))
        assertEquals(22_600_000L, semanticScholarBlockedUntil(null, 1_000_000L))
    }
}
