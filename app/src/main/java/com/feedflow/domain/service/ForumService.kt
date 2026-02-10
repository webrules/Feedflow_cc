package com.feedflow.domain.service

import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.ThreadDetailResult

interface ForumService {
    val name: String
    val id: String
    val logo: Int // drawable resource

    suspend fun fetchCategories(): List<Community>

    suspend fun fetchCategoryThreads(
        categoryId: String,
        communities: List<Community>,
        page: Int = 1
    ): List<ForumThread>

    suspend fun fetchThreadDetail(
        threadId: String,
        page: Int = 1
    ): ThreadDetailResult

    suspend fun postComment(
        topicId: String,
        categoryId: String,
        content: String
    )

    suspend fun createThread(
        categoryId: String,
        title: String,
        content: String
    )

    fun getWebURL(thread: ForumThread): String

    fun supportsPosting(): Boolean = false

    fun requiresLogin(): Boolean = false
}
