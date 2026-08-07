package com.example.apextracker

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.UUID

private const val ATTACH_TAG = "NoteAttachments"
private const val ATTACH_DIR = "note_attachments"

/** The app-private directory holding note image attachments, created on demand. */
private fun attachmentsDir(context: Context): File =
    File(context.filesDir, ATTACH_DIR).apply { if (!exists()) mkdirs() }

/**
 * The [File] for a stored attachment [filename], or null if the name isn't one we'd have written.
 *
 * Two independent checks, because this resolves a name that can come from a hand-edited backup
 * file (Issue #193): the cheap syntactic [isSafeAttachmentName], and a canonical-path containment
 * test that catches anything the first one misses. Callers treat null as "no such attachment",
 * which is the right outcome for a name that shouldn't exist.
 */
fun noteAttachmentFile(context: Context, filename: String): File? {
    if (!isSafeAttachmentName(filename)) {
        Log.w(ATTACH_TAG, "Rejecting unsafe attachment name")
        return null
    }
    val dir = attachmentsDir(context)
    val file = File(dir, filename)
    return if (file.canonicalPath.startsWith(dir.canonicalPath + File.separator)) file else {
        Log.w(ATTACH_TAG, "Rejecting attachment name that escapes the attachments directory")
        null
    }
}

/**
 * Copies the picked [uri]'s bytes into app-private storage and returns the generated filename, or
 * null if the copy fails. The photo-picker grants only transient read access to [uri], so the note
 * has to own its own copy — the original may be unavailable later (Issue #127).
 */
fun saveNoteAttachment(context: Context, uri: Uri): String? {
    val filename = "img_${UUID.randomUUID()}.jpg"
    // Always non-null: a generated UUID name is by construction a bare filename. The check is the
    // compiler's, not a real branch.
    val destination = noteAttachmentFile(context, filename) ?: return null
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        filename
    } catch (e: Exception) {
        Log.w(ATTACH_TAG, "Failed to copy attachment from $uri", e)
        null
    }
}

/** Deletes a stored attachment file; no-ops if it's already gone or the name isn't ours. */
fun deleteNoteAttachment(context: Context, filename: String) {
    try {
        noteAttachmentFile(context, filename)?.delete()
    } catch (e: Exception) {
        Log.w(ATTACH_TAG, "Failed to delete attachment $filename", e)
    }
}
