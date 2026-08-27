package com.example.apextracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperLinkDao {
    /** Every link; the ViewModel/UI resolve which of a paper's links belong to it. */
    @Query("SELECT * FROM paper_links")
    fun getAll(): Flow<List<PaperLink>>

    @Query("SELECT * FROM paper_links WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getByCloudId(cloudId: String): PaperLink?

    /** Undirected existence check before creating a link, so the same pair can't be linked twice. */
    @Query(
        "SELECT * FROM paper_links WHERE " +
            "(paperCloudId = :a AND relatedPaperCloudId = :b) OR " +
            "(paperCloudId = :b AND relatedPaperCloudId = :a) LIMIT 1"
    )
    suspend fun findExisting(a: String, b: String): PaperLink?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: PaperLink): Long

    @Update
    suspend fun updateLink(link: PaperLink)

    @Delete
    suspend fun deleteLink(link: PaperLink)

    @Query("SELECT * FROM paper_links")
    suspend fun getAllOneShot(): List<PaperLink>

    /** Wipes the table — mirrors the other DAOs' full-dataset-restore hook (Issue #121). */
    @Query("DELETE FROM paper_links")
    suspend fun clearAll()
}
