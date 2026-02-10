package com.feedflow.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ForumThread(
    val id: String,
    val title: String,
    val content: String,
    val author: User,
    val community: Community,
    val timeAgo: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val tags: List<String>? = null
)

@Serializable
data class ThreadDetailResult(
    val thread: ForumThread,
    val comments: List<Comment>,
    val totalPages: Int? = null
)

@Serializable
data class ThreadDetailCache(
    val thread: ForumThread,
    val comments: List<Comment>
)
