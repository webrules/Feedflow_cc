package com.feedflow.domain.service

import com.feedflow.R
import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.data.model.Comment
import com.feedflow.data.model.Community
import com.feedflow.data.model.FeedInfo
import com.feedflow.data.model.DefaultFeeds
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.ThreadDetailResult
import com.feedflow.data.model.User
import com.feedflow.domain.parser.RSSParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RSSService @Inject constructor(
    private val client: OkHttpClient,
    private val preferencesManager: PreferencesManager,
    private val rssParser: RSSParser
) : ForumService {

    override val name: String = "RSS Feeds"
    override val id: String = "rss"
    override val logo: Int = R.drawable.ic_rss

    private val json = Json { ignoreUnknownKeys = true }
    private val threadCache = mutableMapOf<String, ForumThread>()

    override suspend fun fetchCategories(): List<Community> {
        val feeds = getFeeds()
        return feeds.map { feed ->
            Community(
                id = feed.id,
                name = feed.name,
                description = feed.description,
                category = id
            )
        }
    }

    override suspend fun fetchCategoryThreads(
        categoryId: String,
        communities: List<Community>,
        page: Int
    ): List<ForumThread> = withContext(Dispatchers.IO) {
        if (page > 1) return@withContext emptyList() // RSS feeds don't have pagination

        val feeds = getFeeds()
        val feed = feeds.find { it.id == categoryId } ?: return@withContext emptyList()
        val community = communities.find { it.id == categoryId }
            ?: Community(feed.id, feed.name, feed.description, id)

        try {
            val request = Request.Builder()
                .url(feed.url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            val xml = response.body?.string() ?: return@withContext emptyList()

            val threads = rssParser.parse(xml, community)

            // Cache threads for detail view
            threads.forEach { thread ->
                threadCache[thread.id] = thread
            }

            threads
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun fetchThreadDetail(threadId: String, page: Int): ThreadDetailResult {
        val thread = threadCache[threadId]
            ?: throw Exception("Thread not found in cache. Please refresh the feed.")

        return ThreadDetailResult(
            thread = thread,
            comments = emptyList(),
            totalPages = null
        )
    }

    override suspend fun postComment(topicId: String, categoryId: String, content: String) {
        throw UnsupportedOperationException("Commenting not supported for RSS feeds")
    }

    override suspend fun createThread(categoryId: String, title: String, content: String) {
        throw UnsupportedOperationException("Thread creation not supported for RSS feeds")
    }

    override fun getWebURL(thread: ForumThread): String = thread.id // RSS uses URL as ID

    // Feed Management
    suspend fun getFeeds(): List<FeedInfo> {
        val customFeedsJson = preferencesManager.customRssFeeds.first()
        val customFeeds = if (!customFeedsJson.isNullOrBlank()) {
            try {
                json.decodeFromString<List<FeedInfo>>(customFeedsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return DefaultFeeds.feeds + customFeeds
    }

    suspend fun addFeed(name: String, url: String) {
        val currentFeeds = getFeeds().filter { it.id !in DefaultFeeds.feeds.map { f -> f.id } }
        val newFeed = FeedInfo(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            url = url
        )
        val updatedFeeds = currentFeeds + newFeed
        preferencesManager.setCustomRssFeeds(json.encodeToString(updatedFeeds))
    }

    suspend fun removeFeeds(ids: Set<String>) {
        val currentFeeds = getFeeds().filter { it.id !in DefaultFeeds.feeds.map { f -> f.id } }
        val updatedFeeds = currentFeeds.filter { it.id !in ids }
        preferencesManager.setCustomRssFeeds(json.encodeToString(updatedFeeds))
    }

    companion object {
        private const val USER_AGENT = "Feedflow RSS Reader/1.0"
    }
}
