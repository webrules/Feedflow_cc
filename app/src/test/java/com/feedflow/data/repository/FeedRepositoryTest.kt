package com.feedflow.data.repository

import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.data.model.FeedInfo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FeedRepositoryTest {

    private lateinit var mockPreferencesManager: PreferencesManager
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockPreferencesManager = mock()
    }

    @Test
    fun isDefaultFeed_returnsTrueForKnownDefaultFeed() = runTest {
        whenever(mockPreferencesManager.customRssFeeds).thenReturn(flowOf(null))
        val feedRepository = FeedRepository(mockPreferencesManager)

        val result = feedRepository.isDefaultFeed("hacker_podcast")
        assertTrue(result)
    }

    @Test
    fun isDefaultFeed_returnsFalseForCustomFeed() = runTest {
        whenever(mockPreferencesManager.customRssFeeds).thenReturn(flowOf(null))
        val feedRepository = FeedRepository(mockPreferencesManager)

        val result = feedRepository.isDefaultFeed("custom_123")
        assertFalse(result)
    }

    @Test
    fun addFeed_callsPreferencesManager() = runTest {
        val customFeedsJson = json.encodeToString(listOf<FeedInfo>())
        whenever(mockPreferencesManager.customRssFeeds).thenReturn(flowOf(customFeedsJson))
        val feedRepository = FeedRepository(mockPreferencesManager)

        feedRepository.addFeed("Test", "https://example.com/feed.xml", "Desc")

        verify(mockPreferencesManager).setCustomRssFeeds(any())
    }

    @Test
    fun removeFeed_callsPreferencesManager() = runTest {
        val customFeed = FeedInfo("custom_1", "Test", "https://example.com/feed.xml")
        val customFeedsJson = json.encodeToString(listOf(customFeed))
        whenever(mockPreferencesManager.customRssFeeds).thenReturn(flowOf(customFeedsJson))
        val feedRepository = FeedRepository(mockPreferencesManager)

        feedRepository.removeFeed("custom_1")

        verify(mockPreferencesManager).setCustomRssFeeds(any())
    }
}
