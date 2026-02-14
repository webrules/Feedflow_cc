package com.feedflow.domain.service

import com.feedflow.R
import com.feedflow.data.model.Comment
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.ThreadDetailResult
import com.feedflow.data.model.User
import com.feedflow.util.HtmlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeSeekService @Inject constructor(
    private val client: OkHttpClient,
    private val encryptionHelper: com.feedflow.data.local.encryption.EncryptionHelper
) : ForumService {

    override val name: String = "NodeSeek"
    override val id: String = "nodeseek"
    override val logo: Int = R.drawable.ic_nodeseek

    private val baseUrl = "https://www.nodeseek.com"

    private val categories = listOf(
        Community("tech", "技术", "Technology discussions", id),
        Community("dev", "Dev", "Development", id),
        Community("info", "情报", "News & info", id),
        Community("review", "测评", "Reviews", id),
        Community("daily", "日常", "Daily life", id),
        Community("trade", "交易", "Trading", id),
        Community("promotion", "推广", "Promotions", id),
        Community("sandbox", "沙盒", "Sandbox", id)
    )

    override suspend fun fetchCategories(): List<Community> = categories

    override suspend fun fetchCategoryThreads(
        categoryId: String,
        communities: List<Community>,
        page: Int
    ): List<ForumThread> = withContext(Dispatchers.IO) {
        val url = if (page <= 1) {
            "$baseUrl/categories/$categoryId"
        } else {
            "$baseUrl/categories/$categoryId/page-$page"
        }
        val request = buildRequest(url)

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return@withContext emptyList()

        val community = communities.find { it.id == categoryId } ?: categories.first()
        parseThreadList(html, community)
    }

    override suspend fun fetchThreadDetail(threadId: String, page: Int): ThreadDetailResult =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/post-$threadId-$page"
            val request = buildRequest(url)

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: throw Exception("Failed to load thread")

            parseThreadDetail(html, threadId)
        }

    override suspend fun postComment(topicId: String, categoryId: String, content: String) {
        throw UnsupportedOperationException("Posting is not supported on NodeSeek")
    }

    override suspend fun createThread(categoryId: String, title: String, content: String) {
        throw UnsupportedOperationException("Thread creation is not supported on NodeSeek")
    }

    override fun getWebURL(thread: ForumThread): String {
        return "$baseUrl/post-${thread.id}-1"
    }

    override fun supportsPosting(): Boolean = false
    override fun requiresLogin(): Boolean = true

    private fun buildRequest(url: String): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        val cookies = encryptionHelper.getCookies(id)
        if (!cookies.isNullOrBlank()) {
            builder.header("Cookie", cookies)
        }
        return builder.build()
    }

    private fun parseThreadList(html: String, community: Community): List<ForumThread> {
        val doc = Jsoup.parse(html)
        val threads = mutableListOf<ForumThread>()

        // NodeSeek uses .post-list-item for thread entries
        val items = doc.select(".post-list-item").ifEmpty {
            doc.select("[class*=post-list] > div, [class*=post-list] > li")
        }

        items.forEach { item ->
            try {
                // Title link points to /post-{id}-{page}
                val titleLink = item.selectFirst("a[href^=/post-]")
                    ?: item.selectFirst(".post-title a")
                    ?: return@forEach
                val title = titleLink.text().trim()
                if (title.isBlank()) return@forEach

                val href = titleLink.attr("href")
                val threadId = href.removePrefix("/post-")
                    .substringBefore("-")
                    .substringBefore("#")
                    .substringBefore("?")

                // Author info
                val authorEl = item.selectFirst(".post-username, .author-name, a[href^=/space/]")
                val authorName = authorEl?.text()?.trim() ?: "Anonymous"

                val avatarEl = item.selectFirst("img.avatar-normal, img.avatar, img[class*=avatar]")
                val authorAvatar = avatarEl?.let { resolveUrl(it.attr("src")) } ?: ""

                // Reply count
                val replyCount = item.selectFirst(".reply-count, [class*=comment-count], [class*=reply]")
                    ?.text()?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0

                // Time info
                val timeAgo = item.selectFirst("time, .post-time, [class*=time], [class*=date]")
                    ?.text()?.trim() ?: ""

                val author = User(
                    id = authorName,
                    username = authorName,
                    avatar = authorAvatar
                )

                threads.add(
                    ForumThread(
                        id = threadId,
                        title = title,
                        content = "",
                        author = author,
                        community = community,
                        timeAgo = timeAgo,
                        likeCount = 0,
                        commentCount = replyCount
                    )
                )
            } catch (e: Exception) {
                // Skip malformed entries
            }
        }

        return threads
    }

    private fun parseThreadDetail(html: String, threadId: String): ThreadDetailResult {
        val doc = Jsoup.parse(html)

        // Parse title
        val title = doc.selectFirst("h1, .post-title")?.text()?.trim() ?: ""

        // Parse author info from the first post / header
        val headerEl = doc.selectFirst(".post-header, .author-info, .post-author")
        val authorName = headerEl?.selectFirst(".post-username, .author-name, a[href^=/space/]")
            ?.text()?.trim()
            ?: doc.selectFirst(".post-username, .author-name")?.text()?.trim()
            ?: "Anonymous"
        val authorAvatar = headerEl?.selectFirst("img.avatar-normal, img.avatar, img[class*=avatar]")
            ?.let { resolveUrl(it.attr("src")) }
            ?: doc.selectFirst("img.avatar-normal, img.avatar")?.let { resolveUrl(it.attr("src")) }
            ?: ""

        val author = User(
            id = authorName,
            username = authorName,
            avatar = authorAvatar
        )

        // Parse content from the first post
        val contentElement = doc.selectFirst("article.post-content, .post-content, .post-body")
        val content = contentElement?.let { HtmlUtils.cleanHtml(it.html()) } ?: ""

        // Determine community from breadcrumb or tag
        val categoryEl = doc.selectFirst("a[href^=/categories/]")
        val communityName = categoryEl?.text()?.trim() ?: "NodeSeek"
        val communityId = categoryEl?.attr("href")
            ?.removePrefix("/categories/")?.substringBefore("/") ?: "tech"

        val community = Community(
            id = communityId,
            name = communityName,
            description = "",
            category = id
        )

        val thread = ForumThread(
            id = threadId,
            title = title,
            content = content,
            author = author,
            community = community,
            timeAgo = "",
            likeCount = 0,
            commentCount = 0
        )

        // Parse comments (skip the first post which is the OP)
        val comments = mutableListOf<Comment>()
        val commentElements = doc.select(".post-comment, .comment-item, [id^=comment-], [class*=reply-item]")

        commentElements.forEach { commentEl ->
            try {
                val commentId = commentEl.id().ifBlank {
                    commentEl.attr("data-id").ifBlank {
                        comments.size.toString()
                    }
                }

                val commentAuthor = commentEl.selectFirst(".post-username, .author-name, a[href^=/space/]")
                    ?.text()?.trim() ?: "Anonymous"
                val commentAvatar = commentEl.selectFirst("img.avatar-normal, img.avatar, img[class*=avatar]")
                    ?.let { resolveUrl(it.attr("src")) } ?: ""
                val commentContent = commentEl.selectFirst(".post-content, .comment-content, .reply-content")
                    ?.let { HtmlUtils.cleanHtml(it.html()) } ?: ""
                val timeAgo = commentEl.selectFirst("time, .post-time, [class*=time]")
                    ?.text()?.trim() ?: ""

                if (commentContent.isNotBlank()) {
                    comments.add(
                        Comment(
                            id = commentId,
                            author = User(
                                id = commentAuthor,
                                username = commentAuthor,
                                avatar = commentAvatar
                            ),
                            content = commentContent,
                            timeAgo = timeAgo,
                            likeCount = 0
                        )
                    )
                }
            } catch (e: Exception) {
                // Skip malformed comments
            }
        }

        // Parse pagination
        val pageLinks = doc.select(".nsk-pager a, .pagination a")
        val totalPages = pageLinks.mapNotNull { it.text().toIntOrNull() }.maxOrNull()

        return ThreadDetailResult(
            thread = thread,
            comments = comments,
            totalPages = totalPages
        )
    }

    private fun resolveUrl(url: String): String {
        if (url.isBlank()) return ""
        if (url.startsWith("//")) return "https:$url"
        if (url.startsWith("/")) return "$baseUrl$url"
        return url
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
