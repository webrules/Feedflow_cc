package com.feedflow.domain.service

import com.feedflow.R
import com.feedflow.data.model.Comment
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.ThreadDetailResult
import com.feedflow.data.model.User
import com.feedflow.data.remote.api.HackerNewsApi
import com.feedflow.util.HtmlUtils
import com.feedflow.util.TimeUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HackerNewsService @Inject constructor(
    private val api: HackerNewsApi
) : ForumService {

    override val name: String = "Hacker News"
    override val id: String = "hackernews"
    override val logo: Int = R.drawable.ic_hackernews

    private val categories = listOf(
        Community("topstories", "Top Stories", "Most popular stories", "hackernews"),
        Community("newstories", "New", "Newest stories", "hackernews"),
        Community("beststories", "Best", "Best stories", "hackernews"),
        Community("showstories", "Show HN", "Show HN submissions", "hackernews"),
        Community("askstories", "Ask HN", "Ask HN questions", "hackernews"),
        Community("jobstories", "Jobs", "Job postings", "hackernews")
    )

    override suspend fun fetchCategories(): List<Community> = categories

    override suspend fun fetchCategoryThreads(
        categoryId: String,
        communities: List<Community>,
        page: Int
    ): List<ForumThread> = coroutineScope {
        val storyIds = when (categoryId) {
            "topstories" -> api.getTopStories()
            "newstories" -> api.getNewStories()
            "beststories" -> api.getBestStories()
            "showstories" -> api.getShowStories()
            "askstories" -> api.getAskStories()
            "jobstories" -> api.getJobStories()
            else -> api.getTopStories()
        }

        val pageSize = 20
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, storyIds.size)

        if (startIndex >= storyIds.size) return@coroutineScope emptyList()

        val pageIds = storyIds.subList(startIndex, endIndex)
        val community = communities.find { it.id == categoryId } ?: categories.first()

        pageIds.map { storyId ->
            async {
                try {
                    val item = api.getItem(storyId) ?: return@async null
                    itemToThread(item, community)
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    override suspend fun fetchThreadDetail(threadId: String, page: Int): ThreadDetailResult = coroutineScope {
        val item = api.getItem(threadId.toInt())
            ?: throw Exception("Story not found")

        val community = Community(
            id = "topstories",
            name = "Hacker News",
            description = "",
            category = "hackernews"
        )

        val thread = itemToThread(item, community)

        // Fetch comments (first 20 kids)
        val comments = item.kids?.take(20)?.map { commentId ->
            async {
                try {
                    val commentItem = api.getItem(commentId)
                    commentItem?.let { itemToComment(it) }
                } catch (e: Exception) {
                    null
                }
            }
        }?.awaitAll()?.filterNotNull() ?: emptyList()

        ThreadDetailResult(
            thread = thread,
            comments = comments,
            totalPages = null
        )
    }

    override suspend fun postComment(topicId: String, categoryId: String, content: String) {
        throw UnsupportedOperationException("Posting not supported for Hacker News")
    }

    override suspend fun createThread(categoryId: String, title: String, content: String) {
        throw UnsupportedOperationException("Thread creation not supported for Hacker News")
    }

    override fun getWebURL(thread: ForumThread): String {
        return "https://news.ycombinator.com/item?id=${thread.id}"
    }

    override fun requiresLogin(): Boolean = true

    private fun itemToThread(item: com.feedflow.data.remote.dto.HNItem, community: Community): ForumThread {
        val author = User(
            id = item.by ?: "unknown",
            username = item.by ?: "unknown",
            avatar = ""
        )

        val content = if (!item.url.isNullOrBlank()) {
            "[LINK:${item.url}|${item.url}]\n\n${HtmlUtils.cleanHtml(item.text ?: "")}"
        } else {
            HtmlUtils.cleanHtml(item.text ?: "")
        }

        return ForumThread(
            id = item.id.toString(),
            title = item.title ?: "",
            content = content,
            author = author,
            community = community,
            timeAgo = TimeUtils.calculateTimeAgo(item.time.toLong()),
            likeCount = item.score ?: 0,
            commentCount = item.descendants ?: 0
        )
    }

    private fun itemToComment(item: com.feedflow.data.remote.dto.HNItem): Comment {
        val author = User(
            id = item.by ?: "unknown",
            username = item.by ?: "unknown",
            avatar = ""
        )

        return Comment(
            id = item.id.toString(),
            author = author,
            content = HtmlUtils.cleanHtml(item.text ?: ""),
            timeAgo = TimeUtils.calculateTimeAgo(item.time.toLong()),
            likeCount = 0
        )
    }
}
