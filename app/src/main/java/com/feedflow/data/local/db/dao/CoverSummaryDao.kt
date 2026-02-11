package com.feedflow.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.feedflow.data.local.db.entity.CoverSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoverSummaryDao {
    @Query("SELECT * FROM cover_summaries ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): CoverSummaryEntity?

    @Query("SELECT * FROM cover_summaries ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<CoverSummaryEntity>>

    @Query("SELECT * FROM cover_summaries ORDER BY createdAt DESC")
    suspend fun getAll(): List<CoverSummaryEntity>

    @Insert
    suspend fun insert(entity: CoverSummaryEntity): Long

    @Query("DELETE FROM cover_summaries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM cover_summaries WHERE createdAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
