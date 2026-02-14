package com.feedflow.domain.service

import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.data.model.Community
import com.feedflow.data.model.FeedInfo
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.User
import com.feedflow.domain.parser.RSSParser
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@RunWith(AndroidJUnit4::class)
class RSSServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var rssParser: RSSParser
    private lateinit var service: RSSService
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = OkHttpClient.Builder().build()
        preferencesManager = mock()
        rssParser = mock()
        service = RSSService(client, preferencesManager, rssParser)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun fetchCategories_returnsDefaultAndCustomFeeds() = runBlocking {
        // Mock preferences to return empty custom feeds
        whenever(preferencesManager.customRssFeeds).thenReturn(flowOf("[]"))

        val categories = service.fetchCategories()

        // Default feeds (3) + 0 custom
        assertTrue(categories.isNotEmpty())
        assertEquals(3, categories.size)
        // Check first default feed
        assertEquals("Hacker Podcast", categories[0].name)
    }

    @Test
    fun fetchCategoryThreads_withValidFeed_callsParserAndReturnsThreads() = runBlocking {
        // Setup
        val xmlContent = "<rss>...</rss>"
        mockWebServer.enqueue(MockResponse().setBody(xmlContent))

        // We need to inject a feed with localhost URL so OkHttp hits MockWebServer
        val feedUrl = mockWebServer.url("/rss").toString()
        val testFeed = FeedInfo("test_feed", "Test Feed", feedUrl)

        // Mock custom feeds to include our test feed
        val customFeeds = listOf(testFeed)
        val customFeedsJson = json.encodeToString(customFeeds)
        whenever(preferencesManager.customRssFeeds).thenReturn(flowOf(customFeedsJson))

        val mockThreads = listOf(
            ForumThread(
                id = "1",
                title = "Title",
                content = "Content",
                author = User("u1", "User", ""),
                community = Community("test_feed", "Test Feed", "", "rss"),
                timeAgo = "time",
                likeCount = 0,
                commentCount = 0
            )
        )
        // Use capture or argument matcher if needed, but any() is fine here
        whenever(rssParser.parse(any(), any())).thenReturn(mockThreads)

        // Act
        val result = service.fetchCategoryThreads("test_feed", emptyList())

        // Assert
        assertEquals(1, result.size)
        assertEquals("Title", result[0].title)
    }

    @Test
    fun fetchThreadDetail_returnsCachedThread() = runBlocking {
        // Setup
        val xmlContent = "<rss>...</rss>"
        mockWebServer.enqueue(MockResponse().setBody(xmlContent))

        val feedUrl = mockWebServer.url("/rss").toString()
        val testFeed = FeedInfo("test_feed", "Test Feed", feedUrl)
        val customFeedsJson = json.encodeToString(listOf(testFeed))
        whenever(preferencesManager.customRssFeeds).thenReturn(flowOf(customFeedsJson))

        val mockThread = ForumThread(
            id = "1",
            title = "Title",
            content = "Content",
            author = User("u1", "User", ""),
            community = Community("test_feed", "Test Feed", "", "rss"),
            timeAgo = "time",
            likeCount = 0,
            commentCount = 0
        )
        whenever(rssParser.parse(any(), any())).thenReturn(listOf(mockThread))

        // First fetch to populate cache
        service.fetchCategoryThreads("test_feed", emptyList())

        // Act
        val result = service.fetchThreadDetail("1")

        // Assert
        assertEquals("Title", result.thread.title)
    }
}
