package com.feedflow.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String,
    val author: User,
    val content: String,
    val timeAgo: String,
    val likeCount: Int = 0,
    val replies: List<Comment>? = null
)
