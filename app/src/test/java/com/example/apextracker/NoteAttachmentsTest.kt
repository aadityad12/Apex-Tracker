package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Issue #127 — note attachment filename-list encoding. */
class NoteAttachmentsTest {

    @Test
    fun `round-trips a list`() {
        val files = listOf("img_a.jpg", "img_b.jpg")
        assertEquals(files, attachmentList(joinAttachments(files)))
    }

    @Test
    fun `blank and whitespace entries are dropped`() {
        assertEquals(emptyList<String>(), attachmentList(""))
        assertEquals(emptyList<String>(), attachmentList("\n  \n"))
        assertEquals(listOf("img_a.jpg"), attachmentList("img_a.jpg\n\n"))
    }

    @Test
    fun `add is idempotent and preserves order`() {
        val once = addAttachment("", "img_a.jpg")
        assertEquals(listOf("img_a.jpg"), attachmentList(once))
        val twice = addAttachment(once, "img_a.jpg")
        assertEquals(listOf("img_a.jpg"), attachmentList(twice))
        val two = addAttachment(once, "img_b.jpg")
        assertEquals(listOf("img_a.jpg", "img_b.jpg"), attachmentList(two))
    }

    @Test
    fun `remove drops only the named file`() {
        val both = joinAttachments(listOf("img_a.jpg", "img_b.jpg"))
        assertEquals(listOf("img_b.jpg"), attachmentList(removeAttachment(both, "img_a.jpg")))
        assertEquals(listOf("img_a.jpg", "img_b.jpg"), attachmentList(removeAttachment(both, "missing.jpg")))
    }

    @Test
    fun `traversal names are rejected`() {
        // Issue #193: these reach isSafeAttachmentName only via a hand-edited backup file, but
        // noteAttachmentFile resolves whatever it is given against the attachments directory,
        // so anything with a separator in it could escape the sandbox.
        assertFalse(isSafeAttachmentName("../../databases/budget_database"))
        assertFalse(isSafeAttachmentName("../shared_prefs/device_identity.xml"))
        assertFalse(isSafeAttachmentName("sub/dir.jpg"))
        assertFalse(isSafeAttachmentName(".."))
        assertFalse(isSafeAttachmentName("."))
        assertFalse(isSafeAttachmentName(""))
        assertFalse(isSafeAttachmentName("   "))
    }

    @Test
    fun `generated names stay valid`() {
        // saveNoteAttachment's own format must survive the check, or attachments break entirely.
        assertTrue(isSafeAttachmentName("img_3f2b7c10-9a1e-4d2b-8c77-0a1b2c3d4e5f.jpg"))
    }

    @Test
    fun `sanitizeAttachments keeps the safe entries and drops the rest`() {
        val mixed = joinAttachments(listOf("img_a.jpg", "../../databases/budget_database", "img_b.jpg"))
        assertEquals(listOf("img_a.jpg", "img_b.jpg"), attachmentList(sanitizeAttachments(mixed)))
    }
}
