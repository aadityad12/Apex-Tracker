package com.example.apextracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperTopicDao {
    /** Every topic including paused ones — scoring/UI need paused topics to render their history. */
    @Query("SELECT * FROM paper_topics ORDER BY createdDate ASC, id ASC")
    fun getAll(): Flow<List<PaperTopic>>

    @Query("SELECT * FROM paper_topics WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getByCloudId(cloudId: String): PaperTopic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: PaperTopic): Long

    @Update
    suspend fun updateTopic(topic: PaperTopic)

    @Delete
    suspend fun deleteTopic(topic: PaperTopic)

    @Query("SELECT * FROM paper_topics")
    suspend fun getAllOneShot(): List<PaperTopic>

    /** Wipes the table — mirrors the other DAOs' full-dataset-restore hook (Issue #121). */
    @Query("DELETE FROM paper_topics")
    suspend fun clearAll()
}
