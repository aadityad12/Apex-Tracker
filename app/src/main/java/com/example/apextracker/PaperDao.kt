package com.example.apextracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {
    /** Every paper; the ViewModel splits queue/history and the Dashboard counts read days. */
    @Query("SELECT * FROM papers ORDER BY addedDate ASC, id ASC")
    fun getAllPapers(): Flow<List<Paper>>

    /** Dedup lookup for re-adding a paper the API already resolved once. */
    @Query("SELECT * FROM papers WHERE s2Id = :s2Id AND s2Id != '' LIMIT 1")
    suspend fun getByS2Id(s2Id: String): Paper?

    /** Dedup lookup for offline seeds (no s2Id) — the landing URL is their stable identity. */
    @Query("SELECT * FROM papers WHERE url = :url AND url != '' LIMIT 1")
    suspend fun getByUrl(url: String): Paper?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: Paper): Long

    @Update
    suspend fun updatePaper(paper: Paper)

    @Delete
    suspend fun deletePaper(paper: Paper)

    @Query("SELECT * FROM papers")
    suspend fun getAllPapersOneShot(): List<Paper>

    /** Wipes the table — mirrors the other DAOs' full-dataset-restore hook (Issue #121). */
    @Query("DELETE FROM papers")
    suspend fun clearAll()
}
