package com.feedflow.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_summaries")
data class AiSummaryEntity(
    @PrimaryKey
    val threadId: String,
    val summary: String,
    val createdAt: Long
) {
    fun isFresh(maxAgeSeconds: Long): Boolean {
        val ageSeconds = (System.currentTimeMillis() - createdAt) / 1000
        return ageSeconds < maxAgeSeconds
    }
}
