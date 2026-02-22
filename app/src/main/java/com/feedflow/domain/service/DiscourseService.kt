package com.feedflow.domain.service

import com.feedflow.R
import com.feedflow.data.model.Comment
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.ThreadDetailResult
import com.feedflow.data.model.User
import com.feedflow.data.remote.api.DiscourseApi
import com.feedflow.data.remote.dto.DiscoursePost
import com.feedflow.util.HtmlUtils
import com.feedflow.util.TimeUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscourseService @Inject constructor(
    private val api: DiscourseApi
) : ForumService {

    override val name: String = "Linux.do"
    override val id: String = "linux_do"
    override val logo: Int = R.drawable.ic_linuxdo

    private val baseUrl = "https://linux.do"

    override suspend fun fetchCategories(): List<Community> {
        val response = api.getCategories()
        return response.categoryList?.categories?.map { category ->
            Community(
                id = category.slug,
                name = category.name,
                description = category.description ?: "",
                category = id,
                activeToday = category.topicCount ?: 0,
                onlineNow = category.postCount ?: 0
            )
        } ?: emptyList()
    }

    override suspend fun fetchCategoryThreads(
        categoryId: String,
        communities: List<Community>,
        page: Int
    ): List<ForumThread> {
        val response = api.getCategoryTopics(categoryId, page - 1) // Discourse uses 0-indexed pages
        val community = communities.find { it.id == categoryId }
            ?: Community(categoryId, categoryId, "", id)

        return response.topicList?.topics?.map { topic ->
            ForumThread(
                id = topic.id.toString(),
                title = topic.fancyTitle ?: topic.title,
                content = "", // Content loaded in detail view
                author = User(
                    id = topic.posters?.firstOrNull()?.userId?.toString() ?: "unknown",
                    username = "User",
                    avatar = ""
                ),
                community = community,
                timeAgo = TimeUtils.calculateTimeAgo(topic.createdAt),
                likeCount = topic.likeCount ?: 0,
                commentCount = topic.postsCount - 1 // Minus the OP
            )
        } ?: emptyList()
    }

    override suspend fun fetchThreadDetail(threadId: String, page: Int): ThreadDetailResult {
        val response = api.getTopic(threadId.toInt(), page)

        val posts = response.postStream?.posts ?: emptyList()
        val firstPost = posts.firstOrNull()

        val author = if (firstPost != null) {
            User(
                id = firstPost.userId?.toString() ?: firstPost.username,
                username = firstPost.username,
                avatar = buildAvatarUrl(firstPost.avatarTemplate),
                role = firstPost.primaryGroupName
            )
        } else {
            response.details?.createdBy?.let {
                User(
                    id = it.id.toString(),
                    username = it.username,
                    avatar = buildAvatarUrl(it.avatarTemplate ?: "")
                )
            } ?: User("unknown", "Unknown", "")
        }

        val content = firstPost?.let { HtmlUtils.cleanHtml(it.cooked) } ?: ""

        val community = Community(
            id = response.categoryId?.toString() ?: "general",
            name = "Linux.do",
            description = "",
            category = id
        )

        val thread = ForumThread(
            id = response.id.toString(),
            title = response.fancyTitle ?: response.title,
            content = content,
            author = author,
            community = community,
            timeAgo = TimeUtils.calculateTimeAgo(response.createdAt),
            likeCount = response.likeCount ?: 0,
            commentCount = response.postsCount - 1
        )

        val comments = posts.drop(1).map { post -> postToComment(post) }

        val totalPages = if (response.postsCount > 20) {
            (response.postsCount / 20) + 1
        } else null

        return ThreadDetailResult(
            thread = thread,
            comments = comments,
            totalPages = totalPages
        )
    }

    override suspend fun postComment(topicId: String, categoryId: String, content: String) {
        // TODO: Implement posting with proper authentication
        throw UnsupportedOperationException("Posting requires authentication")
    }

    override suspend fun createThread(categoryId: String, title: String, content: String) {
        // TODO: Implement thread creation with proper authentication
        throw UnsupportedOperationException("Thread creation requires authentication")
    }

    override fun getWebURL(thread: ForumThread): String {
        return "$baseUrl/t/${thread.id}"
    }

    override fun supportsPosting(): Boolean = true
    override fun requiresLogin(): Boolean = true

    private fun buildAvatarUrl(template: String): String {
        if (template.isBlank()) return ""
        val url = template.replace("{size}", "64")
        return if (url.startsWith("http")) url else "$baseUrl$url"
    }

    private fun postToComment(post: DiscoursePost): Comment {
        val role = when {
            post.admin == true -> "Admin"
            post.moderator == true -> "Moderator"
            else -> post.primaryGroupName
        }

        return Comment(
            id = post.id.toString(),
            author = User(
                id = post.userId?.toString() ?: post.username,
                username = post.username,
                avatar = buildAvatarUrl(post.avatarTemplate),
                role = role
            ),
            content = HtmlUtils.cleanHtml(post.cooked),
            timeAgo = TimeUtils.calculateTimeAgo(post.createdAt),
            likeCount = post.score?.toInt() ?: 0
        )
    }
}
