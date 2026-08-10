package com.example.apextracker

/**
 * Pure encoding for a note's attachment list (Issue #127): a newline-separated list of filenames.
 * Newline (not comma) is the separator because it can't appear in the generated UUID filenames and
 * needs no escaping. Blank/whitespace entries are dropped so a trailing separator can't yield a
 * phantom attachment. Unit-tested in NoteAttachmentsTest.
 */
fun attachmentList(encoded: String): List<String> =
    encoded.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

fun joinAttachments(files: List<String>): String =
    files.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")

/** Adds [file] to [encoded] unless already present; returns the new encoded string. */
fun addAttachment(encoded: String, file: String): String =
    joinAttachments((attachmentList(encoded) + file).distinct())

/** Removes [file] from [encoded]; returns the new encoded string. */
fun removeAttachment(encoded: String, file: String): String =
    joinAttachments(attachmentList(encoded).filterNot { it == file })

/**
 * Whether [name] is a bare filename safe to resolve inside the attachments directory.
 *
 * Attachment names normally come from `saveNoteAttachment`, which generates `img_<uuid>.jpg`. But
 * the column also round-trips through the backup JSON as a plain string, so a hand-edited file can
 * put anything here — and `noteAttachmentFile` used to join it straight onto the directory, so
 * `../../databases/budget_database` resolved outside the sandbox and `deleteNoteAttachment` would
 * happily delete it (Issue #193).
 *
 * Rejects separators and the traversal entries rather than trying to normalize: there is no
 * legitimate attachment name containing a slash, so the strict rule costs nothing.
 */
fun isSafeAttachmentName(name: String): Boolean =
    name.isNotBlank() &&
        !name.contains('/') &&
        !name.contains('\\') &&
        name != "." &&
        name != ".."

/** Drops any attachment name that isn't a bare filename. Applied when parsing a backup file. */
fun sanitizeAttachments(encoded: String): String =
    joinAttachments(attachmentList(encoded).filter(::isSafeAttachmentName))
