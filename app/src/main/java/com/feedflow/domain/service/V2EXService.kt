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
class V2EXService @Inject constructor(
    private val client: OkHttpClient,
    private val encryptionHelper: com.feedflow.data.local.encryption.EncryptionHelper
) : ForumService {

    override val name: String = "V2EX"
    override val id: String = "v2ex"
    override val logo: Int = R.drawable.ic_v2ex

    private val baseUrl = "https://v2ex.com"

    private val categories = listOf(
        Community("tech", "技术", "Technology discussions", id),
        Community("creative", "创意", "Creative content", id),
        Community("play", "好玩", "Fun stuff", id),
        Community("apple", "Apple", "Apple products", id),
        Community("jobs", "酷工作", "Job opportunities", id),
        Community("deals", "交易", "Buy and sell", id),
        Community("city", "城市", "City life", id),
        Community("qna", "问与答", "Q&A", id),
        Community("hot", "最热", "Hot topics", id),
        Community("all", "全部", "All topics", id)
    )

    override suspend fun fetchCategories(): List<Community> = categories

    override suspend fun fetchCategoryThreads(
        categoryId: String,
        communities: List<Community>,
        page: Int
    ): List<ForumThread> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/?tab=$categoryId"
        val request = buildRequest(url)

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return@withContext emptyList()

        val community = communities.find { it.id == categoryId } ?: categories.first()
        parseThreadList(html, community)
    }

    override suspend fun fetchThreadDetail(threadId: String, page: Int): ThreadDetailResult =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/t/$threadId?p=$page"
            val request = buildRequest(url)

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: throw Exception("Failed to load thread")

            parseThreadDetail(html, threadId)
        }

    override suspend fun postComment(topicId: String, categoryId: String, content: String) {
        // TODO: Implement with CSRF token handling
        throw UnsupportedOperationException("Posting requires authentication and CSRF token")
    }

    override suspend fun createThread(categoryId: String, title: String, content: String) {
        throw UnsupportedOperationException("Thread creation requires authentication")
    }

    override fun getWebURL(thread: ForumThread): String {
        return "$baseUrl/t/${thread.id}"
    }

    override fun supportsPosting(): Boolean = true
    override fun requiresLogin(): Boolean = true

    private fun buildRequest(url: String): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
        val cookies = encryptionHelper.getCookies(id)
        if (!cookies.isNullOrBlank()) {
            builder.header("Cookie", cookies)
        }
        return builder.build()
    }

    private fun parseThreadList(html: String, community: Community): List<ForumThread> {
        val doc = Jsoup.parse(html)
        val threads = mutableListOf<ForumThread>()

        doc.select("div.cell.item").forEach { cell ->
            try {
                val titleLink = cell.selectFirst("a.topic-link") ?: return@forEach
                val title = titleLink.text()
                val href = titleLink.attr("href")
                val threadId = href.substringAfter("/t/").substringBefore("#").substringBefore("?")

                val authorLink = cell.selectFirst("a[href^=/member/]")
                val authorName = authorLink?.text() ?: "Anonymous"
                val authorAvatar = cell.selectFirst("img.avatar")?.attr("src") ?: ""

                val replyCount = cell.selectFirst("a.count_livid")?.text()?.toIntOrNull() ?: 0

                val author = User(
                    id = authorName,
                    username = authorName,
                    avatar = if (authorAvatar.startsWith("//")) "https:$authorAvatar" else authorAvatar
                )

                threads.add(
                    ForumThread(
                        id = threadId,
                        title = title,
                        content = "",
                        author = author,
                        community = community,
                        timeAgo = "",
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
        val title = doc.selectFirst("h1")?.text() ?: ""

        // Parse author info
        val authorElement = doc.selectFirst("div.header a[href^=/member/]")
        val authorName = authorElement?.text() ?: "Anonymous"
        val authorAvatar = doc.selectFirst("div.header img.avatar")?.attr("src") ?: ""

        val author = User(
            id = authorName,
            username = authorName,
            avatar = if (authorAvatar.startsWith("//")) "https:$authorAvatar" else authorAvatar
        )

        // Parse content
        val contentElement = doc.selectFirst("div.topic_content")
        val content = contentElement?.let { HtmlUtils.cleanHtml(it.html()) } ?: ""

        // Parse community/node
        val nodeElement = doc.selectFirst("a.node")
        val communityName = nodeElement?.text() ?: "V2EX"
        val communityId = nodeElement?.attr("href")?.substringAfter("/go/") ?: "all"

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

        // Parse comments
        val comments = mutableListOf<Comment>()
        doc.select("div[id^=r_]").forEach { reply ->
            try {
                val commentId = reply.id().substringAfter("r_")
                val commentAuthor = reply.selectFirst("a[href^=/member/]")?.text() ?: "Anonymous"
                val commentAvatar = reply.selectFirst("img.avatar")?.attr("src") ?: ""
                val commentContent = reply.selectFirst("div.reply_content")?.let {
                    HtmlUtils.cleanHtml(it.html())
                } ?: ""
                val timeAgo = reply.selectFirst("span.ago")?.text() ?: ""

                comments.add(
                    Comment(
                        id = commentId,
                        author = User(
                            id = commentAuthor,
                            username = commentAuthor,
                            avatar = if (commentAvatar.startsWith("//")) "https:$commentAvatar" else commentAvatar
                        ),
                        content = commentContent,
                        timeAgo = timeAgo,
                        likeCount = 0
                    )
                )
            } catch (e: Exception) {
                // Skip malformed comments
            }
        }

        // Calculate total pages
        val pageLinks = doc.select("a.page_normal")
        val totalPages = pageLinks.lastOrNull()?.text()?.toIntOrNull()

        return ThreadDetailResult(
            thread = thread,
            comments = comments,
            totalPages = totalPages
        )
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
