package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PaperLinksTest {

    private fun paper(id: Long, cloudId: String) = Paper(id = id, title = "P$id", cloudId = cloudId)

    private fun link(
        paperCloudId: String,
        relatedPaperCloudId: String,
        created: LocalDate = LocalDate.of(2026, 1, 1)
    ) = PaperLink(paperCloudId = paperCloudId, relatedPaperCloudId = relatedPaperCloudId, createdDate = created)

    @Test
    fun `otherPaperCloudId resolves either stored direction`() {
        val l = link("a", "b")
        assertEquals("b", l.otherPaperCloudId("a"))
        assertEquals("a", l.otherPaperCloudId("b"))
    }

    @Test
    fun `relatedPapersFor is empty when paper has no cloudId`() {
        val a = paper(1, "")
        assertTrue(relatedPapersFor(a, listOf(link("", "b")), listOf(a)).isEmpty())
    }

    @Test
    fun `relatedPapersFor resolves links in both stored directions, newest first`() {
        val a = paper(1, "a")
        val b = paper(2, "b")
        val c = paper(3, "c")
        val links = listOf(
            link("a", "b", created = LocalDate.of(2026, 1, 1)),
            link("c", "a", created = LocalDate.of(2026, 2, 1))
        )
        val result = relatedPapersFor(a, links, listOf(a, b, c))
        assertEquals(listOf(3L, 2L), result.map { it.id })
    }

    @Test
    fun `relatedPapersFor excludes a link pointing at a deleted paper`() {
        val a = paper(1, "a")
        val links = listOf(link("a", "gone"))
        assertTrue(relatedPapersFor(a, links, listOf(a)).isEmpty())
    }

    @Test
    fun `relatedPapersFor dedups if somehow linked twice`() {
        val a = paper(1, "a")
        val b = paper(2, "b")
        val links = listOf(link("a", "b"), link("b", "a"))
        val result = relatedPapersFor(a, links, listOf(a, b))
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `linkablePapersFor excludes self and already-linked papers`() {
        val a = paper(1, "a")
        val b = paper(2, "b")
        val c = paper(3, "c")
        val links = listOf(link("a", "b"))
        val result = linkablePapersFor(a, links, listOf(a, b, c))
        assertEquals(listOf(3L), result.map { it.id })
    }

    @Test
    fun `canLinkPapers rejects self-link`() {
        assertFalse(canLinkPapers("a", "a", emptyList()))
    }

    @Test
    fun `canLinkPapers rejects blank cloudIds`() {
        assertFalse(canLinkPapers("", "b", emptyList()))
        assertFalse(canLinkPapers("a", "", emptyList()))
    }

    @Test
    fun `canLinkPapers rejects an existing link in either stored direction`() {
        assertFalse(canLinkPapers("a", "b", listOf(link("a", "b"))))
        assertFalse(canLinkPapers("a", "b", listOf(link("b", "a"))))
    }

    @Test
    fun `canLinkPapers allows a new valid pair`() {
        assertTrue(canLinkPapers("a", "b", listOf(link("a", "c"))))
    }
}
