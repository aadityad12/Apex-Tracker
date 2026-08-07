package com.example.apextracker

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

private const val RESET_TAG = "LocalDataReset"

/**
 * Removes every trace of the previously signed-in account's data from this device (Issue #186).
 *
 * Called from MainActivity when [shouldResetLocalDataForUid] says the uid changed, and it must
 * complete *before* `performInitialSync` runs — that method pushes all local rows to whichever
 * uid is signed in, so anything still in Room at that point is uploaded into the new account.
 *
 * Stop [SyncCoordinator] before calling this: its listeners would otherwise race the wipe and
 * re-insert the very rows being cleared.
 */
suspend fun clearLocalUserData(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
    // Alarms are keyed on Room row ids, which clearAllTables() invalidates (it also resets the
    // AUTOINCREMENT counters, so the next account's reminders reuse those ids). Cancel first or
    // the old account's pending alarms fire against the new account's rows.
    try {
        db.reminderDao().getActiveReminders().first().forEach {
            ReminderScheduler.cancel(context, it.id)
        }
    } catch (e: Exception) {
        Log.w(RESET_TAG, "Failed to cancel reminder alarms before wipe", e)
    }

    db.clearAllTables()

    // Note images live outside the DB, so clearAllTables() leaves them orphaned on disk.
    try {
        File(context.filesDir, "note_attachments").listFiles()?.forEach { it.delete() }
    } catch (e: Exception) {
        Log.w(RESET_TAG, "Failed to delete note attachments", e)
    }

    // DataStores holding data *about* the user, as opposed to device preferences. Deliberately
    // not cleared: reminder/note/study/currency/security settings are this device's preferences
    // and survive an account switch the same way dark mode does. account_identity is excluded
    // for the obvious reason — the caller rewrites it immediately after.
    listOf(
        context.budgetPrefsDataStore,      // the overall monthly spending ceiling
        context.apexTipDataStore,          // cached AI text derived from the previous user's totals
        context.papersDiscoveryDataStore   // reading-discovery bookkeeping
    ).forEach { store ->
        try {
            store.edit { it.clear() }
        } catch (e: Exception) {
            Log.w(RESET_TAG, "Failed to clear a DataStore during wipe", e)
        }
    }

    clearFirestoreCache()

    Log.i(RESET_TAG, "Local data wiped for account switch")
}

/**
 * Drops Firestore's on-disk cache, which holds copies of the previous account's documents.
 *
 * Best-effort by design. `clearPersistence()` is only legal while the client is stopped, so the
 * instance has to be terminated first, and either call can fail if something still holds a
 * listener. A failure here is not security-critical — the cache is app-private and only ever
 * populated from paths the *previous* user's own client read, and the new user's client never
 * queries them — so it is logged rather than propagated. The Room wipe above is the part that
 * actually stops the leak.
 *
 * Safe to terminate because FirebaseManager resolves `FirebaseFirestore.getInstance()` per access
 * rather than caching it at construction; the next call gets a fresh, live client.
 */
private suspend fun clearFirestoreCache() {
    try {
        val firestore = FirebaseFirestore.getInstance()
        firestore.terminate().await()
        firestore.clearPersistence().await()
    } catch (e: Exception) {
        Log.w(RESET_TAG, "Could not clear the Firestore cache; continuing", e)
    }
}
