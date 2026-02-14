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
class TwoLibraService @Inject constructor(
    private val client: OkHttpClient,
    private val encryptionHelper: com.feedflow.data.local.encryption.EncryptionHelper
) : ForumService {

    override val name: String = "2Libra"
    override val id: String = "2libra"
    override val logo: Int = R.drawable.ic_2libra

    private val baseUrl = "https://2libra.com"

    private val categories = listOf(
        Community("forum", "全部", "All posts", id),
        Community("product", "产品", "Product discussions", id),
        Community("ai", "AI", "Artificial intelligence", id),
        Community("technology", "技术", "Technology discussions", id),
        Community("creative-sharing", "创作分享", "Creative sharing", id),
        Community("efficiency-tools", "效率工具", "Efficiency tools", id),
        Community("team-collaboration", "管理协作", "Team collaboration", id),
        Community("career", "职场发展", "Career development", id),
        Community("startup-side-business", "创业副业", "Startups & side business", id),
        Community("daily-life", "生活日常", "Daily life", id),
        Community("reading-movies", "阅读观影", "Reading & movies", id),
        Community("games", "游戏娱乐", "Games & entertainment", id),
        Community("q-and-a", "问与答", "Q&A", id),
        Community("travel", "旅行户外", "Travel & outdoors", id),
        Community("tech-products", "科技数码", "Tech products", id),
        Community("social", "社交活动", "Social activities", id),
        Community("emotion-life", "情感生活", "Emotional life", id)
    )

    override suspend fun fetchCategories(): List<Community> = categories

    override suspend fun fetchCategoryThreads(
        categoryId: String,
        communities: List<Community>,
        page: Int
    ): List<ForumThread> = withContext(Dispatchers.IO) {
        val url = if (page <= 1) {
            "$baseUrl/node/$categoryId"
        } else {
            "$baseUrl/node/$categoryId?page=$page"
        }
        val request = buildRequest(url)

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return@withContext emptyList()

        val community = communities.find { it.id == categoryId } ?: categories.first()
        parseThreadList(html, community)
    }

    override suspend fun fetchThreadDetail(threadId: String, page: Int): ThreadDetailResult =
        withContext(Dispatchers.IO) {
            // threadId is encoded as "nodeSlug:shortId"
            val parts = threadId.split(":", limit = 2)
            val nodeSlug = parts.getOrElse(0) { "" }
            val shortId = parts.getOrElse(1) { threadId }

            val url = if (page <= 1) {
                "$baseUrl/post/$nodeSlug/$shortId"
            } else {
                "$baseUrl/post/$nodeSlug/$shortId?page=$page"
            }
            val request = buildRequest(url)

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: throw Exception("Failed to load thread")

            parseThreadDetail(html, threadId)
        }

    override suspend fun postComment(topicId: String, categoryId: String, content: String) {
        throw UnsupportedOperationException("Posting is not supported on 2Libra")
    }

    override suspend fun createThread(categoryId: String, title: String, content: String) {
        throw UnsupportedOperationException("Thread creation is not supported on 2Libra")
    }

    override fun getWebURL(thread: ForumThread): String {
        val parts = thread.id.split(":", limit = 2)
        val nodeSlug = parts.getOrElse(0) { "" }
        val shortId = parts.getOrElse(1) { thread.id }
        return "$baseUrl/post/$nodeSlug/$shortId"
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

        // 2Libra renders thread list items as <li> with links to /post/{slug}/{id}
        val items = doc.select("li:has(a[href^=/post/])")

        items.forEach { item ->
            try {
                val titleLink = item.selectFirst("a[href^=/post/]") ?: return@forEach
                val title = titleLink.text().trim()
                if (title.isBlank()) return@forEach

                val href = titleLink.attr("href") // /post/{nodeSlug}/{shortId}
                val pathParts = href.removePrefix("/post/").split("/")
                val nodeSlug = pathParts.getOrElse(0) { "" }
                val shortId = pathParts.getOrElse(1) { "" }
                if (shortId.isBlank()) return@forEach

                // Composite ID: nodeSlug:shortId
                val threadId = "$nodeSlug:$shortId"

                // Author info
                val authorLink = item.selectFirst("a[href^=/user/]")
                val authorName = authorLink?.text()?.trim() ?: "Anonymous"

                val avatarImg = item.selectFirst("img[alt]")
                val authorAvatar = avatarImg?.let { resolveUrl(it.attr("src")) } ?: ""

                // Comment count - look for a number that represents replies
                // The comment count is typically shown near the thread entry
                val replyCount = item.select("span, div")
                    .mapNotNull { el ->
                        el.ownText().trim().toIntOrNull()
                    }
                    .firstOrNull() ?: 0

                // Time info
                val timeEl = item.selectFirst("time")
                val timeAgo = timeEl?.text()?.trim() ?: ""

                // Node/subcategory label
                val nodeLink = item.selectFirst("a[href^=/node/]")
                val nodeName = nodeLink?.text()?.trim()

                val author = User(
                    id = authorName,
                    username = authorName,
                    avatar = authorAvatar
                )

                val threadCommunity = if (nodeName != null && community.id == "forum") {
                    // When browsing "all", show the actual node name
                    Community(
                        id = nodeSlug,
                        name = nodeName,
                        description = "",
                        category = id
                    )
                } else {
                    community
                }

                threads.add(
                    ForumThread(
                        id = threadId,
                        title = title,
                        content = "",
                        author = author,
                        community = threadCommunity,
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
        val title = doc.selectFirst("h1")?.text()?.trim() ?: ""

        // Parse author info
        val authorLink = doc.selectFirst("a[href^=/user/]")
        val authorName = authorLink?.text()?.trim() ?: "Anonymous"
        val authorAvatar = doc.selectFirst("img[alt]")?.let { resolveUrl(it.attr("src")) } ?: ""

        val author = User(
            id = authorName,
            username = authorName,
            avatar = authorAvatar
        )

        // Parse content from the post body
        val contentElement = doc.selectFirst(".post-body")
            ?: doc.selectFirst("article")
            ?: doc.selectFirst("[class*=post-body]")
        val content = contentElement?.let { HtmlUtils.cleanHtml(it.html()) } ?: ""

        // Determine community from breadcrumb or node link
        val nodeLink = doc.selectFirst("a[href^=/node/]")
        val communityName = nodeLink?.text()?.trim() ?: "2Libra"
        val communityId = nodeLink?.attr("href")
            ?.removePrefix("/node/")?.substringBefore("/")?.substringBefore("?") ?: "forum"

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

        // Look for comment containers - 2Libra uses floor numbering (1楼, 2楼, etc.)
        // Comments appear after the main post body
        // Try to find comment blocks by looking for repeated user+time+content patterns
        // Each comment has: user link, time element, and content div
        val commentBlocks = doc.select("[id^=comment-], [data-comment-id], .comment-item")
        if (commentBlocks.isNotEmpty()) {
            commentBlocks.forEach { block ->
                parseCommentBlock(block, comments)
            }
        } else {
            // Fallback: look for floor indicators (1楼, 2楼, etc.)
            val floorElements = doc.getElementsMatchingOwnText("\\d+\\s*楼")
            for (floorEl in floorElements) {
                val container = floorEl.parent() ?: continue
                // Walk up to find the comment container
                val commentContainer = container.parent() ?: container
                parseCommentBlock(commentContainer, comments)
            }
        }

        // Parse pagination
        val pageLinks = doc.select("a[href*=page=]")
        val totalPages = pageLinks.mapNotNull { link ->
            link.attr("href").substringAfter("page=").substringBefore("&").toIntOrNull()
        }.maxOrNull()

        return ThreadDetailResult(
            thread = thread,
            comments = comments,
            totalPages = totalPages
        )
    }

    private fun parseCommentBlock(
        block: org.jsoup.nodes.Element,
        comments: MutableList<Comment>
    ) {
        try {
            val commentId = block.id().ifBlank {
                block.attr("data-comment-id").ifBlank {
                    comments.size.toString()
                }
            }

            val commentAuthorLink = block.selectFirst("a[href^=/user/]")
            val commentAuthor = commentAuthorLink?.text()?.trim() ?: "Anonymous"
            val commentAvatar = block.selectFirst("img[alt]")
                ?.let { resolveUrl(it.attr("src")) } ?: ""

            val timeEl = block.selectFirst("time")
            val timeAgo = timeEl?.text()?.trim() ?: ""

            // Find content: look for the largest text block that isn't the author or time
            val contentEl = block.selectFirst(".post-body, [class*=comment-content], [class*=reply-content]")
            val commentContent = if (contentEl != null) {
                HtmlUtils.cleanHtml(contentEl.html())
            } else {
                // Fallback: get text from the block excluding author and time
                val blockClone = block.clone()
                blockClone.select("a[href^=/user/], time, img").remove()
                val text = blockClone.text().trim()
                    .replace(Regex("\\d+\\s*楼"), "") // Remove floor indicators
                    .trim()
                text
            }

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

    private fun resolveUrl(url: String): String {
        if (url.isBlank()) return ""
        if (url.startsWith("//")) return "https:$url"
        if (url.startsWith("/")) return "$baseUrl$url"
        if (!url.startsWith("http")) return "https://r2.2libra.com/$url"
        return url
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
