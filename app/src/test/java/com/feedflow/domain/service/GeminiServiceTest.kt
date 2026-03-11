package com.feedflow.domain.service

import com.feedflow.data.local.preferences.PreferencesManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GeminiServiceTest {

    private lateinit var mockPreferencesManager: PreferencesManager
    private lateinit var service: GeminiService

    @Before
    fun setup() {
        mockPreferencesManager = mock()
        service = GeminiService(mockPreferencesManager)
    }

    @Test
    fun isConfigured_returnsFalseWhenNoApiKey() = runBlocking {
        whenever(mockPreferencesManager.geminiApiKey).thenReturn(flowOf(null))

        val result = service.isConfigured()

        assertFalse(result)
    }

    @Test
    fun isConfigured_returnsFalseWhenEmptyApiKey() = runBlocking {
        whenever(mockPreferencesManager.geminiApiKey).thenReturn(flowOf(""))

        val result = service.isConfigured()

        assertFalse(result)
    }

    @Test
    fun isConfigured_returnsTrueWhenApiKeySet() = runBlocking {
        whenever(mockPreferencesManager.geminiApiKey).thenReturn(flowOf("test-api-key"))

        val result = service.isConfigured()

        assertTrue(result)
    }

    @Test
    fun generateSummary_throwsWhenNoApiKey() = runBlocking {
        whenever(mockPreferencesManager.geminiApiKey).thenReturn(flowOf(null))

        var exceptionThrown = false
        try {
            service.generateSummary("test content")
        } catch (e: Exception) {
            exceptionThrown = true
            assertTrue(e.message?.contains("API key") == true)
        }
        assertTrue(exceptionThrown)
    }

    @Test
    fun generateDailySummary_throwsWhenNoApiKey() = runBlocking {
        whenever(mockPreferencesManager.geminiApiKey).thenReturn(flowOf(null))

        var exceptionThrown = false
        try {
            service.generateDailySummary(listOf("Title" to "Snippet"))
        } catch (e: Exception) {
            exceptionThrown = true
            assertTrue(e.message?.contains("API key") == true)
        }
        assertTrue(exceptionThrown)
    }

    @Test
    fun generateDailySummary_throwsWhenEmptyArticles() = runBlocking {
        whenever(mockPreferencesManager.geminiApiKey).thenReturn(flowOf("test-key"))

        var exceptionThrown = false
        try {
            service.generateDailySummary(emptyList())
        } catch (e: Exception) {
            exceptionThrown = true
            assertTrue(e.message?.contains("No articles") == true)
        }
        assertTrue(exceptionThrown)
    }

    @Test
    fun generateSiteSummary_throwsWhenNoApiKey() = runBlocking {
        whenever(mockPreferencesManager.geminiApiKey).thenReturn(flowOf(null))

        var exceptionThrown = false
        try {
            service.generateSiteSummary("Hacker News", listOf("Title" to "stats"))
        } catch (e: Exception) {
            exceptionThrown = true
            assertTrue(e.message?.contains("API key") == true)
        }
        assertTrue(exceptionThrown)
    }
}
