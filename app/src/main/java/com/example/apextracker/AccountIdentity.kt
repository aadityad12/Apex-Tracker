package com.example.apextracker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Remembers which Firebase account the data currently sitting in Room belongs to (Issue #186).
 *
 * Room has no notion of an owner, and `performInitialSync` deliberately pushes *every* local row
 * to the signed-in uid — that is how data created while signed out reaches the cloud (Issue #4).
 * Without this, an account switch is indistinguishable from "returning user who edited things
 * offline", so signing in as B would upload A's notes and budget items into B's Firestore.
 *
 * Deliberately its own tiny DataStore rather than a key on an existing one: it is the only piece
 * of state that must be readable *before* any sync decision is made, and colocating it with, say,
 * budget settings would mean [clearLocalUserData] has to be careful not to wipe the very record
 * that tells it a wipe is needed.
 */
val Context.accountIdentityDataStore: DataStore<Preferences> by preferencesDataStore(name = "account_identity")

class AccountIdentity(private val context: Context) {
    private companion object {
        val LAST_UID = stringPreferencesKey("last_signed_in_uid")
    }

    /** The uid the local dataset belongs to, or null if this install has never been signed in. */
    suspend fun lastSignedInUid(): String? =
        context.accountIdentityDataStore.data.map { it[LAST_UID] }.first()

    suspend fun setLastSignedInUid(uid: String) {
        context.accountIdentityDataStore.edit { it[LAST_UID] = uid }
    }
}

/**
 * Whether the local dataset must be wiped before syncing as [newUid].
 *
 * A null [previousUid] means this install has never been signed in, so whatever is in Room is
 * this user's own offline data and must be *kept* — pushing it up is the intended behaviour.
 * A different non-null uid means the data belongs to someone else and must not be uploaded,
 * displayed, or merged.
 *
 * Pure so the decision is unit-tested without Android (see AccountIdentityTest), matching
 * [shouldRunInitialSync] in InitialSyncGate.kt.
 */
fun shouldResetLocalDataForUid(previousUid: String?, newUid: String): Boolean =
    previousUid != null && previousUid != newUid
