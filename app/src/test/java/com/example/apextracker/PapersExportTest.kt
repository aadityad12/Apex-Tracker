package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PapersExportTest {
    private val paper = Paper(
        id = 7,
        s2Id = "S2:abc-123",
        title = "Attention & Transformers",
        authors = "Ada Lovelace, Alan Turing",
        year = 2026,
        venue = "Journal of Tests",
        url = "https://example.com/paper",
        pdfUrl = "https://example.com/paper.pdf",
        status = PaperStatus.READ,
        addedDate = LocalDate.of(2026, 8, 1),
        readDate = LocalDate.of(2026, 8, 6),
        memo = "Useful, with \"surprises\"",
        signal = 5
    )

    @Test
    fun `BibTeX includes citation metadata and converts display authors`() {
        val bibtex = buildPapersBibtex(listOf(paper))

        assertTrue(bibtex.startsWith("@article{apexS2abc123,"))
        assertTrue(bibtex.contains("title = {Attention \\& Transformers}"))
        assertTrue(bibtex.contains("author = {Ada Lovelace and Alan Turing}"))
        assertTrue(bibtex.contains("year = {2026}"))
        assertTrue(bibtex.contains("journal = {Journal of Tests}"))
        assertTrue(bibtex.contains("url = {https://example.com/paper}"))
    }

    @Test
    fun `BibTeX separates multiple entries and uses a fallback key`() {
        val second = paper.copy(id = 9, s2Id = "", title = "A Second Paper")
        val bibtex = buildPapersBibtex(listOf(paper, second))

        assertEquals(2, "@article".toRegex().findAll(bibtex).count())
        assertTrue(bibtex.contains("}\n\n@article{apex9ASecondPaper,"))
    }

    @Test
    fun `CSV includes citation and reading-log fields with RFC escaping`() {
        val csv = buildPapersCsv(listOf(paper))

        assertEquals(
            "title,authors,year,venue,url,pdf_url,s2_id,status,added_date,read_date,memo,signal",
            csv.lineSequence().first()
        )
        assertEquals(
            "Attention & Transformers,\"Ada Lovelace, Alan Turing\",2026,Journal of Tests," +
                "https://example.com/paper,https://example.com/paper.pdf,S2:abc-123,READ," +
                "2026-08-01,2026-08-06,\"Useful, with \"\"surprises\"\"\",5",
            csv.lineSequence().drop(1).first()
        )
    }

    @Test
    fun `empty citation exports are valid`() {
        assertEquals("", buildPapersBibtex(emptyList()))
        assertEquals(
            "title,authors,year,venue,url,pdf_url,s2_id,status,added_date,read_date,memo,signal",
            buildPapersCsv(emptyList())
        )
    }
}
