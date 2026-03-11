package com.feedflow.domain.service

import com.feedflow.R
import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.data.model.Comment
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.ThreadDetailResult
import com.feedflow.data.model.User
import com.feedflow.util.HtmlUtils
import com.feedflow.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.Collections
import org.jsoup.Jsoup
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZhihuService @Inject constructor(
    private val client: OkHttpClient,
    private val preferencesManager: PreferencesManager,
    private val encryptionHelper: com.feedflow.data.local.encryption.EncryptionHelper
) : ForumService {

    override val name: String = "Zhihu"
    override val id: String = "zhihu"
    override val logo: Int = R.drawable.ic_zhihu

    private val baseUrl = "https://www.zhihu.com"
    private val json = Json { ignoreUnknownKeys = true }
    private val downvotedIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val questionDataCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, Pair<String, String>>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<String, String>>?): Boolean {
                return size > 100
            }
        }
    )
    private val stateMutex = Mutex()

    private val categories = listOf(
        Community("recommend", "Recommend", "Recommended content", id),
        Community("hot", "Hot", "Hot topics", id)
    )

    override suspend fun fetchCategories(): List<Community> {
        // Load downvoted IDs
        loadDownvotedIds()
        return categories
    }

    override suspend fun fetchCategoryThreads(
        categoryId: String,
        communities: List<Community>,
        page: Int
    ): List<ForumThread> = withContext(Dispatchers.IO) {
        val url = when (categoryId) {
            "hot" -> "$baseUrl/api/v3/feed/topstory/hot-lists/total?limit=20&desktop=true"
            else -> "$baseUrl/api/v3/feed/topstory/recommend?desktop=true&limit=20"
        }

        val request = Request.Builder()
            .url(url)
            .headers(buildHeaders())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext emptyList()

        android.util.Log.d("Zhihu", "fetchCategoryThreads($categoryId): status=${response.code}, bodyLen=${responseBody.length}")

        val community = communities.find { it.id == categoryId } ?: categories.first()
        when (categoryId) {
            "hot" -> parseHotListResponse(responseBody, community)
            else -> parseFeedResponse(responseBody, community)
        }
    }

    override suspend fun fetchThreadDetail(threadId: String, page: Int): ThreadDetailResult =
        withContext(Dispatchers.IO) {
            val parts = threadId.split("_")
            val type = parts.getOrNull(0) ?: "answer"
            val actualId = parts.getOrNull(1) ?: threadId

            val url = when (type) {
                "answer" -> "$baseUrl/api/v4/answers/$actualId?include=content,voteup_count,comment_count"
                "article" -> "$baseUrl/api/v4/articles/$actualId?include=content,voteup_count,comment_count"
                "question" -> "$baseUrl/api/v4/questions/$actualId?include=detail,excerpt,answer_count,visit_count,comment_count,follower_count"
                else -> "$baseUrl/api/v4/answers/$actualId?include=content,voteup_count,comment_count"
            }

            val request = Request.Builder()
                .url(url)
                .headers(buildHeaders())
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            // Parse detail from API or fall back to cached data
            val detailResult = if (response.isSuccessful && responseBody != null) {
                try {
                    parseDetailResponse(responseBody, threadId, type)
                } catch (e: Exception) {
                    android.util.Log.e("Zhihu", "Error parsing detail response", e)
                    scrapeThreadFromWeb(threadId, type, actualId)
                }
            } else {
                android.util.Log.d("Zhihu", "fetchThreadDetail: API returned ${response.code}, using web scraping fallback")
                scrapeThreadFromWeb(threadId, type, actualId)
            }

            // Fetch comments/answers
            val comments = try {
                when (type) {
                    "question" -> fetchQuestionAnswers(actualId, page)
                    else -> fetchComments(type + "s", actualId, page)
                }
            } catch (e: Exception) {
                android.util.Log.e("Zhihu", "Error fetching comments: ${e.message}")
                emptyList()
            }

            ThreadDetailResult(
                thread = detailResult.thread,
                comments = comments,
                totalPages = null
            )
        }

    override suspend fun postComment(topicId: String, categoryId: String, content: String) {
        throw UnsupportedOperationException("Posting not supported for Zhihu")
    }

    override suspend fun createThread(categoryId: String, title: String, content: String) {
        throw UnsupportedOperationException("Thread creation not supported for Zhihu")
    }

    override fun getWebURL(thread: ForumThread): String {
        val parts = thread.id.split("_")
        val type = parts.getOrNull(0) ?: "answer"
        val actualId = parts.getOrNull(1) ?: thread.id

        return when (type) {
            "article" -> "$baseUrl/p/$actualId"
            "question" -> "$baseUrl/question/$actualId"
            "answer" -> "$baseUrl/answer/$actualId"
            else -> "$baseUrl/answer/$actualId"
        }
    }

    // Downvote functionality
    suspend fun downvoteItem(threadId: String) {
        downvotedIds.add(threadId)
        saveDownvotedIds()
    }

    suspend fun undoDownvote(threadId: String) {
        downvotedIds.remove(threadId)
        saveDownvotedIds()
    }

    fun isDownvoted(threadId: String): Boolean = threadId in downvotedIds

    private suspend fun loadDownvotedIds() {
        val idsString = preferencesManager.downvotedIds.first()
        if (!idsString.isNullOrBlank()) {
            downvotedIds.clear()
            downvotedIds.addAll(idsString.split(","))
        }
    }

    private suspend fun saveDownvotedIds() {
        preferencesManager.setDownvotedIds(downvotedIds.joinToString(","))
    }

    private fun buildHeaders(): okhttp3.Headers {
        val builder = okhttp3.Headers.Builder()
            .add("x-api-version", "3.1.8")
            .add("x-app-version", "10.61.0")
            .add("x-requested-with", "fetch")
            .add("Referer", "$baseUrl/")
            .add("User-Agent", USER_AGENT)

        // Inject stored cookies for authenticated access
        val cookies = encryptionHelper.getCookies(id)
        if (!cookies.isNullOrBlank()) {
            builder.add("Cookie", cookies)
        }

        return builder.build()
    }

    private fun parseFeedResponse(responseBody: String, community: Community): List<ForumThread> {
        val threads = mutableListOf<ForumThread>()

        try {
            val jsonObj = json.parseToJsonElement(responseBody).jsonObject
            val data = jsonObj["data"]?.jsonArray ?: return emptyList()

            android.util.Log.d("Zhihu", "parseFeedResponse: data size=${data.size}")

            data.forEach { item ->
                try {
                    val itemObj = item.jsonObject

                    // Skip advertisements
                    val itemType = itemObj["type"]?.jsonPrimitive?.contentOrNull ?: ""
                    if (itemType == "feed_advert") return@forEach

                    val target = itemObj["target"]?.jsonObject ?: return@forEach

                    val type = target["type"]?.jsonPrimitive?.contentOrNull ?: "answer"
                    // Skip unsupported types
                    if (type !in listOf("answer", "article", "question", "pin")) return@forEach

                    // Handle id as both int and string
                    val idValue = target["id"]?.jsonPrimitive?.let {
                        it.intOrNull?.toString() ?: it.contentOrNull
                    } ?: return@forEach
                    val threadId = "${type}_$idValue"

                    // Skip downvoted items
                    if (isDownvoted(threadId)) return@forEach

                    val title = target["question"]?.jsonObject?.get("title")?.jsonPrimitive?.contentOrNull
                        ?: target["title"]?.jsonPrimitive?.contentOrNull
                        ?: ""

                    val excerpt = target["excerpt"]?.jsonPrimitive?.contentOrNull ?: ""
                    val voteupCount = target["voteup_count"]?.jsonPrimitive?.intOrNull ?: 0
                    val commentCount = target["comment_count"]?.jsonPrimitive?.intOrNull ?: 0

                    val authorObj = target["author"]?.jsonObject
                    val authorName = authorObj?.get("name")?.jsonPrimitive?.contentOrNull ?: "Anonymous"
                    val authorAvatar = authorObj?.get("avatar_url")?.jsonPrimitive?.contentOrNull ?: ""
                    val authorId = authorObj?.get("id")?.jsonPrimitive?.contentOrNull ?: authorName

                    val createdTime = target["created_time"]?.jsonPrimitive?.intOrNull
                    val timeAgo = createdTime?.let { TimeUtils.calculateTimeAgo(it.toLong()) } ?: ""

                    threads.add(
                        ForumThread(
                            id = threadId,
                            title = title,
                            content = HtmlUtils.decodeHtmlEntities(excerpt),
                            author = User(authorId, authorName, authorAvatar),
                            community = community,
                            timeAgo = timeAgo,
                            likeCount = voteupCount,
                            commentCount = commentCount,
                            tags = listOf(type)
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("Zhihu", "Error parsing feed item", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Zhihu", "Error parsing feed response", e)
        }

        android.util.Log.d("Zhihu", "parseFeedResponse: parsed ${threads.size} threads")
        return threads
    }

    private fun parseHotListResponse(responseBody: String, community: Community): List<ForumThread> {
        val threads = mutableListOf<ForumThread>()

        try {
            val jsonObj = json.parseToJsonElement(responseBody).jsonObject
            val data = jsonObj["data"]?.jsonArray ?: return emptyList()

            android.util.Log.d("Zhihu", "parseHotListResponse: data size=${data.size}")

            data.forEach { item ->
                try {
                    val itemObj = item.jsonObject
                    val target = itemObj["target"]?.jsonObject ?: return@forEach

                    // Hot list uses card_id format "Q_123456" for questions
                    val cardId = itemObj["card_id"]?.jsonPrimitive?.contentOrNull ?: ""
                    val questionId = cardId.removePrefix("Q_")
                    if (questionId.isBlank()) return@forEach

                    val threadId = "question_$questionId"

                    // Skip downvoted items
                    if (isDownvoted(threadId)) return@forEach

                    // Hot list structure: target.title_area.text, target.excerpt_area.text, etc.
                    val title = target["title_area"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
                    val excerpt = target["excerpt_area"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""

                    // Metrics
                    val metricsArea = target["metrics_area"]?.jsonObject
                    val metricsText = metricsArea?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
                    val answerCount = metricsArea?.get("answer_count")?.jsonPrimitive?.intOrNull ?: 0

                    // Heat text from detail_text (e.g. "123万热度")
                    val detailText = itemObj["detail_text"]?.jsonPrimitive?.contentOrNull ?: ""

                    // Try to get author from children array
                    val children = itemObj["children"]?.jsonArray
                    val firstChild = children?.firstOrNull()?.jsonObject
                    val authorObj = firstChild?.get("author")?.jsonObject
                    val authorName = authorObj?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
                    val authorAvatar = authorObj?.get("avatar_url")?.jsonPrimitive?.contentOrNull ?: ""

                    // Cache question data for detail view fallback
                    questionDataCache[questionId] = Pair(title, excerpt)

                    threads.add(
                        ForumThread(
                            id = threadId,
                            title = title,
                            content = excerpt,
                            author = User(questionId, authorName, authorAvatar),
                            community = community,
                            timeAgo = detailText,
                            likeCount = 0,
                            commentCount = answerCount,
                            tags = listOf("question")
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("Zhihu", "Error parsing hot list item", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Zhihu", "Error parsing hot list response", e)
        }

        android.util.Log.d("Zhihu", "parseHotListResponse: parsed ${threads.size} threads")
        return threads
    }

    private fun parseDetailResponse(responseBody: String, threadId: String, type: String): ThreadDetailResult {
        val jsonObj = json.parseToJsonElement(responseBody).jsonObject

        val title: String
        val cleanContent: String
        val voteupCount: Int
        val commentCount: Int
        val authorName: String
        val authorAvatar: String
        val authorId: String
        val timeAgo: String

        if (type == "question") {
            // Question detail has different structure
            title = jsonObj["title"]?.jsonPrimitive?.contentOrNull ?: ""
            val detail = jsonObj["detail"]?.jsonPrimitive?.contentOrNull
                ?: jsonObj["excerpt"]?.jsonPrimitive?.contentOrNull ?: ""
            cleanContent = HtmlUtils.cleanHtml(detail)
            voteupCount = jsonObj["follower_count"]?.jsonPrimitive?.intOrNull ?: 0
            commentCount = jsonObj["answer_count"]?.jsonPrimitive?.intOrNull ?: 0
            authorName = ""
            authorAvatar = ""
            authorId = ""
            val createdTime = jsonObj["created"]?.jsonPrimitive?.intOrNull
                ?: jsonObj["created_time"]?.jsonPrimitive?.intOrNull
            timeAgo = createdTime?.let { TimeUtils.calculateTimeAgo(it.toLong()) } ?: ""
        } else {
            // Answer/Article detail
            title = jsonObj["question"]?.jsonObject?.get("title")?.jsonPrimitive?.contentOrNull
                ?: jsonObj["title"]?.jsonPrimitive?.contentOrNull
                ?: ""
            val content = jsonObj["content"]?.jsonPrimitive?.contentOrNull ?: ""
            cleanContent = HtmlUtils.cleanHtml(content)
            voteupCount = jsonObj["voteup_count"]?.jsonPrimitive?.intOrNull ?: 0
            commentCount = jsonObj["comment_count"]?.jsonPrimitive?.intOrNull ?: 0
            val authorObj = jsonObj["author"]?.jsonObject
            authorName = authorObj?.get("name")?.jsonPrimitive?.contentOrNull ?: "Anonymous"
            authorAvatar = authorObj?.get("avatar_url")?.jsonPrimitive?.contentOrNull ?: ""
            authorId = authorObj?.get("id")?.jsonPrimitive?.contentOrNull ?: authorName
            val createdTime = jsonObj["created_time"]?.jsonPrimitive?.intOrNull
            timeAgo = createdTime?.let { TimeUtils.calculateTimeAgo(it.toLong()) } ?: ""
        }

        val community = Community("zhihu", "Zhihu", "", id)

        val thread = ForumThread(
            id = threadId,
            title = title,
            content = cleanContent,
            author = User(authorId, authorName, authorAvatar),
            community = community,
            timeAgo = timeAgo,
            likeCount = voteupCount,
            commentCount = commentCount,
            tags = listOf(type)
        )

        return ThreadDetailResult(
            thread = thread,
            comments = emptyList(),
            totalPages = null
        )
    }

    private fun buildFallbackDetail(threadId: String, type: String, actualId: String): ThreadDetailResult {
        val cached = questionDataCache[actualId]
        val title = cached?.first ?: ""
        val excerpt = cached?.second ?: ""

        val community = Community("zhihu", "Zhihu", "", id)
        val thread = ForumThread(
            id = threadId,
            title = title,
            content = excerpt,
            author = User("", "", ""),
            community = community,
            timeAgo = "",
            likeCount = 0,
            commentCount = 0,
            tags = listOf(type)
        )

        return ThreadDetailResult(
            thread = thread,
            comments = emptyList(),
            totalPages = null
        )
    }

    private fun scrapeThreadFromWeb(threadId: String, type: String, actualId: String): ThreadDetailResult {
        val webUrl = when (type) {
            "article" -> "$baseUrl/p/$actualId"
            "question" -> "$baseUrl/question/$actualId"
            "answer" -> "$baseUrl/answer/$actualId"
            else -> "$baseUrl/answer/$actualId"
        }

        return try {
            val request = Request.Builder()
                .url(webUrl)
                .headers(buildHeaders())
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return buildFallbackDetail(threadId, type, actualId)

            android.util.Log.d("Zhihu", "scrapeThreadFromWeb: fetched ${html.length} chars from $webUrl")

            val doc = Jsoup.parse(html)

            val title = doc.selectFirst("h1.QuestionHeader-title, .Post-Title, .ContentItem-title")?.text()?.trim() ?: ""
            val content = doc.selectFirst(".RichContent-inner, .RichText, .Post-RichText")?.let {
                HtmlUtils.cleanHtml(it.html())
            } ?: ""

            val authorName = doc.selectFirst(".AuthorInfo-name, .UserLink-link")?.text()?.trim() ?: "Anonymous"
            val authorAvatar = doc.selectFirst(".AuthorInfo-avatarImage img, .Avatar")?.attr("src") ?: ""

            val voteCount = doc.selectFirst(".VoteButton--up")?.text()?.let {
                Regex("\\d+").find(it)?.value?.toIntOrNull() ?: 0
            } ?: 0

            val commentCount = doc.selectFirst(".ContentItem-actions button:contains(评论)")?.text()?.let {
                Regex("\\d+").find(it)?.value?.toIntOrNull() ?: 0
            } ?: 0

            val community = Community("zhihu", "Zhihu", "", id)
            val thread = ForumThread(
                id = threadId,
                title = title.ifEmpty { "知乎内容" },
                content = content,
                author = User(authorName, authorName, authorAvatar),
                community = community,
                timeAgo = "",
                likeCount = voteCount,
                commentCount = commentCount,
                tags = listOf(type)
            )

            val comments = mutableListOf<Comment>()
            doc.select(".ContentItem:has(.RichContent), .List-item").forEach { item ->
                try {
                    val commentContent = item.selectFirst(".RichContent-inner, .RichText")?.let {
                        HtmlUtils.cleanHtml(it.html())
                    } ?: return@forEach
                    if (commentContent.isBlank()) return@forEach

                    val commentAuthor = item.selectFirst(".AuthorInfo-name, .UserLink-link")?.text()?.trim() ?: "Anonymous"
                    val commentAvatar = item.selectFirst(".AuthorInfo-avatarImage img, .Avatar")?.attr("src") ?: ""
                    val commentId = item.attr("data-zop-feedlist")?.let {
                        Regex("\"id\":\"(\\d+)\"").find(it)?.groupValues?.getOrNull(1)
                    } ?: comments.size.toString()

                    comments.add(
                        Comment(
                            id = commentId,
                            author = User(commentAuthor, commentAuthor, commentAvatar),
                            content = commentContent,
                            timeAgo = "",
                            likeCount = 0
                        )
                    )
                } catch (e: Exception) {
                    // Skip malformed comments
                }
            }

            ThreadDetailResult(
                thread = thread,
                comments = comments,
                totalPages = null
            )
        } catch (e: Exception) {
            android.util.Log.e("Zhihu", "scrapeThreadFromWeb failed", e)
            buildFallbackDetail(threadId, type, actualId)
        }
    }

    private fun fetchComments(type: String, id: String, page: Int): List<Comment> {
        val offset = (page - 1) * 20
        val url = "$baseUrl/api/v4/$type/$id/root_comments?limit=20&offset=$offset&order=normal&status=open"

        val request = Request.Builder()
            .url(url)
            .headers(buildHeaders())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return emptyList()

        android.util.Log.d("Zhihu", "fetchComments($type/$id): status=${response.code}")

        if (!response.isSuccessful) return emptyList()

        val comments = mutableListOf<Comment>()
        try {
            val jsonObj = json.parseToJsonElement(responseBody).jsonObject
            val data = jsonObj["data"]?.jsonArray ?: return emptyList()

            data.forEach { item ->
                try {
                    val commentObj = item.jsonObject
                    val content = commentObj["content"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                    if (content.isBlank()) return@forEach

                    val commentId = commentObj["id"]?.jsonPrimitive?.let {
                        it.intOrNull?.toString() ?: it.contentOrNull
                    } ?: return@forEach

                    val authorObj = commentObj["author"]?.jsonObject
                    val authorName = authorObj?.get("member")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                        ?: authorObj?.get("name")?.jsonPrimitive?.contentOrNull
                        ?: "Anonymous"
                    val authorAvatar = authorObj?.get("member")?.jsonObject?.get("avatar_url")?.jsonPrimitive?.contentOrNull
                        ?: authorObj?.get("avatar_url")?.jsonPrimitive?.contentOrNull
                        ?: ""
                    val authorId = authorObj?.get("member")?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                        ?: authorObj?.get("id")?.jsonPrimitive?.contentOrNull
                        ?: authorName

                    val likeCount = commentObj["like_count"]?.jsonPrimitive?.intOrNull
                        ?: commentObj["vote_count"]?.jsonPrimitive?.intOrNull
                        ?: 0

                    val createdTime = commentObj["created_time"]?.jsonPrimitive?.intOrNull
                    val timeAgo = createdTime?.let { TimeUtils.calculateTimeAgo(it.toLong()) } ?: ""

                    comments.add(
                        Comment(
                            id = commentId,
                            author = User(authorId, authorName, authorAvatar),
                            content = HtmlUtils.cleanHtml(content),
                            timeAgo = timeAgo,
                            likeCount = likeCount
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("Zhihu", "Error parsing comment", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Zhihu", "Error parsing comments response", e)
        }

        android.util.Log.d("Zhihu", "fetchComments: parsed ${comments.size} comments")
        return comments
    }

    private fun fetchQuestionAnswers(questionId: String, page: Int): List<Comment> {
        val offset = (page - 1) * 10
        val url = "$baseUrl/api/v4/questions/$questionId/answers?include=content,voteup_count,comment_count&limit=10&offset=$offset&sort_by=default"

        val request = Request.Builder()
            .url(url)
            .headers(buildHeaders())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return emptyList()

        android.util.Log.d("Zhihu", "fetchQuestionAnswers($questionId): status=${response.code}")

        if (!response.isSuccessful) return emptyList()

        val answers = mutableListOf<Comment>()
        try {
            val jsonObj = json.parseToJsonElement(responseBody).jsonObject
            val data = jsonObj["data"]?.jsonArray ?: return emptyList()

            data.forEach { item ->
                try {
                    val answerObj = item.jsonObject
                    val content = answerObj["content"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                    if (content.isBlank()) return@forEach

                    val answerId = answerObj["id"]?.jsonPrimitive?.let {
                        it.intOrNull?.toString() ?: it.contentOrNull
                    } ?: return@forEach

                    val authorObj = answerObj["author"]?.jsonObject
                    val authorName = authorObj?.get("name")?.jsonPrimitive?.contentOrNull ?: "Anonymous"
                    val authorAvatar = authorObj?.get("avatar_url")?.jsonPrimitive?.contentOrNull ?: ""
                    val authorId = authorObj?.get("id")?.jsonPrimitive?.contentOrNull ?: authorName

                    val voteupCount = answerObj["voteup_count"]?.jsonPrimitive?.intOrNull ?: 0

                    val createdTime = answerObj["created_time"]?.jsonPrimitive?.intOrNull
                    val timeAgo = createdTime?.let { TimeUtils.calculateTimeAgo(it.toLong()) } ?: ""

                    answers.add(
                        Comment(
                            id = answerId,
                            author = User(authorId, authorName, authorAvatar),
                            content = HtmlUtils.cleanHtml(content),
                            timeAgo = timeAgo,
                            likeCount = voteupCount
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("Zhihu", "Error parsing answer", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Zhihu", "Error parsing answers response", e)
        }

        android.util.Log.d("Zhihu", "fetchQuestionAnswers: parsed ${answers.size} answers")
        return answers
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148"
    }
}
