package com.feedflow.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cover_summaries")
data class CoverSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val summary: String,
    val postsJson: String,
    val createdAt: Long,
    val hnCount: Int,
    val v2exCount: Int,
    val fourD4yCount: Int
) {
    fun isFresh(maxAgeMs: Long = 6 * 60 * 60 * 1000L): Boolean {
        return System.currentTimeMillis() - createdAt < maxAgeMs
    }
}
