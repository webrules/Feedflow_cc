package com.feedflow.domain.service

import com.feedflow.R
import com.feedflow.data.local.encryption.EncryptionHelper
import com.feedflow.data.model.Comment
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.ThreadDetailResult
import com.feedflow.data.model.User
import com.feedflow.data.remote.api.HackerNewsApi
import com.feedflow.util.HtmlUtils
import com.feedflow.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HackerNewsService @Inject constructor(
    private val api: HackerNewsApi,
    private val client: OkHttpClient,
    private val encryptionHelper: EncryptionHelper
) : ForumService {

    override val name: String = "Hacker News"
    override val id: String = "hackernews"
    override val logo: Int = R.drawable.ic_hackernews

    private val baseUrl = "https://news.ycombinator.com"
    private val cookieMutex = Mutex()

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

    override suspend fun postComment(topicId: String, categoryId: String, content: String) =
        withContext(Dispatchers.IO) {
            // Step 1: Fetch the item page to get the CSRF token (hmac)
            val itemUrl = "$baseUrl/item?id=$topicId"
            val getRequest = buildRequest(itemUrl).newBuilder()
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val getResponse = client.newCall(getRequest).execute()
            updateCookies(getResponse)
            val html = getResponse.body?.string()
                ?: throw Exception("Failed to load item page")

            // Extract CSRF token (hmac) from the form
            val doc = Jsoup.parse(html)
            val hmacInput = doc.selectFirst("input[name=hmac]")
                ?: throw Exception("Authentication required. Please login via web view.")
            val hmac = hmacInput.attr("value")

            // Step 2: Submit the comment
            val formBody = FormBody.Builder()
                .add("parent", topicId)
                .add("text", content)
                .add("hmac", hmac)
                .add("goto", "item?id=$topicId")
                .build()

            val postRequest = buildRequest("$baseUrl/comment").newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", itemUrl)
                .post(formBody)
                .build()

            val postResponse = client.newCall(postRequest).execute()
            updateCookies(postResponse)

            if (!postResponse.isSuccessful) {
                throw Exception("Failed to post comment: HTTP ${postResponse.code}")
            }

            // Check for error messages in response
            val responseBody = postResponse.body?.string() ?: ""
            if (responseBody.contains("error") || responseBody.contains("bad") || 
                responseBody.contains("You have to be logged in")) {
                throw Exception("Failed to post comment. Please check your login status.")
            }
        }

    override suspend fun createThread(categoryId: String, title: String, content: String) =
        withContext(Dispatchers.IO) {
            // Step 1: Fetch the submit page to get the CSRF token (hmac)
            val submitUrl = "$baseUrl/submit"
            val getRequest = buildRequest(submitUrl).newBuilder()
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val getResponse = client.newCall(getRequest).execute()
            updateCookies(getResponse)
            val html = getResponse.body?.string()
                ?: throw Exception("Failed to load submit page")

            // Extract CSRF token (hmac) from the form
            val doc = Jsoup.parse(html)
            val hmacInput = doc.selectFirst("input[name=hmac]")
                ?: throw Exception("Authentication required. Please login via web view.")
            val hmac = hmacInput.attr("value")

            // Step 2: Submit the new story
            // HN allows either URL or text, not both. If content looks like URL, use it as URL
            val formBuilder = FormBody.Builder()
                .add("title", title)
                .add("hmac", hmac)

            // Check if content is a URL
            val urlRegex = Regex("^(https?://)\\S+")
            if (urlRegex.matches(content)) {
                formBuilder.add("url", content)
            } else {
                formBuilder.add("text", content)
            }

            val formBody = formBuilder.build()

            val postRequest = buildRequest("$baseUrl/r").newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", submitUrl)
                .post(formBody)
                .build()

            val postResponse = client.newCall(postRequest).execute()
            updateCookies(postResponse)

            if (!postResponse.isSuccessful) {
                throw Exception("Failed to create thread: HTTP ${postResponse.code}")
            }

            // Check for error messages
            val responseBody = postResponse.body?.string() ?: ""
            if (responseBody.contains("error") || responseBody.contains("bad") ||
                responseBody.contains("You have to be logged in")) {
                throw Exception("Failed to create thread. Please check your login status.")
            }
        }

    override fun getWebURL(thread: ForumThread): String {
        return "$baseUrl/item?id=${thread.id}"
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

    private suspend fun updateCookies(response: okhttp3.Response) {
        val setCookieHeaders = response.headers("Set-Cookie")
        if (setCookieHeaders.isEmpty()) return

        cookieMutex.withLock {
            val existingCookiesStr = encryptionHelper.getCookies(id) ?: ""
            val cookieMap = mutableMapOf<String, String>()

            if (existingCookiesStr.isNotBlank()) {
                existingCookiesStr.split(";").forEach {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) {
                        cookieMap[parts[0].trim()] = parts[1].trim()
                    }
                }
            }

            val url = response.request.url
            for (header in setCookieHeaders) {
                val cookie = Cookie.parse(url, header)
                if (cookie != null) {
                    if (cookie.expiresAt < System.currentTimeMillis()) {
                        cookieMap.remove(cookie.name)
                    } else {
                        cookieMap[cookie.name] = cookie.value
                    }
                }
            }

            if (cookieMap.isNotEmpty()) {
                val newCookieString = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
                encryptionHelper.saveCookies(id, newCookieString)
            }
        }
    }

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

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
