package com.feedflow.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_threads")
data class CachedThreadEntity(
    @PrimaryKey
    val threadId: String,
    val data: String, // JSON encoded ThreadDetailCache
    val timestamp: Long
)
