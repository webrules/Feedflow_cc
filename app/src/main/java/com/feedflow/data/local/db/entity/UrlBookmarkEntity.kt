package com.feedflow.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "url_bookmarks")
data class UrlBookmarkEntity(
    @PrimaryKey
    val url: String,
    val title: String,
    val timestamp: Long
)
