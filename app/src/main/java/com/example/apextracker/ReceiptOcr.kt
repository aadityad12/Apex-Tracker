package com.example.apextracker

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import kotlin.coroutines.resume

private const val TAG = "ReceiptOcr"

/**
 * The one place ML Kit is touched (Issue #46).
 *
 * Everything else about receipt scanning is pure text handling in `ReceiptParse.kt`, so the
 * dependency is confined to this file and could be dropped by deleting it and one call site.
 *
 * The **Play-Services-delivered** recognizer, not the bundled `com.google.mlkit:text-recognition`
 * one: bundling the model costs 45MB of APK (39MB of native model libraries, one copy per ABI)
 * versus 0.35MB for this, and the app already requires Play Services for Firebase Auth and
 * Credential Manager, so bundling buys nothing.
 * The cost is that the model is downloaded — the manifest asks for it at install time, and a
 * device that hasn't got it yet simply fails to recognise, which is already handled below as an
 * ordinary unreadable photo.
 *
 * Recognition itself runs on-device. The image is read and discarded — nothing is copied into app
 * storage, and the recognized text never leaves the device.
 */
suspend fun recognizeReceipt(
    context: Context,
    imageUri: Uri,
    today: LocalDate = LocalDate.now()
): ReceiptGuess? {
    val text = runCatching { readImageText(context, imageUri) }
        .onFailure { Log.w(TAG, "Receipt text recognition failed", it) }
        .getOrNull()
        ?: return null
    if (text.isBlank()) return null
    return extractReceiptFields(text, today)
}

/**
 * Bridges ML Kit's Task API onto a coroutine; the recognizer is closed on every path.
 *
 * Returns text reflowed from the recognised lines' bounding boxes rather than `Text.text`. That
 * property groups by block, and a receipt's two columns are usually two blocks — so it hands back
 * every label, then every amount, and `TOTAL` never shares a line with its figure. See
 * [reflowReceiptLines]. If a box is missing (nothing in the API guarantees one), fall back to the
 * plain text rather than dropping the line.
 */
private suspend fun readImageText(context: Context, imageUri: Uri): String {
    val image = InputImage.fromFilePath(context, imageUri)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val elements = result.textBlocks
                        .flatMap { block -> block.lines }
                        .mapNotNull { line ->
                            line.boundingBox?.let { box ->
                                ReceiptTextElement(line.text, box.top, box.bottom, box.left)
                            }
                        }
                    continuation.resume(
                        if (elements.isEmpty()) result.text else reflowReceiptLines(elements)
                    )
                }
                .addOnFailureListener { error -> continuation.cancel(error) }
        }
    } finally {
        recognizer.close()
    }
}
