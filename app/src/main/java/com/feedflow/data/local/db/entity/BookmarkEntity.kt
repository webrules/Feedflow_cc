package com.feedflow.data.local.db.entity

import androidx.room.Entity

@Entity(
    tableName = "bookmarks",
    primaryKeys = ["threadId", "serviceId"]
)
data class BookmarkEntity(
    val threadId: String,
    val serviceId: String,
    val data: String, // JSON encoded ForumThread
    val timestamp: Long
)
