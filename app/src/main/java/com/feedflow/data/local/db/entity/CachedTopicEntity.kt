package com.feedflow.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_topics")
data class CachedTopicEntity(
    @PrimaryKey
    val cacheKey: String,
    val data: String, // JSON encoded List<ForumThread>
    val timestamp: Long
)
