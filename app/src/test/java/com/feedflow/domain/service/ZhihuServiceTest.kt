package com.feedflow.domain.service

import com.feedflow.data.local.encryption.EncryptionHelper
import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ZhihuServiceTest {

    private lateinit var mockClient: OkHttpClient
    private lateinit var mockPreferencesManager: PreferencesManager
    private lateinit var mockEncryptionHelper: EncryptionHelper
    private lateinit var service: ZhihuService

    @Before
    fun setup() {
        mockClient = mock()
        mockPreferencesManager = mock()
        mockEncryptionHelper = mock()
        whenever(mockPreferencesManager.downvotedIds).thenReturn(flowOf(null))
        service = ZhihuService(mockClient, mockPreferencesManager, mockEncryptionHelper)
    }

    @Test
    fun fetchCategories_returnsPredefinedCategories() = runBlocking {
        val categories = service.fetchCategories()

        assertEquals(2, categories.size)
        assertEquals("recommend", categories[0].id)
        assertEquals("hot", categories[1].id)
        assertEquals("zhihu", categories[0].category)
    }

    @Test
    fun getWebURL_answer_returnsCorrectUrl() {
        val thread = ForumThread(
            id = "answer_123456",
            title = "Test",
            content = "Content",
            author = com.feedflow.data.model.User("u1", "User", ""),
            community = Community("recommend", "Recommend", "", "zhihu"),
            timeAgo = "1h",
            likeCount = 10,
            commentCount = 5
        )

        val url = service.getWebURL(thread)

        assertEquals("https://www.zhihu.com/answer/123456", url)
    }

    @Test
    fun getWebURL_article_returnsCorrectUrl() {
        val thread = ForumThread(
            id = "article_123456",
            title = "Test",
            content = "Content",
            author = com.feedflow.data.model.User("u1", "User", ""),
            community = Community("recommend", "Recommend", "", "zhihu"),
            timeAgo = "1h",
            likeCount = 10,
            commentCount = 5
        )

        val url = service.getWebURL(thread)

        assertEquals("https://www.zhihu.com/p/123456", url)
    }

    @Test
    fun getWebURL_question_returnsCorrectUrl() {
        val thread = ForumThread(
            id = "question_123456",
            title = "Test",
            content = "Content",
            author = com.feedflow.data.model.User("u1", "User", ""),
            community = Community("hot", "Hot", "", "zhihu"),
            timeAgo = "1h",
            likeCount = 10,
            commentCount = 5
        )

        val url = service.getWebURL(thread)

        assertEquals("https://www.zhihu.com/question/123456", url)
    }

    @Test
    fun supportsPosting_returnsFalse() {
        assertFalse(service.supportsPosting())
    }

    @Test
    fun name_isCorrect() {
        assertEquals("Zhihu", service.name)
    }

    @Test
    fun id_isCorrect() {
        assertEquals("zhihu", service.id)
    }

    @Test
    fun postComment_throwsUnsupportedOperationException() = runBlocking {
        var exceptionThrown = false
        try {
            service.postComment("1", "recommend", "content")
        } catch (e: UnsupportedOperationException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }

    @Test
    fun createThread_throwsUnsupportedOperationException() = runBlocking {
        var exceptionThrown = false
        try {
            service.createThread("recommend", "title", "content")
        } catch (e: UnsupportedOperationException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }

    @Test
    fun isDownvoted_returnsFalseInitially() = runBlocking {
        service.fetchCategories()
        assertFalse(service.isDownvoted("answer_123"))
    }
}
