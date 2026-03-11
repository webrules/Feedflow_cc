package com.feedflow.domain.service

import com.feedflow.data.local.encryption.EncryptionHelper
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class TwoLibraServiceTest {

    private lateinit var mockClient: OkHttpClient
    private lateinit var mockEncryptionHelper: EncryptionHelper
    private lateinit var service: TwoLibraService

    @Before
    fun setup() {
        mockClient = mock()
        mockEncryptionHelper = mock()
        service = TwoLibraService(mockClient, mockEncryptionHelper)
    }

    @Test
    fun fetchCategories_returnsPredefinedCategories() = runBlocking {
        val categories = service.fetchCategories()

        assertEquals(17, categories.size)
        assertEquals("forum", categories[0].id)
        assertEquals("全部", categories[0].name)
        assertEquals("2libra", categories[0].category)
    }

    @Test
    fun getWebURL_returnsCorrectUrl() {
        val thread = ForumThread(
            id = "tech:abc123",
            title = "Test",
            content = "Content",
            author = com.feedflow.data.model.User("u1", "User", ""),
            community = Community("tech", "技术", "", "2libra"),
            timeAgo = "1h",
            likeCount = 10,
            commentCount = 5
        )

        val url = service.getWebURL(thread)

        assertEquals("https://2libra.com/post/tech/abc123", url)
    }

    @Test
    fun getWebURL_withoutColon_handlesCorrectly() {
        val thread = ForumThread(
            id = "abc123",
            title = "Test",
            content = "Content",
            author = com.feedflow.data.model.User("u1", "User", ""),
            community = Community("forum", "全部", "", "2libra"),
            timeAgo = "1h",
            likeCount = 10,
            commentCount = 5
        )

        val url = service.getWebURL(thread)

        assertTrue(url.startsWith("https://2libra.com/post/"))
    }

    @Test
    fun supportsPosting_returnsFalse() {
        assertFalse(service.supportsPosting())
    }

    @Test
    fun requiresLogin_returnsTrue() {
        assertTrue(service.requiresLogin())
    }

    @Test
    fun name_isCorrect() {
        assertEquals("2Libra", service.name)
    }

    @Test
    fun id_isCorrect() {
        assertEquals("2libra", service.id)
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
