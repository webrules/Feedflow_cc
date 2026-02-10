package com.feedflow.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.feedflow.data.local.db.entity.AiSummaryEntity

@Dao
interface AiSummaryDao {
    @Query("SELECT * FROM ai_summaries WHERE threadId = :threadId")
    suspend fun getSummary(threadId: String): AiSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSummary(summary: AiSummaryEntity)

    @Query("DELETE FROM ai_summaries WHERE threadId = :threadId")
    suspend fun deleteSummary(threadId: String)

    @Query("DELETE FROM ai_summaries WHERE createdAt < :olderThan")
    suspend fun deleteOldSummaries(olderThan: Long)

    @Query("DELETE FROM ai_summaries")
    suspend fun deleteAll()
}
