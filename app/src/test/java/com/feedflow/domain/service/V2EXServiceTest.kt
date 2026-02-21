package com.feedflow.domain.service

import com.feedflow.data.local.encryption.EncryptionHelper
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class V2EXServiceTest {

    private lateinit var mockClient: OkHttpClient
    private lateinit var mockEncryptionHelper: EncryptionHelper
    private lateinit var service: V2EXService

    @Before
    fun setup() {
        mockClient = mock()
        mockEncryptionHelper = mock()
        service = V2EXService(mockClient, mockEncryptionHelper)
    }

    @Test
    fun fetchCategories_returnsPredefinedCategories() = runBlocking {
        val categories = service.fetchCategories()

        assertEquals(10, categories.size)
        assertEquals("tech", categories[0].id)
        assertEquals("技术", categories[0].name)
        assertEquals("v2ex", categories[0].category)
    }

    @Test
    fun getWebURL_returnsCorrectUrl() {
        val thread = ForumThread(
            id = "123456",
            title = "Test",
            content = "Content",
            author = com.feedflow.data.model.User("u1", "User", ""),
            community = Community("tech", "技术", "", "v2ex"),
            timeAgo = "1h",
            likeCount = 10,
            commentCount = 5
        )

        val url = service.getWebURL(thread)

        assertEquals("https://v2ex.com/t/123456", url)
    }

    @Test
    fun supportsPosting_returnsTrue() {
        assertTrue(service.supportsPosting())
    }

    @Test
    fun requiresLogin_returnsTrue() {
        assertTrue(service.requiresLogin())
    }

    @Test
    fun name_isCorrect() {
        assertEquals("V2EX", service.name)
    }

    @Test
    fun id_isCorrect() {
        assertEquals("v2ex", service.id)
    }

    @Test
    fun postComment_throwsUnsupportedOperationException() = runBlocking {
        var exceptionThrown = false
        try {
            service.postComment("1", "tech", "content")
        } catch (e: UnsupportedOperationException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }

    @Test
    fun createThread_throwsUnsupportedOperationException() = runBlocking {
        var exceptionThrown = false
        try {
            service.createThread("tech", "title", "content")
        } catch (e: UnsupportedOperationException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }
}
